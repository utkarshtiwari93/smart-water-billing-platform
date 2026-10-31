package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.NotificationResponse;
import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repo;
    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email, boolean unreadOnly) {
        User user = findUser(email);
        List<Notification> items = unreadOnly
                ? repo.findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(user.getId())
                : repo.findByUserIdOrderByCreatedAtDesc(user.getId());
        return items.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        return repo.countByUserIdAndReadFlagFalse(findUser(email).getId());
    }

    @Transactional
    public NotificationResponse markRead(Long id, String email) {
        User user = findUser(email);
        Notification n = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
        if (!n.getUser().getId().equals(user.getId()))
            throw new AccessDeniedException("You can only change your own notifications");
        n.setReadFlag(true);
        return toDto(n);
    }

    @Transactional
    public int markAllRead(String email) {
        return repo.markAllRead(findUser(email).getId());
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private NotificationResponse toDto(Notification n) {
        return new NotificationResponse(n.getId(), n.getType().name(), n.getMessage(),
                n.getHousehold() == null ? null : n.getHousehold().getId(),
                n.getHousehold() == null ? null : n.getHousehold().getFlatNumber(),
                n.isReadFlag(), n.getCreatedAt());
    }
}