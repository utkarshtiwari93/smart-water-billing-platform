package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.ReadingSource;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class ConsumptionAggregationServiceTest {

    @Mock WaterUsageLogRepository repository;
    @InjectMocks ConsumptionAggregationService service;

    private WaterUsageLog reading(String date, String meter) {
        WaterUsageLog reading = new WaterUsageLog();
        reading.setReadingDate(LocalDate.parse(date));
        reading.setMeterReading(new BigDecimal(meter));
        reading.setConsumptionLitres(BigDecimal.ZERO);
        reading.setSource(ReadingSource.MANUAL);
        return reading;
    }

    @Test
    void dailyAggregationSpreadsIntervalAcrossCalendarDaysAndReconcilesTotals() {
        var result = service.aggregate(List.of(
                        reading("2026-01-01", "100.00"),
                        reading("2026-01-04", "109.00")),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-04"),
                ConsumptionAggregationService.Granularity.DAY);

        assertEquals(0, result.totalLitres().compareTo(new BigDecimal("9.00")));
        assertEquals(3, result.coveredDays());
        assertNull(result.points().get(0).consumptionLitres());
        assertEquals(0, result.points().get(1).consumptionLitres().compareTo(new BigDecimal("3.00")));
        assertEquals(0, result.points().get(3).consumptionLitres().compareTo(new BigDecimal("3.00")));
    }

    @Test
    void monthlyAggregationSplitsAnIntervalAtCalendarMonthBoundary() {
        var result = service.aggregate(List.of(
                        reading("2026-10-30", "100.00"),
                        reading("2026-11-02", "109.00")),
                LocalDate.parse("2026-10-01"), LocalDate.parse("2026-11-30"),
                ConsumptionAggregationService.Granularity.MONTH);

        assertEquals(2, result.points().size());
        assertEquals("2026-10", result.points().get(0).period());
        assertEquals(0, result.points().get(0).consumptionLitres().compareTo(new BigDecimal("3.00")));
        assertEquals("2026-11", result.points().get(1).period());
        assertEquals(0, result.points().get(1).consumptionLitres().compareTo(new BigDecimal("6.00")));
        assertEquals(0, result.totalLitres().compareTo(new BigDecimal("9.00")));
    }

    @Test
    void requestedRangeUsesOnlyTheOverlappingPartOfBoundaryInterval() {
        var result = service.aggregate(List.of(
                        reading("2026-01-01", "100"),
                        reading("2026-01-05", "112")),
                LocalDate.parse("2026-01-02"), LocalDate.parse("2026-01-03"),
                ConsumptionAggregationService.Granularity.DAY);

        assertEquals(0, result.totalLitres().compareTo(new BigDecimal("6.00")));
        assertEquals(2, result.coveredDays());
    }

    @Test
    void aSingleBaselineReadingDoesNotBecomeZeroConsumptionCoverage() {
        var result = service.aggregate(List.of(reading("2026-01-01", "100")),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"),
                ConsumptionAggregationService.Granularity.MONTH);

        assertFalse(result.hasData());
        assertNull(result.points().get(0).consumptionLitres());
    }

    @Test
    void zeroConsumptionStillCountsAsValidIntervalCoverage() {
        var result = service.aggregate(List.of(
                        reading("2026-01-01", "100"),
                        reading("2026-01-03", "100")),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-03"),
                ConsumptionAggregationService.Granularity.DAY);

        assertEquals(2, result.coveredDays());
        assertEquals(0, result.totalLitres().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, result.points().get(1).consumptionLitres().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void negativeAndDecreasingReadingsAreSkippedWithoutNegativeConsumption() {
        var result = service.aggregate(List.of(
                        reading("2026-01-01", "100"),
                        reading("2026-01-02", "-1"),
                        reading("2026-01-03", "90"),
                        reading("2026-01-04", "110")),
                LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-04"),
                ConsumptionAggregationService.Granularity.DAY);

        assertEquals(0, result.totalLitres().compareTo(new BigDecimal("10.00")));
        assertEquals(3, result.coveredDays());
        assertEquals(0, result.points().stream()
                .filter(point -> point.consumptionLitres() != null)
                .map(com.infosys.Water_Usage_Monitoring.dto.UsageTrendResponse.Point::consumptionLitres)
                .filter(value -> value.signum() < 0).count());
    }
}
