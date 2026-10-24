package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record PurchaseSummaryResponse(Long billingCycleId, int purchaseCount, BigDecimal totalVolumeKl,
                                      BigDecimal totalCost, BigDecimal averageUnitCost) {}