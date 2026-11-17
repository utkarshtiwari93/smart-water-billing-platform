package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmailRequest(
        @NotBlank @Email @Size(max = 150) String email) {
}
