package com.infosys.Water_Usage_Monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_challenges", uniqueConstraints = {
        @UniqueConstraint(name = "uq_auth_challenge_email_purpose", columnNames = {"email", "purpose"})
})
@Getter
@Setter
@NoArgsConstructor
public class AuthChallenge {

    public enum Purpose {
        REGISTRATION,
        PASSWORD_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Purpose purpose;

    @Column(name = "code_hash", length = 100)
    private String codeHash;

    @Column(name = "reset_token_hash", length = 100)
    private String resetTokenHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;
}
