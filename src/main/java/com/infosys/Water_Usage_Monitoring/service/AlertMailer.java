package com.infosys.Water_Usage_Monitoring.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AlertMailer {

    private final ObjectProvider<JavaMailSender> senderProvider;
    private final String from;

    public AlertMailer(ObjectProvider<JavaMailSender> senderProvider,
                       @Value("${app.alerts.mail-from:no-reply@waterbilling.local}") String from) {
        this.senderProvider = senderProvider;
        this.from = from;
    }

    public boolean send(String to, String subject, String body) {
        JavaMailSender sender = senderProvider.getIfAvailable();
        if (sender == null) {
            log.info("Email skipped (SMTP not configured): to={} subject={}", to, subject);
            return false;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(from);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            return true;
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
            return false;
        }
    }
}