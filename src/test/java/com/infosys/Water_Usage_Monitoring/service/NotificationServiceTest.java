package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.NotificationResponse;
import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock NotificationRepository repo;
    @Mock UserRepository userRepo;
    @InjectMocks NotificationService service;

    private User me;

    @BeforeEach
    void setUp() {
        me = new User();
        me.setId(5L);
        me.setEmail("sita@test.com");
    }

    private Notification notification(long id, User owner) {
        Notification n = new Notification();
        n.setId(id);
        n.setUser(owner);
        n.setType(NotificationType.LEAK_SUSPECTED);
        n.setMessage("Possible leak");
        return n;
    }

    @Test
    void list_all_returnsOwnNotifications() {
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.findByUserIdOrderByCreatedAtDesc(5L)).thenReturn(List.of(notification(1L, me)));

        List<NotificationResponse> r = service.list("sita@test.com", false);

        assertEquals(1, r.size());
        assertEquals("LEAK_SUSPECTED", r.get(0).type());
    }

    @Test
    void list_unreadOnly_usesUnreadQuery() {
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(5L)).thenReturn(List.of());

        assertTrue(service.list("sita@test.com", true).isEmpty());
        verify(repo, never()).findByUserIdOrderByCreatedAtDesc(5L);
    }

    @Test
    void unreadCount_returnsCount() {
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.countByUserIdAndReadFlagFalse(5L)).thenReturn(3L);

        assertEquals(3L, service.unreadCount("sita@test.com"));
    }

    @Test
    void markRead_ownNotification_setsReadFlag() {
        Notification n = notification(1L, me);
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.findById(1L)).thenReturn(Optional.of(n));

        NotificationResponse r = service.markRead(1L, "sita@test.com");

        assertTrue(r.read());
        assertTrue(n.isReadFlag());
    }

    @Test
    void markRead_someoneElsesNotification_isRejected() {
        User other = new User();
        other.setId(9L);
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.findById(1L)).thenReturn(Optional.of(notification(1L, other)));

        assertThrows(AccessDeniedException.class, () -> service.markRead(1L, "sita@test.com"));
    }

    @Test
    void markRead_unknownNotification_throwsNotFound() {
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.markRead(99L, "sita@test.com"));
    }

    @Test
    void markAllRead_updatesOnlyMyNotifications() {
        when(userRepo.findByEmail("sita@test.com")).thenReturn(Optional.of(me));
        when(repo.markAllRead(5L)).thenReturn(4);

        assertEquals(4, service.markAllRead("sita@test.com"));
    }
}