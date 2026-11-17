package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResidentBenchmarkResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal householdConsumptionLitres,
        BigDecimal apartmentAverageLitres,
        BigDecimal similarHouseholdAverageLitres,
        int apartmentHouseholdCount,
        int similarHouseholdCount,
        boolean similarGroupAvailable,
        BigDecimal householdVsApartmentPercent,
        BigDecimal householdVsSimilarPercent,
        BigDecimal householdPerOccupantLitres,
        BigDecimal apartmentAveragePerOccupantLitres,
        BigDecimal similarHouseholdAveragePerOccupantLitres) {}
