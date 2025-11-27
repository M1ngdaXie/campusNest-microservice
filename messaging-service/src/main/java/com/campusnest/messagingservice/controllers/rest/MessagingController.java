package com.campusnest.messagingservice.controllers.rest;

import com.campusnest.messagingservice.clients.HousingServiceClient;
import com.campusnest.messagingservice.clients.UserServiceClient;
import com.campusnest.messagingservice.dto.ConversationDTO;
import com.campusnest.messagingservice.dto.MessageDTO;
import com.campusnest.messagingservice.dto.UserDTO;
import com.campusnest.messagingservice.dto.HousingListingDTO;
import com.campusnest.messagingservice.models.Conversation;
import com.campusnest.messagingservice.models.Message;
import com.campusnest.messagingservice.requests.CreateConversationRequest;
import com.campusnest.messagingservice.services.MessagingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/messaging")
@Slf4j
public class MessagingController {

    @Autowired
    private MessagingService messagingService;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private HousingServiceClient housingServiceClient;

    /**
     * Create or get conversation
     */
    @PostMapping("/conversations")
    public ResponseEntity<ConversationDTO> createOrGetConversation(
            @RequestBody CreateConversationRequest request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Conversation conversation = messagingService.createOrGetConversation(
                currentUserId, request.getOtherParticipantId(), request.getHousingListingId());

        ConversationDTO dto = convertToConversationDTO(conversation, currentUserId);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get all conversations for current user
     */
    @GetMapping("/conversations")
    public ResponseEntity<Page<ConversationDTO>> getUserConversations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);

        Page<Conversation> conversations = messagingService.getUserConversations(currentUserId, pageable);
        Page<ConversationDTO> dtos = conversations.map(conv -> convertToConversationDTO(conv, currentUserId));

        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific conversation
     */
    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<ConversationDTO> getConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Conversation conversation = messagingService.getConversation(conversationId, currentUserId);
        ConversationDTO dto = convertToConversationDTO(conversation, currentUserId);

        return ResponseEntity.ok(dto);
    }

    /**
     * Get messages in a conversation
     */
    @GetMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<Page<MessageDTO>> getConversationMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        Pageable pageable = PageRequest.of(page, size);

        Page<Message> messages = messagingService.getConversationMessages(conversationId, currentUserId, pageable);
        Page<MessageDTO> dtos = messages.map(this::convertToMessageDTO);

        return ResponseEntity.ok(dtos);
    }

    /**
     * Send a message
     */
    @PostMapping("/conversations/{conversationId}/messages")
    public ResponseEntity<MessageDTO> sendMessage(
            @PathVariable Long conversationId,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        String content = request.get("content");

        Message message = messagingService.sendMessage(conversationId, currentUserId, content);
        MessageDTO dto = convertToMessageDTO(message);

        return ResponseEntity.ok(dto);
    }

    /**
     * Mark messages as read
     */
    @PostMapping("/conversations/{conversationId}/mark-read")
    public ResponseEntity<Map<String, String>> markMessagesAsRead(
            @PathVariable Long conversationId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        messagingService.markMessagesAsRead(conversationId, currentUserId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Messages marked as read");
        return ResponseEntity.ok(response);
    }

    /**
     * Get unread message count for a conversation
     */
    @GetMapping("/conversations/{conversationId}/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @PathVariable Long conversationId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        long count = messagingService.getUnreadMessageCount(conversationId, currentUserId);

        Map<String, Long> response = new HashMap<>();
        response.put("unreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Get total unread message count for current user
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getTotalUnreadCount(Authentication authentication) {
        Long currentUserId = getCurrentUserId(authentication);
        long count = messagingService.getTotalUnreadMessageCount(currentUserId);

        Map<String, Long> response = new HashMap<>();
        response.put("totalUnreadCount", count);
        return ResponseEntity.ok(response);
    }

    /**
     * Deactivate a conversation
     */
    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Map<String, String>> deactivateConversation(
            @PathVariable Long conversationId,
            Authentication authentication) {

        Long currentUserId = getCurrentUserId(authentication);
        messagingService.deactivateConversation(conversationId, currentUserId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Conversation deactivated");
        return ResponseEntity.ok(response);
    }

    // Helper methods

    private Long getCurrentUserId(Authentication authentication) {
        // Extract user ID from UserPrincipal
        if (authentication != null && authentication.getPrincipal() instanceof com.campusnest.messagingservice.security.WebSocketAuthenticationHandler.UserPrincipal) {
            com.campusnest.messagingservice.security.WebSocketAuthenticationHandler.UserPrincipal userPrincipal =
                    (com.campusnest.messagingservice.security.WebSocketAuthenticationHandler.UserPrincipal) authentication.getPrincipal();
            return userPrincipal.getUserId();
        }
        throw new IllegalStateException("Unable to extract user ID from authentication");
    }

    private ConversationDTO convertToConversationDTO(Conversation conversation, Long currentUserId) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setParticipant1Id(conversation.getParticipant1Id());
        dto.setParticipant2Id(conversation.getParticipant2Id());
        dto.setHousingListingId(conversation.getHousingListingId());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setLastMessageAt(conversation.getLastMessageAt());
        dto.setIsActive(conversation.getIsActive());

        // Fetch user details via Feign
        try {
            dto.setParticipant1(userServiceClient.getUserById(conversation.getParticipant1Id()));
            dto.setParticipant2(userServiceClient.getUserById(conversation.getParticipant2Id()));
        } catch (Exception e) {
            log.warn("Failed to fetch user details: {}", e.getMessage());
        }

        // Fetch housing listing details via Feign
        try {
            dto.setHousingListing(housingServiceClient.getHousingListingById(conversation.getHousingListingId()));
        } catch (Exception e) {
            log.warn("Failed to fetch housing listing details: {}", e.getMessage());
        }

        // Get unread count
        dto.setUnreadCount(messagingService.getUnreadMessageCount(conversation.getId(), currentUserId));

        // Get last message
        Message lastMessage = messagingService.getLatestMessage(conversation.getId());
        if (lastMessage != null) {
            dto.setLastMessage(convertToMessageDTO(lastMessage));
        }

        return dto;
    }

    private MessageDTO convertToMessageDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversation().getId());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setSentAt(message.getSentAt());
        dto.setIsEdited(message.getIsEdited());
        dto.setEditedAt(message.getEditedAt());

        // Fetch sender details via Feign
        try {
            dto.setSender(userServiceClient.getUserById(message.getSenderId()));
        } catch (Exception e) {
            log.warn("Failed to fetch sender details: {}", e.getMessage());
        }

        return dto;
    }
}