package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageResponse(Long id, Long householdId, LocalDate readingDate,
                            BigDecimal meterReading, BigDecimal consumptionLitres,
                            String source) {}