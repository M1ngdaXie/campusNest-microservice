package com.campusnest.messagingservice.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class WebSocketAuthenticationHandler implements ChannelInterceptor {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticateUser(accessor);
        }

        return message;
    }

    private void authenticateUser(StompHeaderAccessor accessor) {
        try {
            // Try to get Authorization header
            List<String> authHeaders = accessor.getNativeHeader("Authorization");
            if (authHeaders == null || authHeaders.isEmpty()) {
                rejectConnection("No authorization token provided. Please include 'Authorization: Bearer <token>' header.");
                return;
            }

            String authHeader = authHeaders.get(0);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                rejectConnection("Invalid authorization format. Expected 'Bearer <token>'");
                return;
            }

            String token = authHeader.substring(7);
            if (token == null || token.trim().isEmpty()) {
                rejectConnection("Empty authorization token. Please provide a valid JWT token.");
                return;
            }

            // Validate token format before processing
            if (token.length() < 20 || !token.contains(".")) {
                rejectConnection("Invalid token format. JWT tokens should have 3 parts separated by dots.");
                return;
            }

            String email;
            Long userId;
            try {
                email = jwtTokenService.extractEmail(token);
                userId = jwtTokenService.extractUserId(token);
            } catch (Exception tokenError) {
                String errorMsg = getTokenErrorMessage(tokenError);
                rejectConnection("Token validation failed: " + errorMsg);
                return;
            }

            if (email == null) {
                rejectConnection("Token does not contain a valid email address.");
                return;
            }

            if (userId == null) {
                rejectConnection("Token does not contain a valid user ID.");
                return;
            }

            if (!jwtTokenService.isTokenValid(token, email)) {
                rejectConnection("Token has expired or is invalid. Please login again to get a new token.");
                return;
            }

            // Create a simple authentication with user ID as principal
            // In microservices, we don't have the full User entity, just the ID
            UserPrincipal userPrincipal = new UserPrincipal(userId, email);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    userPrincipal, null, new ArrayList<>()
            );
            accessor.setUser(authentication);
            log.info("✅ WebSocket authenticated user: {} (ID: {})",
                    maskEmail(email), userId);

        } catch (Exception e) {
            log.error("❌ Unexpected WebSocket authentication error: {}", e.getMessage());
            rejectConnection("Authentication system error. Please try again or contact support.");
        }
    }

    private void rejectConnection(String userFriendlyMessage) {
        log.warn("🚫 WebSocket connection REJECTED: {}", userFriendlyMessage);
        throw new IllegalArgumentException("WebSocket Authentication Failed: " + userFriendlyMessage);
    }

    private String getTokenErrorMessage(Exception tokenError) {
        String errorMsg = tokenError.getMessage();

        if (errorMsg.contains("JSON format")) {
            return "Token appears to be corrupted or truncated.";
        } else if (errorMsg.contains("expired")) {
            return "Token has expired. Please login again.";
        } else if (errorMsg.contains("signature")) {
            return "Token signature is invalid.";
        } else if (errorMsg.contains("malformed")) {
            return "Token format is malformed.";
        } else {
            return "Token is invalid or corrupted.";
        }
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }

    // Simple UserPrincipal class to hold user ID and email
    public static class UserPrincipal implements Principal {
        private final Long userId;
        private final String email;

        public UserPrincipal(Long userId, String email) {
            this.userId = userId;
            this.email = email;
        }

        public Long getUserId() {
            return userId;
        }
        @Override
        public String getName() {
            return userId.toString();  // CRITICAL: Spring uses this for routing
        }
        public String getEmail() {
            return email;
        }
    }
}