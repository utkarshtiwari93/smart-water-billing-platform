package com.infosys.Water_Usage_Monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;

@Slf4j
@Component
public class AlertMailer {

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String from;
    private final String username;
    private final String password;

    public AlertMailer(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${app.alerts.mail-from:}") String from,
                       @Value("${spring.mail.username:}") String username,
                       @Value("${spring.mail.password:}") String password) {
        this.senderProvider = senderProvider;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        this.from = resolveFrom(from, this.username);
        if (!isConfigured()) {
            log.warn("SMTP is not configured. Set SMTP_USERNAME and SMTP_PASSWORD (Gmail App Password) "
                    + "or verification codes will only appear in the backend console.");
        } else {
            log.info("SMTP mailer ready (from={})", this.from);
        }
    }

    public boolean isConfigured() {
        return !username.isBlank() && !password.isBlank();
    }

    public boolean send(String to, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Email skipped (SMTP credentials are not configured): to={}, subject={}", to, subject);
            return false;
        }

        try {
            JavaMailSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                log.warn("Email skipped (SMTP sender bean is unavailable): to={}, subject={}", to, subject);
                return false;
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Email sent: to={}, subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    public boolean sendHtml(String to, String subject, String htmlBody) {
        if (!isConfigured()) {
            log.warn("Email skipped (SMTP credentials are not configured): to={}, subject={}", to, subject);
            return false;
        }

        try {
            JavaMailSender sender = senderProvider.getIfAvailable();
            if (sender == null) {
                log.warn("Email skipped (SMTP sender bean is unavailable): to={}, subject={}", to, subject);
                return false;
            }
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            sender.send(message);
            log.info("HTML email sent: to={}, subject={}", to, subject);
            return true;
        } catch (Exception e) {
            log.warn("Could not send HTML email to {}: {}", to, e.getMessage());
            return false;
        }
    }

    private static String resolveFrom(String configuredFrom, String username) {
        String candidate = configuredFrom == null ? "" : configuredFrom.trim();
        if (candidate.isBlank() || candidate.endsWith(".local")) {
            return username.isBlank() ? "no-reply@waterbilling.local" : username;
        }
        return candidate;
    }
}
