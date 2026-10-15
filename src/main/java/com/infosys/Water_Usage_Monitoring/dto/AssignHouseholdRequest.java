package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotNull;

public record AssignHouseholdRequest(@NotNull Long householdId) {}