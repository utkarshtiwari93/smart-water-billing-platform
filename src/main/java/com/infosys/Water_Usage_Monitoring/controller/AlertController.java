package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AlertRunResponse;
import com.infosys.Water_Usage_Monitoring.dto.AlertSettingsRequest;
import com.infosys.Water_Usage_Monitoring.dto.AlertSettingsResponse;
import com.infosys.Water_Usage_Monitoring.service.AlertService;
import com.infosys.Water_Usage_Monitoring.service.AlertSettingsService;
import com.infosys.Water_Usage_Monitoring.security.AdminApartmentScope;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AlertController {

    private final AlertSettingsService settingsService;
    private final AlertService alertService;
    private final AdminApartmentScope apartmentScope;

    @GetMapping("/apartments/{apartmentId}/alert-settings")
    public AlertSettingsResponse getSettings(@PathVariable Long apartmentId, Authentication authentication) {
        apartmentScope.apartment(apartmentId, authentication.getName());
        return settingsService.get(apartmentId);
    }

    @PutMapping("/apartments/{apartmentId}/alert-settings")
    public AlertSettingsResponse updateSettings(@PathVariable Long apartmentId,
                                                @Valid @RequestBody AlertSettingsRequest r,
                                                Authentication authentication) {
        apartmentScope.apartment(apartmentId, authentication.getName());
        return settingsService.update(apartmentId, r);
    }

    @PostMapping("/alerts/run")
    public AlertRunResponse runNow(Authentication authentication) {
        return alertService.runChecks(apartmentScope.assignedApartmentId(authentication.getName()));
    }
}