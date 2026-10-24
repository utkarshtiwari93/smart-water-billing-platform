package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TariffPlanResponse(Long id, Long apartmentId, String name,
                                 BigDecimal tier1LimitKl, BigDecimal baseRate, BigDecimal higherRate,
                                 LocalDate effectiveFrom, boolean active) {}