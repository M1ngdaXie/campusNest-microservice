package com.campusnest.messagingservice.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserPresenceService {

    private static final String ONLINE_USERS_KEY = "online_users";
    private static final String USER_TYPING_PREFIX = "typing:conversation:";
    private static final long PRESENCE_TTL_SECONDS = 300; // 5 minutes
    private static final long TYPING_TTL_SECONDS = 10; // 10 seconds

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * Mark user as online
     */
    public void markUserOnline(Long userId) {
        log.debug("Marking user {} as online", userId);
        redisTemplate.opsForSet().add(ONLINE_USERS_KEY, userId.toString());
        redisTemplate.expire(ONLINE_USERS_KEY + ":" + userId, PRESENCE_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Mark user as offline
     */
    public void markUserOffline(Long userId) {
        log.debug("Marking user {} as offline", userId);
        redisTemplate.opsForSet().remove(ONLINE_USERS_KEY, userId.toString());
        redisTemplate.delete(ONLINE_USERS_KEY + ":" + userId);
    }

    /**
     * Check if user is online
     */
    public boolean isUserOnline(Long userId) {
        Boolean isMember = redisTemplate.opsForSet().isMember(ONLINE_USERS_KEY, userId.toString());
        return isMember != null && isMember;
    }

    /**
     * Get all online users
     */
    public Set<Long> getOnlineUsers() {
        Set<Object> members = redisTemplate.opsForSet().members(ONLINE_USERS_KEY);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toSet());
    }

    /**
     * Update user's last seen timestamp
     */
    public void updateLastSeen(Long userId) {
        String key = "last_seen:" + userId;
        redisTemplate.opsForValue().set(key, System.currentTimeMillis());
        redisTemplate.expire(key, 7, TimeUnit.DAYS);
    }

    /**
     * Get user's last seen timestamp
     */
    public Long getLastSeen(Long userId) {
        String key = "last_seen:" + userId;
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value.toString()) : null;
    }

    /**
     * Mark user as typing in a conversation
     */
    public void markUserTyping(Long conversationId, Long userId) {
        String key = USER_TYPING_PREFIX + conversationId;
        log.debug("User {} is typing in conversation {}", userId, conversationId);
        redisTemplate.opsForSet().add(key, userId.toString());
        redisTemplate.expire(key, TYPING_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Remove user typing indicator
     */
    public void removeUserTyping(Long conversationId, Long userId) {
        String key = USER_TYPING_PREFIX + conversationId;
        log.debug("User {} stopped typing in conversation {}", userId, conversationId);
        redisTemplate.opsForSet().remove(key, userId.toString());
    }

    /**
     * Get users typing in a conversation
     */
    public Set<Long> getUsersTyping(Long conversationId) {
        String key = USER_TYPING_PREFIX + conversationId;
        Set<Object> members = redisTemplate.opsForSet().members(key);
        if (members == null) {
            return Set.of();
        }
        return members.stream()
                .map(obj -> Long.parseLong(obj.toString()))
                .collect(Collectors.toSet());
    }

    /**
     * Refresh user's online status (heartbeat)
     */
    public void heartbeat(Long userId) {
        markUserOnline(userId);
        updateLastSeen(userId);
    }
}