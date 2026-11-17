package com.infosys.Water_Usage_Monitoring.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AlertMailerTest {

    @Test
    void missingCredentials_skipsEmail() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "", "");

        assertFalse(mailer.send("resident@example.com", "Usage alert", "High usage"));
        verify(provider, never()).getIfAvailable();
    }

    @Test
    void configuredCredentials_sendEmail() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "smtp-user", "smtp-password");

        assertTrue(mailer.send("resident@example.com", "Usage alert", "High usage"));

        verify(sender).send(any(SimpleMailMessage.class));
    }

    @Test
    void smtpFailure_isReportedAsUnsentAndDoesNotEscape() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(sender).send(any(SimpleMailMessage.class));
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "smtp-user", "smtp-password");

        assertFalse(mailer.send("resident@example.com", "Usage alert", "High usage"));
    }

    @Test
    void senderInitializationFailure_isReportedAsUnsentAndDoesNotEscape() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenThrow(new IllegalStateException("SMTP configuration failed"));
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "smtp-user", "smtp-password");

        assertFalse(mailer.send("resident@example.com", "Usage alert", "High usage"));
    }

    @Test
    void configuredCredentials_sendHtmlEmail() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "smtp-user", "smtp-password");

        assertTrue(mailer.sendHtml("resident@example.com", "Water-saving tips", "<p>Use less water</p>"));

        verify(sender).send(any(MimeMessage.class));
    }

    @Test
    void htmlSmtpFailure_isReportedAsUnsentAndDoesNotEscape() {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        JavaMailSender sender = mock(JavaMailSender.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new org.springframework.mail.MailSendException("SMTP unavailable"))
                .when(sender).send(any(MimeMessage.class));
        AlertMailer mailer = new AlertMailer(provider, "billing@example.com", "smtp-user", "smtp-password");

        assertFalse(mailer.sendHtml("resident@example.com", "Water-saving tips", "<p>Use less water</p>"));
    }
}
