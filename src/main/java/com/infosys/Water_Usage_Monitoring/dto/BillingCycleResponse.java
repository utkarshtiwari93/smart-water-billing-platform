package com.infosys.Water_Usage_Monitoring.dto;

import java.time.LocalDate;

public record BillingCycleResponse(Long id, Long apartmentId, Long tariffPlanId, String tariffPlanName,
                                   LocalDate startDate, LocalDate endDate, String status) {}