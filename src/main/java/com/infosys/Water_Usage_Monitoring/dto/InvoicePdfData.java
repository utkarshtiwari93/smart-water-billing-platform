package com.infosys.Water_Usage_Monitoring.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InvoicePdfData(
        InvoiceResponse invoice,
        String apartmentName,
        String apartmentAddress,
        String apartmentCity,
        String meterNumber,
        LocalDate previousReadingDate,
        BigDecimal previousMeterReading,
        LocalDate currentReadingDate,
        BigDecimal currentMeterReading) {}
