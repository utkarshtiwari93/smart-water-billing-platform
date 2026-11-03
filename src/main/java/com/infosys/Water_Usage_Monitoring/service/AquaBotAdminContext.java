package com.infosys.Water_Usage_Monitoring.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AquaBotAdminContext(
        List<PeriodSummary> periods,
        List<AlertSummary> alerts,
        int openCycleCount,
        boolean olderPeriodsOmitted,
        boolean olderAlertsOmitted,
        boolean paymentStatusAvailable) {

    public AquaBotAdminContext {
        periods = List.copyOf(periods);
        alerts = List.copyOf(alerts);
    }

    public record PeriodSummary(
            LocalDate startDate,
            LocalDate endDate,
            String cycleStatus,
            boolean finalized,
            BigDecimal totalConsumptionLitres,
            int householdsWithReadings,
            BigDecimal averageConsumptionLitres,
            List<HouseholdConsumption> highestConsumers,
            List<HouseholdConsumption> lowestConsumers,
            List<HouseholdAllocation> sharedCostAllocation,
            BigDecimal totalBulkPurchaseVolumeKl,
            BigDecimal totalBulkPurchaseCost,
            BigDecimal sharedCost,
            BigDecimal surplus,
            BigDecimal invoiceTotal,
            int invoiceCount,
            boolean allocationIsEstimate,
            boolean allocationsTruncated) {

        public PeriodSummary {
            highestConsumers = List.copyOf(highestConsumers);
            lowestConsumers = List.copyOf(lowestConsumers);
            sharedCostAllocation = List.copyOf(sharedCostAllocation);
        }
    }

    public record HouseholdConsumption(
            String flatNumber,
            BigDecimal consumptionLitres) {
    }

    public record HouseholdAllocation(
            String flatNumber,
            String allocationBasis,
            BigDecimal allocatedAmount) {
    }

    public record AlertSummary(
            String type,
            String flatNumber,
            LocalDateTime createdAt,
            String details) {
    }
}
