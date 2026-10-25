package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record HouseholdShareResponse(Long householdId, String flatNumber, boolean hasMeter,
                                     BigDecimal consumptionLitres, BigDecimal waterCharge,
                                     String allocationBasis, BigDecimal sharedAllocation,
                                     BigDecimal totalCharge) {}