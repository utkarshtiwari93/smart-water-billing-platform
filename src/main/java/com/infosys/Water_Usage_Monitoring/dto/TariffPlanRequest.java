package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffPlanRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Positive @Digits(integer = 6, fraction = 2) BigDecimal tier1LimitKl,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal baseRate,
        @NotNull @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal higherRate,
        @NotNull LocalDate effectiveFrom,
        Boolean active) {}