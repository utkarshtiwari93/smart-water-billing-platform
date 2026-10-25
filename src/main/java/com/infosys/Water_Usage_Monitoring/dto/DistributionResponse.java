package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.util.List;

public record DistributionResponse(Long cycleId, BigDecimal totalPurchaseCost, BigDecimal totalWaterCharges,
                                   BigDecimal sharedCost, BigDecimal surplus, BigDecimal totalBilled,
                                   String note, List<HouseholdShareResponse> households) {}