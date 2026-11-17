package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge.Purpose;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.repository.AuthChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerificationCodeService {

    private static final int CODE_LIFETIME_MINUTES = 10;
    private static final int CODE_RESEND_SECONDS = 60;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final AuthChallengeRepository challengeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AlertMailer alertMailer;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.console-codes-when-unconfigured:true}")
    private boolean consoleCodesWhenUnconfigured;

    @Transactional
    public void sendCode(String email, Purpose purpose) {
        LocalDateTime now = now();
        AuthChallenge challenge = challengeRepository.findForUpdate(email, purpose)
                .orElseGet(AuthChallenge::new);
        if (challenge.getId() != null && challenge.getSentAt().plusSeconds(CODE_RESEND_SECONDS).isAfter(now)) {
            log.info("Verification code resend throttled for {} ({})", email, purpose);
            return;
        }

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        challenge.setEmail(email);
        challenge.setPurpose(purpose);
        challenge.setCodeHash(passwordEncoder.encode(code));
        challenge.setResetTokenHash(null);
        challenge.setAttempts(0);
        challenge.setExpiresAt(now.plusMinutes(CODE_LIFETIME_MINUTES));
        challenge.setSentAt(now);
        challengeRepository.save(challenge);

        String subject = purpose == Purpose.REGISTRATION
                ? "Verify your AquaFlow email"
                : "Your AquaFlow password reset code";
        String html = purpose == Purpose.REGISTRATION
                ? TransactionalEmailTemplates.verificationCode(code, CODE_LIFETIME_MINUTES)
                : TransactionalEmailTemplates.passwordResetCode(code, CODE_LIFETIME_MINUTES);

        if (alertMailer.sendHtml(email, subject, html)) {
            return;
        }

        // Local/dev fallback: keep the challenge usable even when Gmail TLS fails
        // (common with antivirus/proxy SSL interception / PKIX trust errors).
        if (consoleCodesWhenUnconfigured) {
            log.warn("Email delivery failed — verification code for {} ({}) is {}. "
                            + "If the log above shows PKIX/SSLHandshakeException, Java does not trust "
                            + "the SMTP certificate (often antivirus HTTPS scanning). "
                            + "Fix the truststore or temporarily disable SSL scanning, then retry.",
                    email, purpose, code);
            return;
        }

        throw new BadRequestException(
                "Unable to send verification email. Check SMTP settings and try again later.");
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public AuthChallenge consumeCode(String email, Purpose purpose, String code) {
        AuthChallenge challenge = activeChallenge(email, purpose);
        if (challenge.getCodeHash() == null || challenge.getAttempts() >= MAX_CODE_ATTEMPTS) {
            throw invalidCode();
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw invalidCode();
        }
        challengeRepository.delete(challenge);
        return challenge;
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public String verifyResetCode(String email, String code) {
        AuthChallenge challenge = activeChallenge(email, Purpose.PASSWORD_RESET);
        if (challenge.getCodeHash() == null || challenge.getAttempts() >= MAX_CODE_ATTEMPTS) {
            throw invalidCode();
        }
        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            challengeRepository.save(challenge);
            throw invalidCode();
        }

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String resetToken = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        challenge.setCodeHash(null);
        challenge.setResetTokenHash(passwordEncoder.encode(resetToken));
        challenge.setAttempts(0);
        challenge.setExpiresAt(now().plusMinutes(CODE_LIFETIME_MINUTES));
        challengeRepository.save(challenge);
        return resetToken;
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public AuthChallenge consumeResetToken(String email, String resetToken) {
        AuthChallenge challenge = activeChallenge(email, Purpose.PASSWORD_RESET);
        if (challenge.getResetTokenHash() == null
                || !passwordEncoder.matches(resetToken, challenge.getResetTokenHash())) {
            throw new BadRequestException("Invalid or expired password reset request");
        }
        return challenge;
    }

    @Transactional
    public void delete(AuthChallenge challenge) {
        challengeRepository.delete(challenge);
    }

    private AuthChallenge activeChallenge(String email, Purpose purpose) {
        AuthChallenge challenge = challengeRepository.findForUpdate(email, purpose)
                .orElseThrow(this::invalidCode);
        if (!challenge.getExpiresAt().isAfter(now())) {
            challengeRepository.delete(challenge);
            throw invalidCode();
        }
        return challenge;
    }

    private BadRequestException invalidCode() {
        return new BadRequestException("Invalid or expired verification code");
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
