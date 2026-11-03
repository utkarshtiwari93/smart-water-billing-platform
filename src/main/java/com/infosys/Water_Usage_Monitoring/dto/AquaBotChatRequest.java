package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AquaBotChatRequest(
        @NotBlank
        @Size(max = 2000)
        String message
) {
}
