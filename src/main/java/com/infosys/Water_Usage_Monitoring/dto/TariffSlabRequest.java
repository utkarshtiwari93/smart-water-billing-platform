package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record TariffSlabRequest(
        @PositiveOrZero @Digits(integer = 6, fraction = 2) BigDecimal upToKl,
        @jakarta.validation.constraints.NotNull
        @PositiveOrZero @Digits(integer = 8, fraction = 2) BigDecimal rate) {
}
