package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;

import java.util.List;

public final class TransactionalEmailTemplates {

    private static final List<String> WATER_SAVING_TIPS = List.of(
            "Check taps, toilets and exposed pipes for leaks.",
            "Run washing machines only with full loads.",
            "Turn off the tap while brushing or washing dishes.",
            "Use a bucket instead of a running hose where practical.",
            "Track your meter readings to spot unusual changes early."
    );

    private TransactionalEmailTemplates() {
    }

    public static String invoice(Household household, Invoice invoice, String paymentInstructions) {
        return layout("Your water bill is ready",
                "<p>Hello " + escape(household.getFlatNumber()) + ",</p>"
                        + "<p>Your invoice <strong>" + escape(invoice.getInvoiceNumber())
                        + "</strong> for " + invoice.getBillingCycle().getStartDate() + " to "
                        + invoice.getBillingCycle().getEndDate() + " is ready.</p>"
                        + "<p>Consumption: <strong>" + invoice.getConsumptionLitres().toPlainString()
                        + " litres</strong><br>Amount payable: <strong>INR "
                        + invoice.getTotalAmount().toPlainString() + "</strong></p>"
                        + "<p>" + escape(paymentInstructions) + "</p>");
    }

    public static String waterSavingTips(Household household, Invoice invoice) {
        StringBuilder tips = new StringBuilder("<p>Practical ways to reduce water use at ")
                .append(escape(household.getFlatNumber())).append(" during ")
                .append(invoice.getBillingCycle().getStartDate()).append(" to ")
                .append(invoice.getBillingCycle().getEndDate()).append(":</p><ul>");
        for (String tip : WATER_SAVING_TIPS) tips.append("<li>").append(escape(tip)).append("</li>");
        tips.append("</ul><p>These are general recommendations; your bill is based only on recorded usage.</p>");
        return layout("Water-saving tips for this billing period", tips.toString());
    }

    public static String alert(String message) {
        return layout("Water usage alert", "<p>" + escape(message).replace("\n", "<br>") + "</p>");
    }

    public static String verificationCode(String code, int lifetimeMinutes) {
        return layout("Verify your AquaFlow email",
                "<p>Use this six-digit code to finish creating your AquaFlow account:</p>"
                        + "<p style=\"font-size:28px;letter-spacing:6px;font-weight:700;color:#087e9b\">"
                        + escape(code) + "</p>"
                        + "<p>This code expires in " + lifetimeMinutes
                        + " minutes. If you did not request this, you can ignore this email.</p>");
    }

    public static String passwordResetCode(String code, int lifetimeMinutes) {
        return layout("Reset your AquaFlow password",
                "<p>Use this six-digit code to reset your AquaFlow password:</p>"
                        + "<p style=\"font-size:28px;letter-spacing:6px;font-weight:700;color:#087e9b\">"
                        + escape(code) + "</p>"
                        + "<p>This code expires in " + lifetimeMinutes
                        + " minutes. If you did not request this, you can ignore this email.</p>");
    }

    private static String layout(String title, String content) {
        return "<!doctype html><html><body style=\"margin:0;background:#f2f7fa;font-family:Arial,sans-serif;color:#16324f\">"
                + "<div style=\"max-width:620px;margin:24px auto;padding:24px;background:#fff;border-radius:12px\">"
                + "<h2 style=\"color:#087e9b\">" + escape(title) + "</h2>" + content
                + "<hr style=\"border:0;border-top:1px solid #dce8ee;margin:24px 0\">"
                + "<p style=\"font-size:12px;color:#607d8b\">Smart Water Billing Platform</p>"
                + "</div></body></html>";
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
