package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AlertRunResponse;
import com.infosys.Water_Usage_Monitoring.entity.AlertSettings;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock WaterUsageLogRepository logRepo;
    @Mock UserRepository userRepo;
    @Mock NotificationRepository notificationRepo;
    @Mock AlertSettingsService settingsService;
    @Mock AlertMailer mailer;
    @Spy AnomalyDetector detector = new AnomalyDetector();
    @InjectMocks AlertService service;

    private final LocalDate day = LocalDate.of(2026, 9, 7);
    private Household household;
    private User resident;
    private User admin;

    @BeforeEach
    void setUp() {
        Apartment apartment = new Apartment();
        apartment.setId(1L);

        household = new Household();
        household.setId(1L);
        household.setFlatNumber("B-201");
        household.setApartment(apartment);

        resident = user(5L, Role.RESIDENT, "sita@test.com");
        admin = user(1L, Role.ADMIN, "admin@water.local");
    }

    private User user(long id, Role role, String email) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        u.setEmail(email);
        return u;
    }

    private WaterUsageLog log(long id, String consumption) {
        WaterUsageLog l = new WaterUsageLog();
        l.setId(id);
        l.setHousehold(household);
        l.setReadingDate(day);
        l.setConsumptionLitres(new BigDecimal(consumption));
        return l;
    }

    private List<WaterUsageLog> history(String... values) {
        return Stream.of(values).map(v -> log(0L, v)).toList();
    }

    private AlertSettings settings(String threshold, boolean leak, boolean email) {
        AlertSettings s = new AlertSettings();
        s.setUsageThresholdLitres(threshold == null ? null : new BigDecimal(threshold));
        s.setLeakDetectionEnabled(leak);
        s.setEmailEnabled(email);
        return s;
    }

    private void stubHistory(String... values) {
        when(logRepo.findTop30ByHouseholdIdAndReadingDateLessThanAndConsumptionLitresGreaterThanOrderByReadingDateDesc(
                1L, day, BigDecimal.ZERO)).thenReturn(history(values));
    }

    private void stubRecipients(boolean withResident) {
        when(userRepo.findByHouseholdId(1L)).thenReturn(withResident ? List.of(resident) : List.of());
        when(userRepo.findByApartmentIdAndRole(1L, Role.ADMIN)).thenReturn(List.of(admin));
        when(notificationRepo.existsByUsageLogIdAndTypeAndUserId(anyLong(), any(NotificationType.class), anyLong()))
                .thenReturn(false);
        when(notificationRepo.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void spike_createsLeakNotificationForResidentAndAdmin_andSendsEmails() {
        WaterUsageLog spike = log(10L, "400");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(spike));
        when(settingsService.getOrCreate(1L)).thenReturn(settings(null, true, true));
        stubHistory("100", "110", "95", "105", "100");
        stubRecipients(true);
        when(mailer.send(anyString(), anyString(), anyString())).thenReturn(true);

        AlertRunResponse r = service.runChecks();

        assertEquals(1, r.readingsChecked());
        assertEquals(1, r.alertsCreated());
        assertEquals(2, r.emailsSent());
        assertTrue(spike.isAlertChecked());

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, times(2)).save(captor.capture());
        assertEquals(NotificationType.LEAK_SUSPECTED, captor.getAllValues().get(0).getType());
        assertTrue(captor.getAllValues().get(0).getMessage().contains("B-201"));
        assertTrue(captor.getAllValues().get(0).isEmailSent());
    }

    @Test
    void normalReading_createsNoAlert_butIsMarkedChecked() {
        WaterUsageLog normal = log(10L, "110");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(normal));
        when(settingsService.getOrCreate(1L)).thenReturn(settings(null, true, true));
        stubHistory("100", "110", "95", "105", "100");

        AlertRunResponse r = service.runChecks();

        assertEquals(1, r.readingsChecked());
        assertEquals(0, r.alertsCreated());
        assertTrue(normal.isAlertChecked());
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void baselineReadingWithZeroUsage_isSkipped() {
        WaterUsageLog baseline = log(10L, "0");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(baseline));

        AlertRunResponse r = service.runChecks();

        assertEquals(1, r.readingsChecked());
        assertEquals(0, r.alertsCreated());
        assertTrue(baseline.isAlertChecked());
        verify(settingsService, never()).getOrCreate(anyLong());
    }

    @Test
    void thresholdCrossed_withoutEnoughHistory_createsOveruseAlert_forAdminOnlyWhenNoResident() {
        WaterUsageLog big = log(10L, "500");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(big));
        when(settingsService.getOrCreate(1L)).thenReturn(settings("350", true, false));
        stubHistory("100", "110");
        stubRecipients(false);

        AlertRunResponse r = service.runChecks();

        assertEquals(1, r.alertsCreated());
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepo, times(1)).save(captor.capture());
        assertEquals(NotificationType.OVERUSE, captor.getValue().getType());
        assertEquals(1L, captor.getValue().getUser().getId());
    }

    @Test
    void alreadyNotified_isNotDuplicated() {
        WaterUsageLog spike = log(10L, "400");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(spike));
        when(settingsService.getOrCreate(1L)).thenReturn(settings(null, true, true));
        stubHistory("100", "110", "95", "105", "100");
        when(userRepo.findByHouseholdId(1L)).thenReturn(List.of(resident));
        when(userRepo.findByApartmentIdAndRole(1L, Role.ADMIN)).thenReturn(List.of(admin));
        when(notificationRepo.existsByUsageLogIdAndTypeAndUserId(anyLong(), any(NotificationType.class), anyLong()))
                .thenReturn(true);

        AlertRunResponse r = service.runChecks();

        assertEquals(0, r.alertsCreated());
        verify(notificationRepo, never()).save(any(Notification.class));
    }

    @Test
    void emailSwitchedOff_savesNotificationsWithoutSendingEmail() {
        WaterUsageLog spike = log(10L, "400");
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of(spike));
        when(settingsService.getOrCreate(1L)).thenReturn(settings(null, true, false));
        stubHistory("100", "110", "95", "105", "100");
        stubRecipients(true);

        AlertRunResponse r = service.runChecks();

        assertEquals(1, r.alertsCreated());
        assertEquals(0, r.emailsSent());
        verify(mailer, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void noPendingReadings_doesNothing() {
        when(logRepo.findTop500ByAlertCheckedFalseOrderByReadingDateAscIdAsc()).thenReturn(List.of());

        AlertRunResponse r = service.runChecks();

        assertEquals(0, r.readingsChecked());
        assertFalse(false);
    }
}