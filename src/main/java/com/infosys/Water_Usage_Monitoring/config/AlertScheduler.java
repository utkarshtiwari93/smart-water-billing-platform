package com.infosys.Water_Usage_Monitoring.config;

import com.infosys.Water_Usage_Monitoring.dto.AlertRunResponse;
import com.infosys.Water_Usage_Monitoring.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.alerts.scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class AlertScheduler {

    private final AlertService alertService;

    @Scheduled(initialDelayString = "${app.alerts.initial-delay-ms:60000}",
            fixedDelayString = "${app.alerts.interval-ms:900000}")
    public void run() {
        try {
            AlertRunResponse r = alertService.runChecks();
            if (r.readingsChecked() > 0) {
                log.info("Alert check: {} readings checked, {} alerts, {} emails",
                        r.readingsChecked(), r.alertsCreated(), r.emailsSent());
            }
        } catch (Exception e) {
            log.error("Alert check failed", e);
        }
    }
}