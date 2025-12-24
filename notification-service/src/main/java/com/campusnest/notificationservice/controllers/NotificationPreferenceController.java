package com.campusnest.notificationservice.controllers;

import com.campusnest.notificationservice.enums.NotificationChannel;
import com.campusnest.notificationservice.enums.NotificationType;
import com.campusnest.notificationservice.models.NotificationPreference;
import com.campusnest.notificationservice.services.NotificationPreferenceService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications/preferences")
public class NotificationPreferenceController {

    @Autowired
    private NotificationPreferenceService preferenceService;

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        throw new RuntimeException("User not authenticated");
    }

    @GetMapping
    public ResponseEntity<List<NotificationPreference>> getPreferences() {
        Long userId = getCurrentUserId();
        List<NotificationPreference> preferences = preferenceService.getUserPreferences(userId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping
    public ResponseEntity<NotificationPreference> updatePreference(
            @RequestBody UpdatePreferenceRequest request) {
        Long userId = getCurrentUserId();
        NotificationPreference updated = preferenceService.updatePreference(
                userId, request.getType(), request.getChannel(), request.isEnabled());
        return ResponseEntity.ok(updated);
    }

    @Data
    static class UpdatePreferenceRequest {
        private NotificationType type;
        private NotificationChannel channel;
        private boolean enabled;
    }
}
