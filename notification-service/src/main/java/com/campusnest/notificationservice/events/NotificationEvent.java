package com.campusnest.notificationservice.events;

import com.campusnest.notificationservice.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent implements Serializable {
    private Long userId;
    private String userEmail;  // Email of the user to notify
    private String userName;   // Name of the user (optional, for personalization)
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> metadata;
    private String actionUrl;
}