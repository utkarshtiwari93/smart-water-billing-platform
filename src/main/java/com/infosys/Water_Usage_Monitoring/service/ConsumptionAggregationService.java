package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.UsageTrendResponse;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class ConsumptionAggregationService {

    public enum Granularity { DAY, MONTH }

    public record Aggregate(
            BigDecimal totalLitres,
            int coveredDays,
            LocalDate latestReadingDate,
            List<UsageTrendResponse.Point> points) {
        public boolean hasData() {
            return coveredDays > 0;
        }
    }

    private record DailyUsage(BigDecimal litres, boolean covered) {}

    private final WaterUsageLogRepository repository;

    @Transactional(readOnly = true)
    public Aggregate household(Long householdId, LocalDate from, LocalDate to, Granularity granularity) {
        return aggregate(repository.findHouseholdReadingsWithBoundaries(householdId, from, to),
                from, to, granularity);
    }

    @Transactional(readOnly = true)
    public Map<Long, Aggregate> apartment(Long apartmentId, LocalDate from, LocalDate to,
                                          Granularity granularity) {
        List<WaterUsageLog> readings = repository.findApartmentReadingsWithBoundaries(apartmentId, from, to);
        Map<Long, List<WaterUsageLog>> readingsByHousehold = new HashMap<>();
        for (WaterUsageLog reading : readings) {
            readingsByHousehold.computeIfAbsent(reading.getHousehold().getId(), ignored -> new ArrayList<>())
                    .add(reading);
        }
        Map<Long, Aggregate> results = new HashMap<>();
        readingsByHousehold.forEach((householdId, householdReadings) ->
                results.put(householdId, aggregate(householdReadings, from, to, granularity)));
        return results;
    }

    public Aggregate aggregate(List<WaterUsageLog> readings, LocalDate from, LocalDate to,
                               Granularity granularity) {
        if (from.isAfter(to)) throw new IllegalArgumentException("'from' must not be after 'to'");

        Map<LocalDate, DailyUsage> daily = new TreeMap<>();
        List<WaterUsageLog> orderedReadings = readings.stream()
                .filter(reading -> reading.getReadingDate() != null)
                .sorted(Comparator.comparing(WaterUsageLog::getReadingDate))
                .toList();
        LocalDate latestReadingDate = null;
        WaterUsageLog previousValid = null;
        for (WaterUsageLog current : orderedReadings) {
            if (current.getMeterReading() == null || current.getMeterReading().signum() < 0) continue;
            if (previousValid == null) {
                previousValid = current;
                if (!current.getReadingDate().isAfter(to)) latestReadingDate = current.getReadingDate();
                continue;
            }
            if (current.getMeterReading().compareTo(previousValid.getMeterReading()) < 0) continue;
            if (!current.getReadingDate().isAfter(to)) latestReadingDate = current.getReadingDate();

            WaterUsageLog previous = previousValid;
            if (current.getReadingDate().isAfter(previous.getReadingDate())) {
                LocalDate firstDay = previous.getReadingDate().plusDays(1);
                LocalDate lastDay = current.getReadingDate();
                long intervalDays = ChronoUnit.DAYS.between(previous.getReadingDate(), lastDay);
                BigDecimal difference = current.getMeterReading().subtract(previous.getMeterReading())
                        .setScale(2, RoundingMode.UNNECESSARY);
                BigInteger[] dailyCents = difference.unscaledValue()
                        .divideAndRemainder(BigInteger.valueOf(intervalDays));
                BigDecimal baseDaily = new BigDecimal(dailyCents[0], 2);
                long extraCents = dailyCents[1].longValueExact();

                LocalDate overlapStart = firstDay.isBefore(from) ? from : firstDay;
                LocalDate overlapEnd = lastDay.isAfter(to) ? to : lastDay;
                for (LocalDate day = overlapStart; !day.isAfter(overlapEnd); day = day.plusDays(1)) {
                    long offset = ChronoUnit.DAYS.between(firstDay, day);
                    BigDecimal litres = baseDaily;
                    if (offset < extraCents) litres = litres.add(new BigDecimal("0.01"));
                    daily.put(day, new DailyUsage(litres, true));
                }
                previousValid = current;
            }
        }

        return toAggregate(daily, from, to, granularity, latestReadingDate);
    }

    private Aggregate toAggregate(Map<LocalDate, DailyUsage> daily, LocalDate from, LocalDate to,
                                  Granularity granularity, LocalDate latestReadingDate) {
        List<UsageTrendResponse.Point> points = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        int coveredDays = daily.size();

        if (granularity == Granularity.DAY) {
            for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
                DailyUsage usage = daily.get(day);
                BigDecimal value = usage == null ? null : usage.litres();
                if (value != null) total = total.add(value);
                points.add(new UsageTrendResponse.Point(day.toString(), value, usage == null ? 0 : 1, 1));
            }
        } else {
            YearMonth month = YearMonth.from(from);
            YearMonth endMonth = YearMonth.from(to);
            while (!month.isAfter(endMonth)) {
                LocalDate monthStart = month.atDay(1).isBefore(from) ? from : month.atDay(1);
                LocalDate monthEnd = month.atEndOfMonth().isAfter(to) ? to : month.atEndOfMonth();
                BigDecimal monthTotal = BigDecimal.ZERO.setScale(2);
                int monthCoverage = 0;
                for (LocalDate day = monthStart; !day.isAfter(monthEnd); day = day.plusDays(1)) {
                    DailyUsage usage = daily.get(day);
                    if (usage != null) {
                        monthTotal = monthTotal.add(usage.litres());
                        monthCoverage++;
                    }
                }
                BigDecimal value = monthCoverage == 0 ? null : monthTotal;
                if (value != null) total = total.add(value);
                int periodDays = Math.toIntExact(ChronoUnit.DAYS.between(monthStart, monthEnd) + 1);
                points.add(new UsageTrendResponse.Point(month.toString(), value, monthCoverage, periodDays));
                month = month.plusMonths(1);
            }
        }
        return new Aggregate(total, coveredDays, latestReadingDate, List.copyOf(points));
    }
}
