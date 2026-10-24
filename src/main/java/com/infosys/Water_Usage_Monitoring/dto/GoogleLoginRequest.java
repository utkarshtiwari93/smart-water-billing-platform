package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(
        @NotBlank String idToken,
        Long apartmentId,
        Long householdId) {}