package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.ResidentCycleSummary;
import com.infosys.Water_Usage_Monitoring.service.ResidentCycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resident")
@RequiredArgsConstructor
public class ResidentCycleController {

    private final ResidentCycleService service;

    @GetMapping("/current-cycle")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResidentCycleSummary currentCycle(Authentication authentication) {
        return service.current(authentication.getName());
    }
}
