package com.campusnest.messagingservice.models;

import com.campusnest.messagingservice.enums.MessageStatusType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "message_status",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id", "status"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    // Store user ID instead of ManyToOne relationship (microservices pattern)
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatusType status;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }

    public static MessageStatus createSentStatus(Message message, Long userId) {
        MessageStatus status = new MessageStatus();
        status.setMessage(message);
        status.setUserId(userId);
        status.setStatus(MessageStatusType.SENT);
        status.setTimestamp(LocalDateTime.now());
        return status;
    }

    public static MessageStatus createDeliveredStatus(Message message, Long userId) {
        MessageStatus status = new MessageStatus();
        status.setMessage(message);
        status.setUserId(userId);
        status.setStatus(MessageStatusType.DELIVERED);
        status.setTimestamp(LocalDateTime.now());
        return status;
    }

    public static MessageStatus createReadStatus(Message message, Long userId) {
        MessageStatus status = new MessageStatus();
        status.setMessage(message);
        status.setUserId(userId);
        status.setStatus(MessageStatusType.READ);
        status.setTimestamp(LocalDateTime.now());
        return status;
    }
}