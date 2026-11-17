package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsagePeriodComparisonResponse(
        LocalDate from,
        LocalDate to,
        LocalDate previousFrom,
        LocalDate previousTo,
        BigDecimal currentConsumptionLitres,
        BigDecimal previousConsumptionLitres,
        int currentCoveredDays,
        int previousCoveredDays,
        BigDecimal percentageDifference) {}
