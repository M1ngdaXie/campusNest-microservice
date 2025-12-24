package com.campusnest.notificationservice.services;

import com.campusnest.notificationservice.enums.NotificationChannel;
import com.campusnest.notificationservice.models.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationChannelService {
    @Autowired
    private  EmailChannelHandler emailHandler;
    @Autowired
    private InAppChannelHandler inAppHandler;

    @Autowired
    private NotificationPreferenceService preferenceService;

    public void sendNotification(Notification notification) {
        // Check user preferences
        boolean emailEnabled = preferenceService.isChannelEnabled(
                notification.getUserId(), notification.getType(), NotificationChannel.EMAIL);
        boolean inAppEnabled = preferenceService.isChannelEnabled(
                notification.getUserId(), notification.getType(), NotificationChannel.IN_APP);

        // Send via enabled channels
        if (inAppEnabled) {
            inAppHandler.send(notification);
        }

        if (emailEnabled) {
            emailHandler.send(notification);
        }
    }

}
