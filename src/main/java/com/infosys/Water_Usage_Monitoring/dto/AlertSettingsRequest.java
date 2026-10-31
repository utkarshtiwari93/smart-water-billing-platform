package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AlertSettingsRequest(
        @Positive @Digits(integer = 10, fraction = 2) BigDecimal usageThresholdLitres,
        @NotNull Boolean leakDetectionEnabled,
        @NotNull Boolean emailEnabled) {}