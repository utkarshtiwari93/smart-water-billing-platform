package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ApartmentUsageResponse(LocalDate from, LocalDate to, BigDecimal apartmentAverageLitres,
                                     int householdsWithReadings, List<HouseholdUsage> households) {
    public record HouseholdUsage(Long householdId, String flatNumber, BigDecimal consumptionLitres,
                                 boolean hasReadings, int coveredDays, boolean unusuallyHigh) {}
}
