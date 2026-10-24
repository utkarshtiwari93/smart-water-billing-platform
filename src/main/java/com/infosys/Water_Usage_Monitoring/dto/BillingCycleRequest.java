package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BillingCycleRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Long tariffPlanId) {}