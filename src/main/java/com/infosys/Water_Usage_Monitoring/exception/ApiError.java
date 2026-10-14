package com.infosys.Water_Usage_Monitoring.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ApiError(int status, String message, LocalDateTime timestamp, Map<String, String> fieldErrors) {}