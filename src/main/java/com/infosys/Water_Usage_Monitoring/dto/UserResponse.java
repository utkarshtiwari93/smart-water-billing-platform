package com.infosys.Water_Usage_Monitoring.dto;

public record UserResponse(Long id, String name, String email, String role,
                           Long apartmentId, Long householdId) {}