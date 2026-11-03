package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByUserIdAndReadFlagFalseOrderByCreatedAtDesc(Long userId);

    List<Notification> findTop21ByUserIdAndTypeInOrderByCreatedAtDescIdDesc(
            Long userId, List<NotificationType> types);

    @Query("""
            select n from Notification n
            where n.household.apartment.id = :apartmentId
              and n.type in :types
            order by n.createdAt desc, n.id desc
            """)
    List<Notification> findLatestApartmentAlerts(
            @Param("apartmentId") Long apartmentId,
            @Param("types") List<NotificationType> types,
            org.springframework.data.domain.Pageable pageable);

    long countByUserIdAndReadFlagFalse(Long userId);

    boolean existsByUsageLogIdAndTypeAndUserId(Long usageLogId, NotificationType type, Long userId);

    @Modifying
    @Query("update Notification n set n.readFlag = true where n.user.id = :userId and n.readFlag = false")
    int markAllRead(@Param("userId") Long userId);
}