package com.campusnest.messagingservice.controllers.websocket;

import com.campusnest.messagingservice.dto.ChatMessageRequest;
import com.campusnest.messagingservice.dto.ChatMessageResponse;
import com.campusnest.messagingservice.dto.TypingIndicatorRequest;
import com.campusnest.messagingservice.dto.TypingIndicatorResponse;
import com.campusnest.common.events.MessageReceivedEvent;
import com.campusnest.messagingservice.models.Conversation;
import com.campusnest.messagingservice.models.Message;
import com.campusnest.messagingservice.security.WebSocketAuthenticationHandler;
import com.campusnest.messagingservice.services.MessagingService;
import com.campusnest.messagingservice.services.UserPresenceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import static org.apache.commons.lang.time.DateUtils.truncate;

@Controller
@Slf4j
public class WebSocketMessagingController {

    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private MessagingService messagingService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private UserPresenceService presenceService;

    @Autowired
    private com.campusnest.messagingservice.clients.UserServiceClient userServiceClient;

    /**
     * Helper method to construct user-specific queue destination for RabbitMQ STOMP.
     * Format: /queue/user.{userId}.{channel}
     */
    private String getUserQueue(Long userId, String channel) {
        if (userId == null || channel == null) {
            throw new IllegalArgumentException("UserId and Channel must not be null");
        }
        return "/queue/user." + userId + "." + channel;
    }
    @MessageMapping("/chat/send")
    public void sendMessage(@jakarta.validation.Valid ChatMessageRequest request, Principal principal) {
        try {
            WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
            Long currentUserId = userPrincipal.getUserId();
            String currentUserEmail = userPrincipal.getEmail();

            log.info("Received WebSocket message from user {} (ID: {}) for conversation {}",
                    maskEmail(currentUserEmail), currentUserId, request.getConversationId());

            // Validate user can access conversation
            if (!messagingService.canUserAccessConversation(request.getConversationId(), currentUserId)) {
                log.warn("User {} attempted to send message to unauthorized conversation {}",
                        currentUserId, request.getConversationId());
                return;
            }

            // Send message through service
            Message message = messagingService.sendMessage(
                    request.getConversationId(),
                    currentUserId,
                    request.getContent(),
                    request.getMessageType() != null ? request.getMessageType() :
                            com.campusnest.messagingservice.enums.MessageType.TEXT
            );

            // Clear user caches
            clearUserCaches(currentUserId);

            // Create response
            ChatMessageResponse response = ChatMessageResponse.fromMessage(message);

            // Get conversation details
            Conversation conversation = messagingService.getConversation(request.getConversationId(), currentUserId);

            // Get other participant ID
            Long otherParticipantId = conversation.getOtherParticipantId(currentUserId);

            if (otherParticipantId != null) {
                // Send to the other participant
                String destination = getUserQueue(otherParticipantId, "messages." + request.getConversationId());
                log.info("Sending WebSocket message to user: {}, destination: {}", otherParticipantId, destination);

                messagingTemplate.convertAndSendToUser(
                        otherParticipantId.toString(),
                        destination,
                        response
                );

                // Also send to a general message queue
                String generalQueue = getUserQueue(otherParticipantId, "messages");
                messagingTemplate.convertAndSend(generalQueue, response);

                log.info("Message sent to user {} in conversation {}",
                        otherParticipantId, request.getConversationId());
            }

            // Send confirmation back to sender
            // Send confirmation back to sender - conversation-specific
            String senderConvQueue = getUserQueue(currentUserId, "message-sent." + request.getConversationId());
            messagingTemplate.convertAndSend(senderConvQueue, response);

            // Also send to general message-sent queue
            String senderGeneralQueue = getUserQueue(currentUserId, "message-sent");
            messagingTemplate.convertAndSend(senderGeneralQueue, response);

            //Publish RabbitMQ event for notifications
            if (otherParticipantId != null) {
                log.info("Fetching recipient user info for notification event");
                try {
                    // Fetch recipient user info to get email
                    com.campusnest.messagingservice.dto.UserDTO recipientUser =
                        userServiceClient.getUserById(otherParticipantId);

                    log.info("Publishing message.received event to RabbitMQ");
                    MessageReceivedEvent event = MessageReceivedEvent.builder()
                            .recipientUserId(otherParticipantId)
                            .recipientEmail(recipientUser.getEmail())
                            .senderUserId(currentUserId)
                            .senderName(currentUserEmail)
                            .conversationId(request.getConversationId())
                            .messagePreview(StringUtils.left(request.getContent(), 100)+"...")
                            .build();

                    rabbitTemplate.convertAndSend(
                            "campusnest.notifications",
                            "message.received",
                            event
                    );
                    log.info("Message notification event sent successfully");
                } catch (Exception e) {
                    log.error("Failed to send notification event: {}", e.getMessage(), e);
                    // Don't fail the main flow if notification fails
                }
            }

        } catch (Exception e) {
            log.error("Error sending WebSocket message: {}", e.getMessage());

            // Send error back to sender
            if (principal != null) {
                WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
                String errorQueue = getUserQueue(userPrincipal.getUserId(), "errors");
                messagingTemplate.convertAndSend(
                        errorQueue,
                        "Failed to send message: " + e.getMessage()
                );
            }
        }
    }

    @MessageMapping("/chat/typing")
    public void handleTypingIndicator(TypingIndicatorRequest request, Principal principal) {
        try {
            WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
            Long currentUserId = userPrincipal.getUserId();

            log.debug("Received typing indicator from user {} for conversation {}: {}",
                    currentUserId, request.getConversationId(), request.getIsTyping());

            // Validate user can access conversation
            if (!messagingService.canUserAccessConversation(request.getConversationId(), currentUserId)) {
                log.warn("User {} attempted to send typing indicator to unauthorized conversation {}",
                        currentUserId, request.getConversationId());
                return;
            }

            // Update Redis typing indicator
            if (Boolean.TRUE.equals(request.getIsTyping())) {
                presenceService.markUserTyping(request.getConversationId(), currentUserId);
            } else {
                presenceService.removeUserTyping(request.getConversationId(), currentUserId);
            }

            // Get conversation details
            Conversation conversation = messagingService.getConversation(request.getConversationId(), currentUserId);

            // Get other participant ID
            Long otherParticipantId = conversation.getOtherParticipantId(currentUserId);

            if (otherParticipantId != null) {
                // Create typing indicator response
                TypingIndicatorResponse response = TypingIndicatorResponse.create(
                        request.getConversationId(), currentUserId, request.getIsTyping());

                // Send typing indicator to the other participant
                String typingQueue = getUserQueue(otherParticipantId, "typing." + request.getConversationId());
                messagingTemplate.convertAndSend(typingQueue, response);
                log.debug("Typing indicator sent to user {} in conversation {}",
                        otherParticipantId, request.getConversationId());
            }

        } catch (Exception e) {
            log.error("Error handling typing indicator: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat/join")
    public void joinConversation(String conversationIdStr, Principal principal) {
        try {
            WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
            Long currentUserId = userPrincipal.getUserId();
            Long conversationId = Long.parseLong(conversationIdStr);

            log.info("User {} joining conversation {}", currentUserId, conversationId);

            // Validate user can access conversation
            if (!messagingService.canUserAccessConversation(conversationId, currentUserId)) {
                log.warn("User {} attempted to join unauthorized conversation {}", currentUserId, conversationId);
                return;
            }

            // Mark messages as read when user joins conversation
            messagingService.markMessagesAsRead(conversationId, currentUserId);

            // Send confirmation
            String joinQueue = getUserQueue(currentUserId, "conversation-joined." + conversationId);
            messagingTemplate.convertAndSend(joinQueue, "Successfully joined conversation");

            log.info("User {} successfully joined conversation {}", currentUserId, conversationId);

        } catch (Exception e) {
            log.error("Error joining conversation: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat/leave")
    public void leaveConversation(String conversationIdStr, Principal principal) {
        try {
            WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
            Long currentUserId = userPrincipal.getUserId();
            Long conversationId = Long.parseLong(conversationIdStr);

            log.info("User {} leaving conversation {}", currentUserId, conversationId);

            // Send confirmation
            String leaveQueue = getUserQueue(currentUserId, "conversation-left." + conversationId);
            messagingTemplate.convertAndSend(leaveQueue, "Left conversation");

        } catch (Exception e) {
            log.error("Error leaving conversation: {}", e.getMessage());
        }
    }

    @MessageMapping("/chat/status")
    public void getConnectionStatus(Principal principal) {
        WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
        log.debug("Connection status requested by user: {}", userPrincipal.getUserId());

        String statusQueue = getUserQueue(userPrincipal.getUserId(), "status");
        messagingTemplate.convertAndSend(statusQueue, "Connected as user " + userPrincipal.getUserId());
    }

    @MessageMapping("/chat/connect")
    public void handleConnect(Principal principal) {
        WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
        presenceService.markUserOnline(userPrincipal.getUserId());
        log.info("User {} connected - now ONLINE", userPrincipal.getUserId());

        // Broadcast online status to all conversation participants
        List<Conversation> userConversations = messagingService.getUserConversations(userPrincipal.getUserId());
        for (Conversation conv : userConversations) {
            Long otherUserId = conv.getOtherParticipantId(userPrincipal.getUserId());
            if (otherUserId != null) {
                String presenceQueue = getUserQueue(otherUserId, "presence");
                messagingTemplate.convertAndSend(
                        presenceQueue,
                        Map.of("userId", userPrincipal.getUserId(), "isOnline", true)
                );
            }
        }
    }

    @MessageMapping("/chat/disconnect")
    public void handleDisconnect(Principal principal) {
        WebSocketAuthenticationHandler.UserPrincipal userPrincipal = getCurrentUserPrincipal(principal);
        presenceService.markUserOffline(userPrincipal.getUserId());
        log.info("User {} disconnected - now OFFLINE", userPrincipal.getUserId());

        // Broadcast offline status to all conversation participants
        List<Conversation> userConversations = messagingService.getUserConversations(userPrincipal.getUserId());
        for (Conversation conv : userConversations) {
            Long otherUserId = conv.getOtherParticipantId(userPrincipal.getUserId());
            if (otherUserId != null) {
                String presenceQueue = getUserQueue(otherUserId, "presence");
                messagingTemplate.convertAndSend(
                        presenceQueue,
                        Map.of("userId", userPrincipal.getUserId(), "isOnline", false)
                );
            }
        }
    }

    private WebSocketAuthenticationHandler.UserPrincipal getCurrentUserPrincipal(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken) {
            var auth = (UsernamePasswordAuthenticationToken) principal;
            if (auth.getPrincipal() instanceof WebSocketAuthenticationHandler.UserPrincipal) {
                return (WebSocketAuthenticationHandler.UserPrincipal) auth.getPrincipal();
            }
        }
        throw new RuntimeException("User not found in WebSocket authentication context");
    }

    private void clearUserCaches(Long userId) {
        // Clear specific user caches that might be affected by new messages
        if (cacheManager.getCache("unread-counts") != null) {
            cacheManager.getCache("unread-counts").evict(userId);
        }
        if (cacheManager.getCache("conversations") != null) {
            cacheManager.getCache("conversations").clear();
        }
        log.debug("Cleared caches for user: {}", userId);
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }
}