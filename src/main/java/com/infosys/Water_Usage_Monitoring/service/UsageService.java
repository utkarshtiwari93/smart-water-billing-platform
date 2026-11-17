package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.ApartmentUsageResponse;
import com.infosys.Water_Usage_Monitoring.dto.ResidentBenchmarkResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsageRequest;
import com.infosys.Water_Usage_Monitoring.dto.UsageResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsagePeriodComparisonResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsageTrendResponse;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UsageService {

    private final WaterUsageLogRepository logRepo;
    private final HouseholdRepository householdRepo;
    private final UserRepository userRepo;
    private final ConsumptionAggregationService aggregationService;

    @Transactional
    public UsageResponse createManual(UsageRequest r) {
        Household household = householdRepo.findById(r.householdId())
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + r.householdId()));
        return saveReading(household, r.readingDate(), r.meterReading(), ReadingSource.MANUAL);
    }

    @Transactional
    public UsageResponse saveReading(Household household, LocalDate date,
                                     BigDecimal meterReading, ReadingSource source) {
        if (logRepo.existsByHouseholdIdAndReadingDate(household.getId(), date))
            throw new DuplicateResourceException("Reading already exists for " + date);

        BigDecimal consumption = BigDecimal.ZERO;   // pehli reading = baseline
        WaterUsageLog latest = logRepo.findTopByHouseholdIdOrderByReadingDateDesc(household.getId()).orElse(null);

        if (latest != null) {
            if (!date.isAfter(latest.getReadingDate()))
                throw new BadRequestException("Reading date must be after the latest reading ("
                        + latest.getReadingDate() + ")");
            if (meterReading.compareTo(latest.getMeterReading()) < 0)
                throw new BadRequestException("Meter reading cannot be less than the previous reading ("
                        + latest.getMeterReading() + ")");
            consumption = meterReading.subtract(latest.getMeterReading());
        }

        WaterUsageLog log = new WaterUsageLog();
        log.setHousehold(household);
        log.setReadingDate(date);
        log.setMeterReading(meterReading);
        log.setConsumptionLitres(consumption);
        log.setSource(source);
        return toDto(logRepo.save(log));
    }

    @Transactional(readOnly = true)
    public List<UsageResponse> list(Long householdId, LocalDate from, LocalDate to, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));

        if (user.getRole() == Role.RESIDENT
                && (user.getHousehold() == null || !user.getHousehold().getId().equals(householdId)))
            throw new AccessDeniedException("You can only view your own household usage");

        if (!householdRepo.existsById(householdId))
            throw new ResourceNotFoundException("Household not found: " + householdId);

        LocalDate[] range = resolveRange(from, to);

        return logRepo.findByHouseholdIdAndReadingDateBetweenOrderByReadingDate(householdId, range[0], range[1])
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ApartmentUsageResponse apartmentUsage(Long apartmentId, LocalDate from, LocalDate to, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (user.getRole() != Role.ADMIN || user.getApartment() == null
                || !user.getApartment().getId().equals(apartmentId))
            throw new AccessDeniedException("You can only view usage for your assigned apartment");

        LocalDate[] range = resolveRange(from, to);
        Map<Long, ConsumptionAggregationService.Aggregate> aggregates =
                aggregationService.apartment(apartmentId, range[0], range[1], ConsumptionAggregationService.Granularity.DAY);
        List<Household> apartmentHouseholds = householdRepo.findByApartmentId(apartmentId);
        List<ConsumptionAggregationService.Aggregate> valid = apartmentHouseholds.stream()
                .map(h -> aggregates.get(h.getId()))
                .filter(Objects::nonNull)
                .filter(ConsumptionAggregationService.Aggregate::hasData)
                .toList();
        BigDecimal average = valid.isEmpty() ? null : valid.stream()
                .map(ConsumptionAggregationService.Aggregate::totalLitres)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(valid.size()), 2, java.math.RoundingMode.HALF_UP);
        List<ApartmentUsageResponse.HouseholdUsage> households = householdRepo.findByApartmentId(apartmentId)
                .stream()
                .map(household -> {
                    ConsumptionAggregationService.Aggregate aggregate = aggregates.get(household.getId());
                    boolean hasData = aggregate != null && aggregate.hasData();
                    BigDecimal usage = hasData ? aggregate.totalLitres() : null;
                    boolean unusuallyHigh = hasData && average != null && average.signum() > 0
                            && usage.compareTo(average.multiply(new BigDecimal("1.5"))) > 0;
                    return new ApartmentUsageResponse.HouseholdUsage(household.getId(), household.getFlatNumber(),
                            usage, hasData, hasData ? aggregate.coveredDays() : 0, unusuallyHigh);
                })
                .toList();
        return new ApartmentUsageResponse(range[0], range[1], average, valid.size(), households);
    }

    @Transactional(readOnly = true)
    public UsageTrendResponse trend(Long householdId, LocalDate from, LocalDate to,
                                    ConsumptionAggregationService.Granularity granularity, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Household household = householdRepo.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));
        if (user.getRole() == Role.RESIDENT
                && (user.getHousehold() == null || !user.getHousehold().getId().equals(householdId))) {
            throw new AccessDeniedException("You can only view your own household usage");
        }
        if (user.getRole() == Role.ADMIN
                && (user.getApartment() == null
                || !user.getApartment().getId().equals(household.getApartment().getId()))) {
            throw new AccessDeniedException("You can only view usage for your assigned apartment");
        }

        LocalDate[] range = resolveRange(from, to);
        long days = java.time.temporal.ChronoUnit.DAYS.between(range[0], range[1]) + 1;
        if ((granularity == ConsumptionAggregationService.Granularity.DAY && days > 366)
                || (granularity == ConsumptionAggregationService.Granularity.MONTH && days > 3660)) {
            throw new BadRequestException("Requested usage trend range is too large");
        }
        ConsumptionAggregationService.Aggregate aggregate =
                aggregationService.household(householdId, range[0], range[1], granularity);
        return new UsageTrendResponse(range[0], range[1], granularity.name(), aggregate.totalLitres(),
                aggregate.coveredDays(), aggregate.points());
    }

    @Transactional(readOnly = true)
    public UsagePeriodComparisonResponse compare(Long householdId, LocalDate from, LocalDate to, String email) {
        LocalDate[] currentRange = resolveRange(from, to);
        long periodDays = java.time.temporal.ChronoUnit.DAYS.between(currentRange[0], currentRange[1]) + 1;
        if (periodDays > 366) throw new BadRequestException("Requested comparison range is too large");
        LocalDate previousTo = currentRange[0].minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(periodDays - 1);
        UsageTrendResponse current = trend(householdId, currentRange[0], currentRange[1],
                ConsumptionAggregationService.Granularity.DAY, email);
        UsageTrendResponse previous = trend(householdId, previousFrom, previousTo,
                ConsumptionAggregationService.Granularity.DAY, email);
        BigDecimal percent = current.coveredDays() == 0 || previous.coveredDays() == 0
                || previous.totalConsumptionLitres().signum() == 0
                ? null
                : current.totalConsumptionLitres().subtract(previous.totalConsumptionLitres())
                .multiply(new BigDecimal("100"))
                .divide(previous.totalConsumptionLitres(), 2, java.math.RoundingMode.HALF_UP);
        return new UsagePeriodComparisonResponse(current.from(), current.to(), previous.from(), previous.to(),
                current.coveredDays() == 0 ? null : current.totalConsumptionLitres(),
                previous.coveredDays() == 0 ? null : previous.totalConsumptionLitres(),
                current.coveredDays(), previous.coveredDays(), percent);
    }

    @Transactional(readOnly = true)
    public ResidentBenchmarkResponse benchmark(LocalDate from, LocalDate to, String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        Household own = user.getHousehold();
        if (user.getRole() != Role.RESIDENT || own == null)
            throw new BadRequestException("You are not assigned to a household");

        LocalDate[] range = resolveRange(from, to);
        Long apartmentId = own.getApartment().getId();
        List<Household> households = householdRepo.findByApartmentId(apartmentId);
        Map<Long, ConsumptionAggregationService.Aggregate> aggregates =
                aggregationService.apartment(apartmentId, range[0], range[1], ConsumptionAggregationService.Granularity.DAY);
        List<Household> withReadings = households.stream()
                .filter(h -> aggregates.containsKey(h.getId()) && aggregates.get(h.getId()).hasData())
                .toList();
        ConsumptionAggregationService.Aggregate ownAggregate = aggregates.get(own.getId());
        boolean ownHasData = ownAggregate != null && ownAggregate.hasData();
        BigDecimal ownTotal = ownHasData ? ownAggregate.totalLitres() : null;
        BigDecimal apartmentAverage = averageOf(withReadings, aggregates);

        List<Household> peers = ownHasData ? withReadings.stream()
                .filter(h -> !h.getId().equals(own.getId()))
                .filter(h -> similarHousehold(own, h))
                .toList() : List.of();
        BigDecimal peerAverage = averageOf(peers, aggregates);

        BigDecimal householdPerOccupant = perOccupant(own, ownTotal);
        BigDecimal apartmentPerOccupant = averagePerOccupant(withReadings, aggregates);
        BigDecimal similarPerOccupant = averagePerOccupant(peers, aggregates);
        return new ResidentBenchmarkResponse(range[0], range[1], ownTotal, apartmentAverage, peerAverage,
                withReadings.size(), peers.size(), !peers.isEmpty(),
                percentageDifference(ownTotal, apartmentAverage),
                percentageDifference(ownTotal, peerAverage),
                householdPerOccupant, apartmentPerOccupant, similarPerOccupant);
    }

    private boolean similarHousehold(Household own, Household candidate) {
        if (own.getAreaSqft() == null || own.getAreaSqft().signum() <= 0
                || candidate.getAreaSqft() == null || candidate.getAreaSqft().signum() <= 0) return false;
        BigDecimal minArea = own.getAreaSqft().multiply(new BigDecimal("0.80"));
        BigDecimal maxArea = own.getAreaSqft().multiply(new BigDecimal("1.20"));
        if (candidate.getAreaSqft().compareTo(minArea) < 0 || candidate.getAreaSqft().compareTo(maxArea) > 0)
            return false;
        return own.getOccupancy() == null || own.getOccupancy() <= 0
                || candidate.getOccupancy() == null || candidate.getOccupancy() <= 0
                || Math.abs(own.getOccupancy() - candidate.getOccupancy()) <= 1;
    }

    private BigDecimal averageOf(List<Household> households,
                                 Map<Long, ConsumptionAggregationService.Aggregate> aggregates) {
        if (households.isEmpty()) return null;
        BigDecimal total = households.stream().map(h -> aggregates.get(h.getId()).totalLitres())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(households.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal averagePerOccupant(List<Household> households,
                                          Map<Long, ConsumptionAggregationService.Aggregate> aggregates) {
        List<BigDecimal> values = households.stream()
                .filter(h -> h.getOccupancy() != null && h.getOccupancy() > 0)
                .map(h -> perOccupant(h, aggregates.get(h.getId()).totalLitres()))
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) return null;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal perOccupant(Household household, BigDecimal consumption) {
        if (consumption == null || household.getOccupancy() == null || household.getOccupancy() <= 0) return null;
        return consumption.divide(BigDecimal.valueOf(household.getOccupancy()), 2, java.math.RoundingMode.HALF_UP);
    }

    private BigDecimal percentageDifference(BigDecimal household, BigDecimal comparison) {
        if (household == null || comparison == null || comparison.signum() == 0) return null;
        return household.subtract(comparison).multiply(new BigDecimal("100"))
                .divide(comparison, 2, java.math.RoundingMode.HALF_UP);
    }

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate end = to != null ? to : LocalDate.now();
        LocalDate start = from != null ? from : end.minusDays(30);
        if (start.isAfter(end)) throw new BadRequestException("'from' must not be after 'to'");
        return new LocalDate[]{start, end};
    }

    private UsageResponse toDto(WaterUsageLog l) {
        return new UsageResponse(l.getId(), l.getHousehold().getId(), l.getReadingDate(),
                l.getMeterReading(), l.getConsumptionLitres(), l.getSource().name());
    }
}