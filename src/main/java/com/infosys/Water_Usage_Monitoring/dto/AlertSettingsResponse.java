package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record AlertSettingsResponse(Long apartmentId, BigDecimal usageThresholdLitres,
                                    boolean leakDetectionEnabled, boolean emailEnabled, String leakRule) {}