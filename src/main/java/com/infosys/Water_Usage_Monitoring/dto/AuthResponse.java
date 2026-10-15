package com.infosys.Water_Usage_Monitoring.dto;

public record AuthResponse(String token, String tokenType, Long userId,
                           String name, String email, String role) {}