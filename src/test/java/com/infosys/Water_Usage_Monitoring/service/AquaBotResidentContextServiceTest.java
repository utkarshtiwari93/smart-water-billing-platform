package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.access.AccessDeniedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AquaBotResidentContextServiceTest {

    @Mock UserRepository userRepository;
    @Mock WaterUsageLogRepository usageRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks AquaBotResidentContextService contextService;

    @Test
    void forResidentA_neverQueriesResidentBRecords() {
        User resident = resident(101L, 201L, Role.RESIDENT);
        when(userRepository.findByEmail("resident-a@example.com")).thenReturn(Optional.of(resident));
        when(usageRepository.findTop51ByHouseholdIdOrderByReadingDateDescIdDesc(201L)).thenReturn(List.of());
        when(invoiceRepository.findTop13ByHouseholdIdOrderByIssuedAtDescIdDesc(201L)).thenReturn(List.of());
        when(notificationRepository.findTop21ByUserIdAndTypeInOrderByCreatedAtDescIdDesc(
                101L, List.of(NotificationType.LEAK_SUSPECTED, NotificationType.OVERUSE)))
                .thenReturn(List.of());

        AquaBotResidentContext context = contextService.forResident("resident-a@example.com");

        assertTrue(context.householdAssigned());
        assertTrue(context.readings().isEmpty());
        assertTrue(context.invoices().isEmpty());
        assertTrue(context.alerts().isEmpty());
        assertFalse(context.readingsTruncated());
        verify(usageRepository).findTop51ByHouseholdIdOrderByReadingDateDescIdDesc(201L);
        verify(invoiceRepository).findTop13ByHouseholdIdOrderByIssuedAtDescIdDesc(201L);
        verify(notificationRepository).findTop21ByUserIdAndTypeInOrderByCreatedAtDescIdDesc(
                101L, List.of(NotificationType.LEAK_SUSPECTED, NotificationType.OVERUSE));
        verify(usageRepository, never()).findTop51ByHouseholdIdOrderByReadingDateDescIdDesc(202L);
        verify(invoiceRepository, never()).findTop13ByHouseholdIdOrderByIssuedAtDescIdDesc(202L);
        verifyNoMoreInteractions(usageRepository, invoiceRepository, notificationRepository);
    }

    @Test
    void forResident_withoutHousehold_returnsEmptyContextWithoutQueryingPersonalRecords() {
        User resident = resident(101L, null, Role.RESIDENT);
        when(userRepository.findByEmail("resident@example.com")).thenReturn(Optional.of(resident));

        AquaBotResidentContext context = contextService.forResident("resident@example.com");

        assertFalse(context.householdAssigned());
        assertTrue(context.readings().isEmpty());
        assertTrue(context.invoices().isEmpty());
        assertTrue(context.alerts().isEmpty());
        verifyNoInteractions(usageRepository, invoiceRepository, notificationRepository);
    }

    @Test
    void forResident_rejectsPersistedNonResidentRole() {
        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(resident(101L, 201L, Role.ADMIN)));

        assertThrows(AccessDeniedException.class, () -> contextService.forResident("admin@example.com"));

        verifyNoInteractions(usageRepository, invoiceRepository, notificationRepository);
    }

    @Test
    void forResident_unknownAuthenticatedIdentityFailsWithoutFetchingRecords() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> contextService.forResident("missing@example.com"));

        verifyNoInteractions(usageRepository, invoiceRepository, notificationRepository);
    }

    private User resident(Long userId, Long householdId, Role role) {
        User user = new User();
        user.setId(userId);
        user.setEmail(userId == 101L ? "resident@example.com" : "other@example.com");
        user.setRole(role);
        if (householdId != null) {
            Household household = new Household();
            household.setId(householdId);
            user.setHousehold(household);
        }
        return user;
    }
}
