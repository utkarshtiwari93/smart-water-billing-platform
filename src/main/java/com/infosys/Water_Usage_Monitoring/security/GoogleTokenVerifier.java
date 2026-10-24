package com.infosys.Water_Usage_Monitoring.security;

import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class GoogleTokenVerifier {

    private static final String GOOGLE_JWK_URI = "https://www.googleapis.com/oauth2/v3/certs";

    public record GoogleUser(String sub, String email, String name) {}

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${app.google.client-id:}") String clientId) {
        this.clientId = clientId;
        this.decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_URI).build();

        OAuth2TokenValidator<Jwt> issuerValidator = jwt -> {
            String iss = jwt.getClaimAsString("iss");
            if ("https://accounts.google.com".equals(iss) || "accounts.google.com".equals(iss))
                return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid issuer", null));
        };

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> {
            if (jwt.getAudience() != null && jwt.getAudience().contains(this.clientId))
                return OAuth2TokenValidatorResult.success();
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        };

        this.decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(), issuerValidator, audienceValidator));
    }

    public GoogleUser verify(String idToken) {
        if (clientId == null || clientId.isBlank())
            throw new BadRequestException("Google login is not configured on the server");

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (JwtException e) {
            throw new BadCredentialsException("Invalid Google token");
        }

        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified")))
            throw new BadCredentialsException("Google email is not verified");

        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank())
            throw new BadCredentialsException("Google token has no email");

        return new GoogleUser(jwt.getSubject(), email.trim().toLowerCase(Locale.ROOT), jwt.getClaimAsString("name"));
    }
}