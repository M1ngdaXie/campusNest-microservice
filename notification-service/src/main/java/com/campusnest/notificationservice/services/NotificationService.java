package com.campusnest.notificationservice.services;

import com.campusnest.common.events.UserRegisteredEvent;
import com.campusnest.common.events.MessageReceivedEvent;
import com.campusnest.common.events.ListingInquiryEvent;
import com.campusnest.notificationservice.events.NotificationEvent;
import com.campusnest.notificationservice.models.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    // Process generic notification event
    void processNotificationEvent(NotificationEvent event);

    // Handle specific events
    void handleUserRegistered(UserRegisteredEvent event);
    void handleMessageReceived(MessageReceivedEvent event);
    void handleListingInquiry(ListingInquiryEvent event);

    // Notification CRUD
    Notification createNotification(NotificationEvent event);
    Page<Notification> getUserNotifications(Long userId, Pageable pageable);
    long getUnreadCount(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void markAllAsRead(Long userId);
    void deleteNotification(Long notificationId, Long userId);

    // Cleanup
    int cleanupExpiredNotifications();
}