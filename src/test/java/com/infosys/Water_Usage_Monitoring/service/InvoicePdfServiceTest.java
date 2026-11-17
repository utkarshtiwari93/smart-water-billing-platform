package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.InvoiceItemResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoicePdfData;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfServiceTest {

    @Test
    void generate_createsReadableInvoicePdf() throws Exception {
        InvoicePdfService service = new InvoicePdfService();
        ReflectionTestUtils.setField(service, "paymentInstructions", "Pay through the resident portal.");
        InvoiceResponse invoice = new InvoiceResponse(1L, "INV-C1-H1", 1L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1L, "A-101",
                new java.math.BigDecimal("12000"), new java.math.BigDecimal("450.00"),
                LocalDateTime.of(2026, 11, 1, 10, 0),
                List.of(
                        new InvoiceItemResponse("WATER_TIER1", "Water charge - tier 1", new java.math.BigDecimal("10"),
                                new java.math.BigDecimal("10"), new java.math.BigDecimal("100.00")),
                        new InvoiceItemResponse("WATER_TIER2", "Water charge - tier 2", new java.math.BigDecimal("10"),
                                new java.math.BigDecimal("20"), new java.math.BigDecimal("200.00")),
                        new InvoiceItemResponse("SHARED_AREA", "Shared common water usage",
                                null, null, new java.math.BigDecimal("100.00")),
                        new InvoiceItemResponse("ADJUSTMENT", "Late fee",
                                null, null, new java.math.BigDecimal("50.00"))));
        InvoicePdfData data = new InvoicePdfData(invoice, "River View Apartments", "12 Main Road", "Lucknow",
                "MTR-101", LocalDate.of(2026, 9, 30), new java.math.BigDecimal("100000"),
                LocalDate.of(2026, 10, 31), new java.math.BigDecimal("112000"));

        byte[] bytes = service.generate(data);
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("INV-C1-H1"));
            assertTrue(text.contains("River View Apartments"));
            assertTrue(text.contains("12 Main Road, Lucknow"));
            assertTrue(text.contains("MTR-101"));
            assertTrue(text.contains("100000 litres on 30 Sep 2026"));
            assertTrue(text.contains("112000 litres on 31 Oct 2026"));
            assertTrue(text.contains("Water charge - tier 1"));
            assertTrue(text.contains("Water charge - tier 2"));
            assertTrue(text.contains("Shared common water usage"));
            assertTrue(text.contains("Late fee"));
            assertTrue(text.contains("INR 100.00"));
            assertTrue(text.contains("INR 200.00"));
            assertTrue(text.contains("Pay through the resident portal."));
            assertTrue(text.contains("TOTAL PAYABLE"));
            assertTrue(text.contains("INR 450.00"));
        }
    }

    @Test
    void generate_rejectsSavedTotalThatDoesNotMatchInvoiceItems() {
        InvoicePdfService service = new InvoicePdfService();
        InvoiceResponse invoice = new InvoiceResponse(1L, "INV-C1-H1", 1L,
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), 1L, "A-101",
                new java.math.BigDecimal("12000"), new java.math.BigDecimal("450.00"),
                LocalDateTime.of(2026, 11, 1, 10, 0),
                List.of(new InvoiceItemResponse("SHARED_AREA", "Shared common water usage",
                        null, null, new java.math.BigDecimal("100.00"))));
        InvoicePdfData data = new InvoicePdfData(invoice, "River View", null, null,
                null, null, null, null, null);

        assertThrows(IllegalStateException.class, () -> service.generate(data));
    }
}
