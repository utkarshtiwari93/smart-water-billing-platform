package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.InvoiceItemResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoicePdfData;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private static final Color NAVY = new Color(15, 52, 86);
    private static final Color TEAL = new Color(13, 148, 136);
    private static final Color MUTED = new Color(88, 104, 120);
    private static final Color RULE = new Color(218, 227, 234);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @Value("${app.billing.payment-instructions:Please pay through the payment channel configured by your apartment administrator.}")
    private String paymentInstructions;

    public byte[] generate(InvoicePdfData data) throws IOException {
        validate(data);
        InvoiceResponse invoice = data.invoice();

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Page page = new Page(document, invoice.invoiceNumber(), data.apartmentName());
            page.text("SMART WATER PLATFORM", 52, page.y, 10, true, Color.WHITE);
            page.text("WATER USAGE BILLING STATEMENT", 52, page.y - 20, 20, true, Color.WHITE);
            page.text(invoice.invoiceNumber(), 430, page.y - 44, 9, true, Color.WHITE);
            page.y -= 74;

            page.section("APARTMENT / PROJECT");
            page.text(data.apartmentName(), 52, page.y, 12, true, NAVY);
            page.y -= 17;
            String location = joinNonBlank(data.apartmentAddress(), data.apartmentCity());
            if (!location.isBlank()) {
                page.text(location, 52, page.y, 9, false, MUTED);
                page.y -= 15;
            }

            page.section("BILLING DETAILS");
            page.pair("Invoice number", invoice.invoiceNumber(), "Generated", format(invoice.issuedAt().toLocalDate()));
            page.pair("Household / flat", invoice.flatNumber(),
                    "Billing period", format(invoice.periodStart()) + " to " + format(invoice.periodEnd()));
            page.pair("Meter number", valueOr(data.meterNumber(), "Not assigned"),
                    "Household ID", invoice.householdId().toString());
            page.y -= 8;

            page.section("METER READINGS");
            page.pair("Previous reading",
                    reading(data.previousReadingDate(), data.previousMeterReading()),
                    "Current reading", reading(data.currentReadingDate(), data.currentMeterReading()));
            page.pair("Consumption billed", invoice.consumptionLitres().toPlainString() + " litres",
                    "Reading status", data.currentMeterReading() == null
                            ? "No reading recorded in this billing period" : "Recorded");
            page.y -= 8;

            page.section("ITEMIZED CHARGES");
            page.tableHeader();
            BigDecimal waterCharges = BigDecimal.ZERO;
            BigDecimal sharedCharges = BigDecimal.ZERO;
            BigDecimal otherCharges = BigDecimal.ZERO;
            for (InvoiceItemResponse item : invoice.items()) {
                page.ensure(38);
                page.chargeRow(item);
                switch (item.type()) {
                    case "WATER_TIER1", "WATER_TIER2" -> waterCharges = waterCharges.add(item.amount());
                    case "SHARED_AREA" -> sharedCharges = sharedCharges.add(item.amount());
                    default -> otherCharges = otherCharges.add(item.amount());
                }
            }
            page.y -= 10;
            page.section("BILL SUMMARY");
            page.summaryLine("Water charges (progressive tariff)", money(waterCharges));
            page.summaryLine("Shared-cost allocation", money(sharedCharges));
            page.summaryLine("Other applicable charges", money(otherCharges));
            page.totalLine(invoice.totalAmount());
            page.y -= 9;

            page.section("PAYMENT INSTRUCTIONS");
            page.paragraph(paymentInstructions);
            page.y -= 7;
            page.text("This invoice is generated from finalized billing records. Amounts shown are the saved bill amounts.",
                    52, page.y, 8, false, MUTED);

            page.close();
            document.save(output);
            return output.toByteArray();
        }
    }

    private void validate(InvoicePdfData data) {
        if (data == null || data.invoice() == null) throw new IllegalStateException("Invoice data is missing");
        InvoiceResponse invoice = data.invoice();
        if (isBlank(invoice.invoiceNumber()) || isBlank(invoice.flatNumber()) || invoice.householdId() == null
                || invoice.periodStart() == null || invoice.periodEnd() == null
                || invoice.periodEnd().isBefore(invoice.periodStart()) || invoice.issuedAt() == null
                || invoice.consumptionLitres() == null || invoice.consumptionLitres().signum() < 0
                || invoice.totalAmount() == null || invoice.totalAmount().signum() < 0
                || isBlank(data.apartmentName()) || invoice.items() == null
                || isBlank(paymentInstructions)) {
            throw new IllegalStateException("Invoice contains missing or invalid required data");
        }

        BigDecimal itemTotal = BigDecimal.ZERO;
        for (InvoiceItemResponse item : invoice.items()) {
            if (item == null || isBlank(item.type()) || isBlank(item.description()) || item.amount() == null) {
                throw new IllegalStateException("Invoice contains an invalid charge item");
            }
            itemTotal = itemTotal.add(item.amount());
        }
        if (itemTotal.compareTo(invoice.totalAmount()) != 0) {
            throw new IllegalStateException("Invoice charge items do not match the saved total");
        }
        if ((data.previousReadingDate() == null) != (data.previousMeterReading() == null)
                || (data.currentReadingDate() == null) != (data.currentMeterReading() == null)
                || data.previousMeterReading() != null && data.previousMeterReading().signum() < 0
                || data.currentMeterReading() != null && data.currentMeterReading().signum() < 0
                || data.previousReadingDate() != null && data.currentReadingDate() != null
                && !data.currentReadingDate().isAfter(data.previousReadingDate())
                || data.previousMeterReading() != null && data.currentMeterReading() != null
                && data.currentMeterReading().compareTo(data.previousMeterReading()) < 0) {
            throw new IllegalStateException("Invoice contains invalid meter reading details");
        }
        if (data.previousMeterReading() != null && data.currentMeterReading() != null
                && data.currentMeterReading().subtract(data.previousMeterReading())
                .compareTo(invoice.consumptionLitres()) != 0) {
            throw new IllegalStateException("Meter readings do not match the saved invoice consumption");
        }
    }

    private String reading(LocalDate date, BigDecimal meterReading) {
        return date == null || meterReading == null
                ? "Not available"
                : meterReading.toPlainString() + " litres on " + format(date);
    }

    private String format(LocalDate date) {
        return DATE.format(date);
    }

    private String money(BigDecimal amount) {
        return "INR " + amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private String joinNonBlank(String address, String city) {
        return java.util.stream.Stream.of(address, city).filter(value -> value != null && !value.isBlank())
                .reduce((first, second) -> first + ", " + second).orElse("");
    }

    private String valueOr(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String ascii(String value) {
        return value.replaceAll("[^\\x20-\\x7E]", "?");
    }

    private final class Page {
        private final PDDocument document;
        private final String invoiceNumber;
        private final String apartmentName;
        private PDPageContentStream stream;
        private float y;

        private Page(PDDocument document, String invoiceNumber, String apartmentName) throws IOException {
            this.document = document;
            this.invoiceNumber = invoiceNumber;
            this.apartmentName = apartmentName;
            newPage(true);
        }

        private void newPage(boolean first) throws IOException {
            if (stream != null) stream.close();
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            stream = new PDPageContentStream(document, page);
            y = 742;
            if (first) {
                stream.setNonStrokingColor(NAVY);
                stream.addRect(40, 682, 532, 78);
                stream.fill();
            } else {
                text(apartmentName + " | Invoice " + invoiceNumber, 52, 748, 8, true, MUTED);
                line(52, 738, 560, 738, RULE, 0.7f);
                y = 720;
            }
        }

        private void text(String value, float x, float baseline, float size, boolean bold, Color color)
                throws IOException {
            stream.beginText();
            stream.setFont(new PDType1Font(bold
                    ? Standard14Fonts.FontName.HELVETICA_BOLD
                    : Standard14Fonts.FontName.HELVETICA), size);
            stream.setNonStrokingColor(color);
            stream.newLineAtOffset(x, baseline);
            stream.showText(ascii(value));
            stream.endText();
        }

        private void section(String title) throws IOException {
            ensure(34);
            y -= 3;
            text(title, 52, y, 10, true, TEAL);
            y -= 10;
            line(52, y, 560, y, RULE, 0.7f);
            y -= 16;
        }

        private void pair(String firstLabel, String firstValue, String secondLabel, String secondValue)
                throws IOException {
            ensure(30);
            text(firstLabel.toUpperCase(), 52, y, 7, true, MUTED);
            text(secondLabel.toUpperCase(), 315, y, 7, true, MUTED);
            y -= 13;
            text(firstValue, 52, y, 9, true, NAVY);
            text(secondValue, 315, y, 9, true, NAVY);
            y -= 21;
        }

        private void tableHeader() throws IOException {
            ensure(30);
            stream.setNonStrokingColor(new Color(239, 246, 249));
            stream.addRect(48, y - 5, 516, 22);
            stream.fill();
            text("DESCRIPTION", 54, y + 2, 7, true, NAVY);
            text("QUANTITY", 350, y + 2, 7, true, NAVY);
            text("RATE", 430, y + 2, 7, true, NAVY);
            text("AMOUNT", 500, y + 2, 7, true, NAVY);
            y -= 24;
        }

        private void chargeRow(InvoiceItemResponse item) throws IOException {
            String description = item.description();
            List<String> lines = wrap(description, 48);
            ensure(Math.max(28, lines.size() * 12 + 10));
            text(lines.get(0), 54, y, 8, false, NAVY);
            text(item.quantity() == null ? "-" : item.quantity().toPlainString(), 350, y, 8, false, MUTED);
            text(item.unitRate() == null ? "-" : "INR " + item.unitRate().toPlainString(), 430, y, 8, false, MUTED);
            text("INR " + item.amount().toPlainString(), 500, y, 8, true, NAVY);
            y -= 12;
            for (int i = 1; i < lines.size(); i++) {
                ensure(18);
                text(lines.get(i), 54, y, 8, false, NAVY);
                y -= 12;
            }
            y -= 5;
            line(52, y, 560, y, RULE, 0.4f);
            y -= 12;
        }

        private void summaryLine(String label, String value) throws IOException {
            ensure(20);
            text(label, 54, y, 9, false, NAVY);
            text(value, 438, y, 9, false, NAVY);
            y -= 18;
        }

        private void totalLine(BigDecimal total) throws IOException {
            ensure(40);
            stream.setNonStrokingColor(NAVY);
            stream.addRect(48, y - 12, 516, 34);
            stream.fill();
            text("TOTAL PAYABLE", 58, y, 11, true, Color.WHITE);
            text(money(total), 438, y, 12, true, Color.WHITE);
            y -= 38;
        }

        private void paragraph(String content) throws IOException {
            List<String> lines = wrap(content, 96);
            for (String line : lines) {
                ensure(18);
                text(line, 54, y, 9, false, NAVY);
                y -= 14;
            }
        }

        private List<String> wrap(String value, int maxLength) {
            List<String> lines = new java.util.ArrayList<>();
            StringBuilder line = new StringBuilder();
            for (String word : value.split("\\s+")) {
                if (!line.isEmpty() && line.length() + word.length() + 1 > maxLength) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                if (!line.isEmpty()) line.append(' ');
                line.append(word);
            }
            if (!line.isEmpty()) lines.add(line.toString());
            return lines.isEmpty() ? List.of("") : lines;
        }

        private void ensure(float height) throws IOException {
            if (y - height < 52) newPage(false);
        }

        private void line(float x1, float y1, float x2, float y2, Color color, float width)
                throws IOException {
            stream.setStrokingColor(color);
            stream.setLineWidth(width);
            stream.moveTo(x1, y1);
            stream.lineTo(x2, y2);
            stream.stroke();
        }

        private void close() throws IOException {
            stream.close();
        }
    }
}
