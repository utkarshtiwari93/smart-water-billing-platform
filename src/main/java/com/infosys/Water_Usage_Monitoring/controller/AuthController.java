package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AuthResponse;
import com.infosys.Water_Usage_Monitoring.dto.EmailRequest;
import com.infosys.Water_Usage_Monitoring.dto.GoogleLoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.LoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.MessageResponse;
import com.infosys.Water_Usage_Monitoring.dto.PasswordResetCodeRequest;
import com.infosys.Water_Usage_Monitoring.dto.PasswordResetRequest;
import com.infosys.Water_Usage_Monitoring.dto.PasswordResetTokenResponse;
import com.infosys.Water_Usage_Monitoring.dto.RegisterRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest r) {
        return authService.register(r);
    }

    @PostMapping("/register/send-code")
    public MessageResponse sendRegistrationCode(@Valid @RequestBody EmailRequest r) {
        authService.sendRegistrationCode(r.email());
        return new MessageResponse("If this email can be registered, a verification code has been sent.");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return authService.login(r);
    }

    @PostMapping("/password/forgot")
    public MessageResponse requestPasswordReset(@Valid @RequestBody EmailRequest r) {
        authService.requestPasswordReset(r.email());
        return new MessageResponse("If an account exists for this email, a password reset code has been sent.");
    }

    @PostMapping("/password/verify-code")
    public PasswordResetTokenResponse verifyPasswordResetCode(@Valid @RequestBody PasswordResetCodeRequest r) {
        return new PasswordResetTokenResponse(authService.verifyPasswordResetCode(r.email(), r.code()));
    }

    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody PasswordResetRequest r) {
        authService.resetPassword(r.email(), r.resetToken(), r.newPassword());
        return new MessageResponse("Password reset successfully. You can now sign in.");
    }

    @PostMapping("/google")
    public AuthResponse google(@Valid @RequestBody GoogleLoginRequest r) {
        return authService.googleLogin(r);
    }
}