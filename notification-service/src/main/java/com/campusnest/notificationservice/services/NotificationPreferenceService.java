package com.campusnest.notificationservice.services;

import com.campusnest.notificationservice.enums.NotificationChannel;
import com.campusnest.notificationservice.enums.NotificationType;
import com.campusnest.notificationservice.models.NotificationPreference;
import com.campusnest.notificationservice.repository.NotificationPreferenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationPreferenceService {
    @Autowired
    private NotificationPreferenceRepository preferenceRepository;

    @Cacheable(value = "preference", key = "#userId")
    public List<NotificationPreference> getUserPreferences(Long userId){
        return preferenceRepository.findByUserId(userId);
    }
    public boolean isChannelEnabled(Long userId, NotificationType type, NotificationChannel channel){
        return preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                .map(NotificationPreference::getEnabled)
                .orElse(true); // Default to enabled if no preference set
    }
    @Transactional
    @CacheEvict(value = "preference", key = "#userId")
    public NotificationPreference updatePreference(
            Long userId, NotificationType type, NotificationChannel channel, boolean enabled) {

        NotificationPreference preference = preferenceRepository
                .findByUserIdAndNotificationTypeAndChannel(userId, type, channel)
                .orElse(NotificationPreference.builder()
                        .userId(userId)
                        .notificationType(type)
                        .channel(channel)
                        .build());

        preference.setEnabled(enabled);
        return preferenceRepository.save(preference);
    }
    @Transactional
    public void initializeDefaultPreferences(Long userId) {
        // Create default preferences for new users
        for (NotificationType type : NotificationType.values()) {
            // Enable IN_APP and EMAIL by default
            createIfNotExists(userId, type, NotificationChannel.IN_APP, true);
            createIfNotExists(userId, type, NotificationChannel.EMAIL, true);
        }
    }

    private void createIfNotExists(Long userId, NotificationType type, NotificationChannel channel, boolean enabled) {
        if (preferenceRepository.findByUserIdAndNotificationTypeAndChannel(userId, type, channel).isEmpty()) {
            NotificationPreference pref = NotificationPreference.builder()
                    .userId(userId)
                    .notificationType(type)
                    .channel(channel)
                    .enabled(enabled)
                    .build();
            preferenceRepository.save(pref);
        }
    }
}
