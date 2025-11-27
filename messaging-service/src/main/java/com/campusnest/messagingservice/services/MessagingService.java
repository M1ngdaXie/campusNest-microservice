package com.campusnest.messagingservice.services;

import com.campusnest.messagingservice.enums.MessageType;
import com.campusnest.messagingservice.models.Conversation;
import com.campusnest.messagingservice.models.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface MessagingService {

    /**
     * Create a new conversation or get existing one between two users for a housing listing
     */
    Conversation createOrGetConversation(Long user1Id, Long user2Id, Long listingId);

    /**
     * Send a message in a conversation
     */
    Message sendMessage(Long conversationId, Long senderId, String content, MessageType messageType);

    /**
     * Send a text message (default type)
     */
    Message sendMessage(Long conversationId, Long senderId, String content);

    /**
     * Get all messages in a conversation (with pagination)
     */
    Page<Message> getConversationMessages(Long conversationId, Long requestingUserId, Pageable pageable);

    /**
     * Get all messages in a conversation (without pagination)
     */
    List<Message> getConversationMessages(Long conversationId, Long requestingUserId);

    /**
     * Get recent messages since a specific timestamp
     */
    List<Message> getRecentMessages(Long conversationId, Long requestingUserId, LocalDateTime since);

    /**
     * Get all conversations for a user
     */
    List<Conversation> getUserConversations(Long userId);

    /**
     * Get conversations for a user with pagination
     */
    Page<Conversation> getUserConversations(Long userId, Pageable pageable);

    /**
     * Mark messages as read by a user
     */
    void markMessagesAsRead(Long conversationId, Long userId);

    /**
     * Mark specific message as read
     */
    void markMessageAsRead(Long messageId, Long userId);

    /**
     * Get unread message count for a conversation
     */
    long getUnreadMessageCount(Long conversationId, Long userId);

    /**
     * Get total unread message count for a user
     */
    long getTotalUnreadMessageCount(Long userId);

    /**
     * Get conversation by ID if user is a participant
     */
    Conversation getConversation(Long conversationId, Long userId);

    /**
     * Check if user can access a conversation
     */
    boolean canUserAccessConversation(Long conversationId, Long userId);

    /**
     * Delete/deactivate a conversation
     */
    void deactivateConversation(Long conversationId, Long userId);

    /**
     * Get latest message in a conversation
     */
    Message getLatestMessage(Long conversationId);

    /**
     * Validate conversation and associated housing listing are still valid
     */
    void validateConversationIntegrity(Long conversationId, Long userId);
}