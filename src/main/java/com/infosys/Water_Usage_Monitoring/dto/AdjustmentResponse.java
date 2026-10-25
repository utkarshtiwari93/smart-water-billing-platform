package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record AdjustmentResponse(Long id, Long billingCycleId, Long householdId, String flatNumber,
                                 BigDecimal amount, String reason, String createdBy) {}