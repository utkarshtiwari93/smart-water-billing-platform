package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionalEmailTemplatesTest {

    @Test
    void invoiceAndTipsTemplatesEscapeHouseholdContent() {
        Household household = new Household();
        household.setFlatNumber("<script>alert(1)</script>");

        BillingCycle cycle = new BillingCycle();
        cycle.setStartDate(LocalDate.of(2026, 10, 1));
        cycle.setEndDate(LocalDate.of(2026, 10, 31));
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber("INV-1");
        invoice.setBillingCycle(cycle);
        invoice.setConsumptionLitres(new BigDecimal("1200"));
        invoice.setTotalAmount(new BigDecimal("24.00"));

        String invoiceEmail = TransactionalEmailTemplates.invoice(household, invoice, "Pay <securely>");
        String tipsEmail = TransactionalEmailTemplates.waterSavingTips(household, invoice);

        assertTrue(invoiceEmail.contains("&lt;script&gt;"));
        assertTrue(invoiceEmail.contains("Pay &lt;securely&gt;"));
        assertTrue(tipsEmail.contains("full loads"));
        assertFalse(tipsEmail.contains("<script>"));
    }

    @Test
    void authCodeTemplatesEscapeCodeContent() {
        String verification = TransactionalEmailTemplates.verificationCode("<b>123456</b>", 10);
        String reset = TransactionalEmailTemplates.passwordResetCode("<script>1</script>", 10);

        assertTrue(verification.contains("&lt;b&gt;123456&lt;/b&gt;"));
        assertTrue(verification.contains("10 minutes"));
        assertTrue(reset.contains("&lt;script&gt;"));
        assertFalse(reset.contains("<script>1</script>"));
    }
}
