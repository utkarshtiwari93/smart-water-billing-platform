package com.infosys.Water_Usage_Monitoring.dto;

import java.time.LocalDateTime;

public record NotificationResponse(Long id, String type, String message, Long householdId,
                                   String flatNumber, boolean read, LocalDateTime createdAt) {}