package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AlertSettingsRequest;
import com.infosys.Water_Usage_Monitoring.dto.AlertSettingsResponse;
import com.infosys.Water_Usage_Monitoring.entity.AlertSettings;
import com.infosys.Water_Usage_Monitoring.repository.AlertSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlertSettingsService {

    private final AlertSettingsRepository repo;
    private final ApartmentService apartmentService;

    @Transactional
    public AlertSettings getOrCreate(Long apartmentId) {
        return repo.findByApartmentId(apartmentId).orElseGet(() -> {
            AlertSettings s = new AlertSettings();
            s.setApartment(apartmentService.find(apartmentId));
            s.setLeakDetectionEnabled(true);
            s.setEmailEnabled(true);
            return repo.save(s);
        });
    }

    @Transactional
    public AlertSettingsResponse get(Long apartmentId) {
        return toDto(getOrCreate(apartmentId));
    }

    @Transactional
    public AlertSettingsResponse update(Long apartmentId, AlertSettingsRequest r) {
        AlertSettings s = getOrCreate(apartmentId);
        s.setUsageThresholdLitres(r.usageThresholdLitres());
        s.setLeakDetectionEnabled(r.leakDetectionEnabled());
        s.setEmailEnabled(r.emailEnabled());
        return toDto(s);
    }

    private AlertSettingsResponse toDto(AlertSettings s) {
        String rule = "Leak is suspected when a reading is above (average + " + (int) AnomalyDetector.SIGMA
                + " x standard deviation) of the flat's last 30 readings. Needs at least "
                + AnomalyDetector.MIN_HISTORY + " earlier readings.";
        return new AlertSettingsResponse(s.getApartment().getId(), s.getUsageThresholdLitres(),
                s.isLeakDetectionEnabled(), s.isEmailEnabled(), rule);
    }
}