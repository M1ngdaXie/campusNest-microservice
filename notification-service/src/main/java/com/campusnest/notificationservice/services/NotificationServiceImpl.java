package com.campusnest.notificationservice.services;

import com.campusnest.notificationservice.enums.NotificationChannel;
import com.campusnest.notificationservice.enums.NotificationStatus;
import com.campusnest.notificationservice.enums.NotificationType;
import com.campusnest.common.events.ListingInquiryEvent;
import com.campusnest.common.events.MessageReceivedEvent;
import com.campusnest.notificationservice.events.NotificationEvent;
import com.campusnest.common.events.UserRegisteredEvent;
import com.campusnest.notificationservice.models.Notification;
import com.campusnest.notificationservice.repository.NotificationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService{
    @Autowired
    private NotificationChannelService channelService;
    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationPreferenceService preferenceService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${app.frontend.url}")
    private String frontendUrl;
    @Override
    @Transactional
    public void processNotificationEvent(NotificationEvent event) {
        Notification notification = createNotification(event);
        channelService.sendNotification(notification);
    }

    @Override
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        NotificationEvent notificationEvent = NotificationEvent.builder()
                .userId(event.getUserId())
                .userEmail(event.getEmail())
                .userName(event.getFirstName())
                .type(NotificationType.USER_REGISTERED)
                .title("Welcome to CampusNest!")
                .message(String.format("Hi %s, welcome to CampusNest! Start exploring housing options now.",
                        event.getFirstName()))
                .actionUrl(frontendUrl +  "/housing/search")
                .build();
        processNotificationEvent(notificationEvent);
    }

    @Override
    public void handleMessageReceived(MessageReceivedEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("conversationId", event.getConversationId());
        metadata.put("senderId", event.getSenderUserId());

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .userId(event.getRecipientUserId())
                .userEmail(event.getRecipientEmail())
                .userName(null)  // We don't have recipient name in this event
                .type(NotificationType.MESSAGE_RECEIVED)
                .title("New Message from " + event.getSenderName())
                .message(event.getMessagePreview())
                .metadata(metadata)
                .actionUrl(frontendUrl + "/messages/" + event.getConversationId())
                .build();

        processNotificationEvent(notificationEvent);
    }

    @Override
    public void handleListingInquiry(ListingInquiryEvent event) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("listingId", event.getListingId());
        metadata.put("conversationId", event.getConversationId());
        metadata.put("inquirerId", event.getInquirerUserId());

        NotificationEvent notificationEvent = NotificationEvent.builder()
                .userId(event.getLandlordUserId())
                .userEmail(event.getLandlordEmail())
                .userName(null)  // We don't have landlord name in this event
                .type(NotificationType.LISTING_INQUIRY)
                .title("New Inquiry on " + event.getListingTitle())
                .message(String.format("%s is interested in your listing '%s'",
                        event.getInquirerName(), event.getListingTitle()))
                .metadata(metadata)
                .actionUrl(frontendUrl + "/messages/" + event.getConversationId())
                .build();

        processNotificationEvent(notificationEvent);
    }

    @Override
    public Notification createNotification(NotificationEvent event) {
        String metadataJson = null;
        if (event.getMetadata() != null) {
            try {
                metadataJson = objectMapper.writeValueAsString(event.getMetadata());
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize metadata", e);
            }
        }

        Notification notification = Notification.builder()
                .userId(event.getUserId())
                .userEmail(event.getUserEmail())  // Pass email directly from event
                .type(event.getType())
                .channel(NotificationChannel.IN_APP) // Default to in-app
                .title(event.getTitle())
                .message(event.getMessage())
                .metadata(metadataJson)
                .actionUrl(event.getActionUrl())
                .status(NotificationStatus.PENDING)
                .build();

        // Save to database (userEmail won't be persisted since it's @Transient)
        Notification savedNotification = notificationRepository.save(notification);

        // Restore transient fields to the saved notification for use by channel handlers
        savedNotification.setUserEmail(event.getUserEmail());

        return savedNotification;
    }

    @Override
    @Cacheable(value = "notifications", key = "#userId + '-' + #pageable.pageNumber")
    public Page<Notification> getUserNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    @Cacheable(value = "unread-count", key = "#userId")
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"notifications", "unread-count"}, key = "#userId")
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.markAsRead(List.of(notificationId), userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"notifications", "unread-count"}, key = "#userId")
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    @CacheEvict(value = {"notifications", "unread-count"}, key = "#userId")
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            if (notification.getUserId().equals(userId)) {
                notificationRepository.delete(notification);
            }
        });
    }

    /**
     * Scheduled task to cleanup expired notifications.
     * Runs daily at 2 AM to remove old notifications and free up database space.
     */
    @Override
    @Transactional
    @Scheduled(cron = "0 0 2 * * ?")  // Run at 2 AM every day
    public int cleanupExpiredNotifications() {
        int deleted = notificationRepository.deleteExpiredNotifications(LocalDateTime.now());
        log.info("Scheduled cleanup: Deleted {} expired notifications", deleted);
        return deleted;
    }
}
