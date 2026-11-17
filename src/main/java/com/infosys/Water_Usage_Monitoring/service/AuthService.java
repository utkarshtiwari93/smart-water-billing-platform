package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AuthResponse;
import com.infosys.Water_Usage_Monitoring.dto.GoogleLoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.LoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.RegisterRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.security.GoogleTokenVerifier;
import com.infosys.Water_Usage_Monitoring.security.JwtService;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge.Purpose;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final VerificationCodeService verificationCodeService;

    public void sendRegistrationCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        // Keep the same generic response as forgot-password: do not reveal whether the email exists.
        if (userRepository.existsByEmail(email)) {
            log.info("Registration code skipped because email is already registered: {}", email);
            return;
        }
        verificationCodeService.sendCode(email, Purpose.REGISTRATION);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public UserResponse register(RegisterRequest r) {
        String email = normalizeEmail(r.email());
        if (userRepository.existsByEmail(email))
            throw new DuplicateResourceException("Email already registered: " + email);

        Apartment apartment = apartmentService.find(r.apartmentId());
        Household household = resolveHousehold(r.apartmentId(), r.householdId());
        verificationCodeService.consumeCode(email, Purpose.REGISTRATION, r.verificationCode());

        User u = new User();
        u.setName(r.name());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setRole(Role.RESIDENT);
        u.setApartment(apartment);
        u.setHousehold(household);
        return userService.toResponse(userRepository.save(u));
    }

    public void requestPasswordReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null || user.getPasswordHash() == null) {
            log.info("Password reset code skipped (no password account) for {}", email);
            return;
        }
        verificationCodeService.sendCode(email, Purpose.PASSWORD_RESET);
    }

    @Transactional(noRollbackFor = BadRequestException.class)
    public String verifyPasswordResetCode(String rawEmail, String code) {
        return verificationCodeService.verifyResetCode(normalizeEmail(rawEmail), code);
    }

    @Transactional
    public void resetPassword(String rawEmail, String resetToken, String newPassword) {
        String email = normalizeEmail(rawEmail);
        AuthChallenge challenge = verificationCodeService.consumeResetToken(email, resetToken);
        User user = userRepository.findByEmail(email)
                .filter(existing -> existing.getPasswordHash() != null)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset request"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        verificationCodeService.delete(challenge);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = userRepository.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        // Google-only account ka password nahi hota, to password login nahi chalega
        if (u.getPasswordHash() == null || !passwordEncoder.matches(r.password(), u.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password");

        return toAuthResponse(u);
    }

    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest r) {
        GoogleTokenVerifier.GoogleUser g = googleTokenVerifier.verify(r.idToken());

        User user = userRepository.findByGoogleSub(g.sub()).orElse(null);

        if (user == null) {
            user = userRepository.findByEmail(g.email()).orElse(null);

            if (user != null) {
                user.setGoogleSub(g.sub());
            } else {
                if (r.apartmentId() == null)
                    throw new BadRequestException("apartmentId is required for first-time Google sign-up");

                Apartment apartment = apartmentService.find(r.apartmentId());
                Household household = resolveHousehold(r.apartmentId(), r.householdId());

                User u = new User();
                u.setName(displayName(g));
                u.setEmail(g.email());
                u.setPasswordHash(null);
                u.setGoogleSub(g.sub());
                u.setRole(Role.RESIDENT);
                u.setApartment(apartment);
                u.setHousehold(household);
                user = userRepository.save(u);
            }
        }
        return toAuthResponse(user);
    }

    private Household resolveHousehold(Long apartmentId, Long householdId) {
        if (householdId == null) return null;
        return householdRepository.findById(householdId)
                .filter(h -> h.getApartment().getId().equals(apartmentId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Household not found in this apartment: " + householdId));
    }

    private String displayName(GoogleTokenVerifier.GoogleUser g) {
        String name = g.name();
        if (name == null || name.isBlank()) name = g.email().substring(0, g.email().indexOf('@'));
        return name.length() > 100 ? name.substring(0, 100) : name;
    }

    private AuthResponse toAuthResponse(User u) {
        return new AuthResponse(jwtService.generateToken(u), "Bearer",
                u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }
}