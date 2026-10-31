package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AlertRunResponse;
import com.infosys.Water_Usage_Monitoring.entity.AlertSettings;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final WaterUsageLogRepository logRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notificationRepo;
    private final AlertSettingsService settingsService;
    private final AnomalyDetector detector;
    private final AlertMailer mailer;

    @Transactional
    public AlertRunResponse runChecks() {
        List<WaterUsageLog> pending = logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc();
        Map<Long, AlertSettings> settingsByApartment = new HashMap<>();
        int alerts = 0;
        int emails = 0;

        for (WaterUsageLog log : pending) {
            if (log.getConsumptionLitres().signum() > 0) {
                Household household = log.getHousehold();
                AlertSettings settings = settingsByApartment.computeIfAbsent(
                        household.getApartment().getId(), settingsService::getOrCreate);

                List<BigDecimal> history = logRepo
                        .findTop30ByHouseholdIdAndReadingDateLessThanAndConsumptionLitresGreaterThanOrderByReadingDateDesc(
                                household.getId(), log.getReadingDate(), BigDecimal.ZERO)
                        .stream().map(WaterUsageLog::getConsumptionLitres).toList();

                AnomalyDetector.Verdict verdict = detector.analyze(log.getConsumptionLitres(), history,
                        settings.getUsageThresholdLitres(), settings.isLeakDetectionEnabled());

                if (verdict.leak()) {
                    int[] r = notifyAll(log, household, settings, NotificationType.LEAK_SUSPECTED, verdict);
                    if (r[0] > 0) alerts++;
                    emails += r[1];
                }
                if (verdict.overuse()) {
                    int[] r = notifyAll(log, household, settings, NotificationType.OVERUSE, verdict);
                    if (r[0] > 0) alerts++;
                    emails += r[1];
                }
            }
            log.setAlertChecked(true);
        }
        return new AlertRunResponse(pending.size(), alerts, emails);
    }

    // returns {notifications created, emails sent}
    private int[] notifyAll(WaterUsageLog log, Household household, AlertSettings settings,
                            NotificationType type, AnomalyDetector.Verdict verdict) {
        Map<Long, User> recipients = new LinkedHashMap<>();
        for (User u : userRepo.findByHouseholdId(household.getId())) {
            if (u.getRole() == Role.RESIDENT) recipients.put(u.getId(), u);
        }
        for (User u : userRepo.findByApartmentIdAndRole(household.getApartment().getId(), Role.ADMIN)) {
            recipients.put(u.getId(), u);
        }

        int created = 0;
        int emailsSent = 0;
        List<Long> done = new ArrayList<>();

        for (User user : recipients.values()) {
            if (notificationRepo.existsByUsageLogIdAndTypeAndUserId(log.getId(), type, user.getId())) continue;

            String message = buildMessage(user.getRole(), type, household, log, settings, verdict);

            Notification n = new Notification();
            n.setUser(user);
            n.setHousehold(household);
            n.setUsageLogId(log.getId());
            n.setType(type);
            n.setMessage(message);

            if (settings.isEmailEnabled()
                    && mailer.send(user.getEmail(), subject(type, household), message)) {
                n.setEmailSent(true);
                emailsSent++;
            }
            notificationRepo.save(n);
            created++;
            done.add(user.getId());
        }
        return new int[]{created, emailsSent};
    }

    private String subject(NotificationType type, Household h) {
        return (type == NotificationType.LEAK_SUSPECTED ? "Possible water leak" : "High water usage")
                + " - flat " + h.getFlatNumber();
    }

    private String buildMessage(Role role, NotificationType type, Household h, WaterUsageLog log,
                                AlertSettings settings, AnomalyDetector.Verdict v) {
        String flat = h.getFlatNumber();
        String used = litres(log.getConsumptionLitres());
        String date = log.getReadingDate().toString();
        boolean admin = role == Role.ADMIN;

        if (type == NotificationType.LEAK_SUSPECTED) {
            return admin
                    ? String.format("Flat %s: possible leak. Used %s L on %s (usual about %d L, alert limit %d L).",
                    flat, used, date, Math.round(v.mean()), Math.round(v.limit()))
                    : String.format("Possible leak: your flat %s used %s L on %s, much more than your usual %d L. "
                    + "Please check taps, tanks and pipes.", flat, used, date, Math.round(v.mean()));
        }
        String limit = litres(settings.getUsageThresholdLitres());
        return admin
                ? String.format("Flat %s used %s L on %s, above the apartment limit of %s L.", flat, used, date, limit)
                : String.format("High usage: your flat %s used %s L on %s, above the limit of %s L set by your society.",
                flat, used, date, limit);
    }

    private String litres(BigDecimal v) {
        return v.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}