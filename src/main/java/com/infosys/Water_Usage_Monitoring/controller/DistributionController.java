package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.service.DistributionService;
import com.infosys.Water_Usage_Monitoring.security.AdminApartmentScope;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/billing-cycles/{cycleId}/distribution")
@RequiredArgsConstructor
public class DistributionController {

    private final DistributionService service;
    private final AdminApartmentScope apartmentScope;

    @GetMapping
    public DistributionResponse distribution(@PathVariable Long cycleId, Authentication authentication) {
        apartmentScope.cycle(cycleId, authentication.getName());
        return service.calculate(cycleId);
    }
}