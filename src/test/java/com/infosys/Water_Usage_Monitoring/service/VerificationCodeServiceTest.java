package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge.Purpose;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.repository.AuthChallengeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock AuthChallengeRepository challengeRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AlertMailer alertMailer;
    @InjectMocks VerificationCodeService verificationCodeService;

    @BeforeEach
    void enableConsoleFallback() {
        ReflectionTestUtils.setField(verificationCodeService, "consoleCodesWhenUnconfigured", true);
    }

    @Test
    void sendCode_persistsOnlyHashAndEmailsHtmlCode() {
        when(challengeRepository.findForUpdate("person@example.com", Purpose.REGISTRATION))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-code");
        when(alertMailer.sendHtml(eq("person@example.com"), anyString(), anyString())).thenReturn(true);

        verificationCodeService.sendCode("person@example.com", Purpose.REGISTRATION);

        ArgumentCaptor<AuthChallenge> challengeCaptor = ArgumentCaptor.forClass(AuthChallenge.class);
        verify(challengeRepository).save(challengeCaptor.capture());
        assertEquals("hashed-code", challengeCaptor.getValue().getCodeHash());
        assertEquals(Purpose.REGISTRATION, challengeCaptor.getValue().getPurpose());
        assertNotNull(challengeCaptor.getValue().getExpiresAt());

        ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(alertMailer).sendHtml(eq("person@example.com"), eq("Verify your AquaFlow email"), htmlCaptor.capture());
        assertTrue(htmlCaptor.getValue().matches("(?s).*\\d{6}.*"));
        assertTrue(htmlCaptor.getValue().contains("Verify your AquaFlow email"));
    }

    @Test
    void sendCode_whenSmtpMissing_logsConsoleFallbackAndKeepsChallenge() {
        when(challengeRepository.findForUpdate("person@example.com", Purpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-code");
        when(alertMailer.sendHtml(anyString(), anyString(), anyString())).thenReturn(false);

        verificationCodeService.sendCode("person@example.com", Purpose.PASSWORD_RESET);

        verify(challengeRepository).save(any(AuthChallenge.class));
    }

    @Test
    void sendCode_whenConfiguredSmtpFails_andConsoleFallbackEnabled_keepsChallenge() {
        when(challengeRepository.findForUpdate("person@example.com", Purpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-code");
        when(alertMailer.sendHtml(anyString(), anyString(), anyString())).thenReturn(false);

        verificationCodeService.sendCode("person@example.com", Purpose.PASSWORD_RESET);

        verify(challengeRepository).save(any(AuthChallenge.class));
    }

    @Test
    void sendCode_whenConfiguredSmtpFails_andConsoleFallbackDisabled_rejects() {
        ReflectionTestUtils.setField(verificationCodeService, "consoleCodesWhenUnconfigured", false);
        when(challengeRepository.findForUpdate("person@example.com", Purpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-code");
        when(alertMailer.sendHtml(anyString(), anyString(), anyString())).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> verificationCodeService.sendCode("person@example.com", Purpose.PASSWORD_RESET));
        verify(challengeRepository).save(any(AuthChallenge.class));
    }

    @Test
    void sendCode_withinResendWindow_skipsNewCode() {
        AuthChallenge existing = activeChallenge(Purpose.REGISTRATION);
        existing.setId(9L);
        existing.setSentAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(10));
        when(challengeRepository.findForUpdate("person@example.com", Purpose.REGISTRATION))
                .thenReturn(Optional.of(existing));

        verificationCodeService.sendCode("person@example.com", Purpose.REGISTRATION);

        verify(challengeRepository, never()).save(any(AuthChallenge.class));
        verify(alertMailer, never()).sendHtml(anyString(), anyString(), anyString());
    }

    @Test
    void consumeCode_validCodeDeletesChallenge() {
        AuthChallenge challenge = activeChallenge(Purpose.REGISTRATION);
        challenge.setCodeHash("hashed-code");
        when(challengeRepository.findForUpdate("person@example.com", Purpose.REGISTRATION))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);

        verificationCodeService.consumeCode("person@example.com", Purpose.REGISTRATION, "123456");

        verify(challengeRepository).delete(challenge);
    }

    @Test
    void consumeCode_invalidCodeIncrementsAttemptAndRejects() {
        AuthChallenge challenge = activeChallenge(Purpose.REGISTRATION);
        challenge.setCodeHash("hashed-code");
        when(challengeRepository.findForUpdate("person@example.com", Purpose.REGISTRATION))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("000000", "hashed-code")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> verificationCodeService.consumeCode("person@example.com", Purpose.REGISTRATION, "000000"));

        assertEquals(1, challenge.getAttempts());
        verify(challengeRepository).save(challenge);
        verify(challengeRepository, never()).delete(challenge);
    }

    @Test
    void verifyResetCode_returnsShortLivedOpaqueTokenAndReplacesCodeHash() {
        AuthChallenge challenge = activeChallenge(Purpose.PASSWORD_RESET);
        challenge.setCodeHash("hashed-code");
        when(challengeRepository.findForUpdate("person@example.com", Purpose.PASSWORD_RESET))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("123456", "hashed-code")).thenReturn(true);
        when(passwordEncoder.encode(any(String.class))).thenReturn("hashed-reset-token");

        String resetToken = verificationCodeService.verifyResetCode("person@example.com", "123456");

        assertEquals(43, resetToken.length());
        assertEquals("hashed-reset-token", challenge.getResetTokenHash());
        org.junit.jupiter.api.Assertions.assertNull(challenge.getCodeHash());
        verify(challengeRepository).save(challenge);
    }

    @Test
    void consumeResetToken_rejectsInvalidToken() {
        AuthChallenge challenge = activeChallenge(Purpose.PASSWORD_RESET);
        challenge.setCodeHash(null);
        challenge.setResetTokenHash("hashed-reset-token");
        when(challengeRepository.findForUpdate("person@example.com", Purpose.PASSWORD_RESET))
                .thenReturn(Optional.of(challenge));
        when(passwordEncoder.matches("bad-token", "hashed-reset-token")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> verificationCodeService.consumeResetToken("person@example.com", "bad-token"));
    }

    private AuthChallenge activeChallenge(Purpose purpose) {
        AuthChallenge challenge = new AuthChallenge();
        challenge.setEmail("person@example.com");
        challenge.setPurpose(purpose);
        challenge.setExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5));
        challenge.setSentAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1));
        challenge.setAttempts(0);
        return challenge;
    }
}
