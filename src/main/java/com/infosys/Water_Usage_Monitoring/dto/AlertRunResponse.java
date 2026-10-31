package com.infosys.Water_Usage_Monitoring.dto;

public record AlertRunResponse(int readingsChecked, int alertsCreated, int emailsSent) {}