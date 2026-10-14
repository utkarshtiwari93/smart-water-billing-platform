package com.infosys.Water_Usage_Monitoring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApartmentRequest(
        @NotBlank @Size(max = 150) String name,
        @Size(max = 255) String address,
        @Size(max = 100) String city) {}