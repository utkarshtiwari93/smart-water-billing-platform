package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BillPreviewResponse(Long cycleId, Long householdId, String flatNumber,
                                  LocalDate periodStart, LocalDate periodEnd,
                                  BigDecimal consumptionLitres, BigDecimal consumptionKl,
                                  BigDecimal tier1LimitKl, BigDecimal baseRate, BigDecimal higherRate,
                                  BigDecimal tier1Kl, BigDecimal tier2Kl,
                                  BigDecimal tier1Charge, BigDecimal tier2Charge,
                                  BigDecimal waterCharge) {}