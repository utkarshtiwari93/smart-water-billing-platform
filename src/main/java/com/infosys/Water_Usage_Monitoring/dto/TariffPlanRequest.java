package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record TariffPlanRequest(
        @NotBlank @Size(max = 100) String name,
        @Digits(integer = 6, fraction = 2) BigDecimal tier1LimitKl,
        @Digits(integer = 8, fraction = 2) BigDecimal baseRate,
        @Digits(integer = 8, fraction = 2) BigDecimal higherRate,
        @NotNull LocalDate effectiveFrom,
        Boolean active,
        @jakarta.validation.Valid List<TariffSlabRequest> slabs) {

    public TariffPlanRequest(String name, BigDecimal tier1LimitKl, BigDecimal baseRate,
                             BigDecimal higherRate, LocalDate effectiveFrom, Boolean active) {
        this(name, tier1LimitKl, baseRate, higherRate, effectiveFrom, active, null);
    }
}