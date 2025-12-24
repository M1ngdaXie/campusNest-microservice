package com.campusnest.notificationservice.repository;

import com.campusnest.notificationservice.enums.NotificationStatus;
import com.campusnest.notificationservice.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Notification> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status = :status")
    long countByUserIdAndStatus(Long userId, NotificationStatus status);

    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.status IN ('PENDING', 'SENT')")
    long countUnreadByUserId(Long userId);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.id IN :ids AND n.userId = :userId")
    int markAsRead(List<Long> ids, Long userId, LocalDateTime readAt);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :readAt WHERE n.userId = :userId AND n.status IN ('PENDING', 'SENT')")
    int markAllAsReadByUserId(Long userId, LocalDateTime readAt);

    @Modifying
    @Query("DELETE FROM Notification n WHERE n.expiresAt < :now")
    int deleteExpiredNotifications(LocalDateTime now);

    List<Notification> findByStatusAndCreatedAtBefore(NotificationStatus status, LocalDateTime createdAt);
}
