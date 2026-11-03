package com.infosys.Water_Usage_Monitoring.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AquaBotResidentContext(
        boolean householdAssigned,
        List<Reading> readings,
        List<Invoice> invoices,
        List<Alert> alerts,
        boolean readingsTruncated,
        boolean invoicesTruncated,
        boolean alertsTruncated) {

    public AquaBotResidentContext {
        readings = List.copyOf(readings);
        invoices = List.copyOf(invoices);
        alerts = List.copyOf(alerts);
    }

    public static AquaBotResidentContext withoutHousehold() {
        return new AquaBotResidentContext(false, List.of(), List.of(), List.of(),
                false, false, false);
    }

    public record Reading(LocalDate date, BigDecimal meterReading, BigDecimal consumptionLitres) {
    }

    public record Invoice(
            LocalDate periodStart,
            LocalDate periodEnd,
            BigDecimal consumptionLitres,
            BigDecimal totalAmount,
            LocalDateTime issuedAt,
            List<InvoiceItem> items) {
        public Invoice {
            items = List.copyOf(items);
        }
    }

    public record InvoiceItem(String type, BigDecimal quantity, BigDecimal unitRate, BigDecimal amount) {
    }

    public record Alert(String type, LocalDateTime createdAt) {
    }
}
