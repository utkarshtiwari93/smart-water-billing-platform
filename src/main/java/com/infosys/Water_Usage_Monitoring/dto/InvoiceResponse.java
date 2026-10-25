package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(Long id, String invoiceNumber, Long cycleId, LocalDate periodStart,
                              LocalDate periodEnd, Long householdId, String flatNumber,
                              BigDecimal consumptionLitres, BigDecimal totalAmount,
                              LocalDateTime issuedAt, List<InvoiceItemResponse> items) {}