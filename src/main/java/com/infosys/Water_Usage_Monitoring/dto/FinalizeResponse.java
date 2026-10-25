package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record FinalizeResponse(Long cycleId, String status, int invoicesCreated, BigDecimal totalInvoiced) {}