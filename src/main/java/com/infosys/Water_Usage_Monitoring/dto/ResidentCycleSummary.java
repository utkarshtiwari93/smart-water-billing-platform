package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ResidentCycleSummary(
        Long id,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        BigDecimal consumptionLitres,
        int coveredDays,
        LocalDate latestReadingDate,
        BigDecimal estimatedWaterCharge) {}
