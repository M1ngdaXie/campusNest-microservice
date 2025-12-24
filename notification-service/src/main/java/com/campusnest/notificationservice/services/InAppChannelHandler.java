package com.campusnest.notificationservice.services;

import com.campusnest.notificationservice.enums.NotificationStatus;
import com.campusnest.notificationservice.models.Notification;
import com.campusnest.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class InAppChannelHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Async
    public void send(Notification notification) {
        try {
            // Send via WebSocket to user's notification queue
            messagingTemplate.convertAndSendToUser(
                    notification.getUserId().toString(),
                    "/queue/notifications",
                    notification
            );

            notification.setStatus(NotificationStatus.SENT);
            notification.setSendAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("In-app notification sent to userId: {}", notification.getUserId());
        } catch (Exception e) {
            log.error("Failed to send in-app notification: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }
}
