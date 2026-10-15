package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UsageRequest(
        @NotNull Long householdId,
        @NotNull @PastOrPresent LocalDate readingDate,
        @NotNull @PositiveOrZero BigDecimal meterReading) {}