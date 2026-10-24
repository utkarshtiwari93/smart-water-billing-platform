package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BulkPurchaseResponse(Long id, Long billingCycleId, LocalDate purchaseDate, String source,
                                   String supplier, BigDecimal volumeKl, BigDecimal unitCost,
                                   BigDecimal totalCost, String notes) {}