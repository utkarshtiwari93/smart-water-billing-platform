package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record HouseholdRequest(
        @NotBlank @Size(max = 20) String flatNumber,
        @Positive BigDecimal areaSqft,
        @Positive Integer occupancy,
        @Size(max = 50) String meterNumber) {}