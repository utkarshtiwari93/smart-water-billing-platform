package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;

public record HouseholdResponse(Long id, Long apartmentId, String flatNumber,
                                BigDecimal areaSqft, Integer occupancy, String meterNumber) {}