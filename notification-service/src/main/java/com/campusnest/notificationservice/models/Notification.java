package com.campusnest.notificationservice.models;

import com.campusnest.notificationservice.enums.NotificationChannel;
import com.campusnest.notificationservice.enums.NotificationStatus;
import com.campusnest.notificationservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_user_status", columnList = "userId,status"),
        @Index(name = "idx_user_created", columnList = "userId,createdAt"),
        @Index(name = "idx_status", columnList = "status")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    // Transient field - not persisted to database, used for email sending
    @Transient
    private String userEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationChannel channel;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(length = 500)
    private String actionUrl;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
    private LocalDateTime readAt;
    private LocalDateTime sendAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Set default expiry date before persisting to database.
     * This ensures each notification gets a unique expiry time (30 days from creation).
     */
    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(30);
        }
    }
}
