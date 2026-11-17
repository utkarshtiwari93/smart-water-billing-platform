package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetCodeRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a six-digit verification code") String code) {
}
