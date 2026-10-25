package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustmentRequest(
        @NotNull Long householdId,
        @NotNull @Digits(integer = 10, fraction = 2) BigDecimal amount,
        @NotBlank @Size(max = 255) String reason) {}