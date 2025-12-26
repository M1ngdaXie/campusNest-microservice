package com.campusnest.notificationservice.config;

import com.campusnest.notificationservice.jwtutils.JwtUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel){
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())){
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")){
                log.error("WebSocket connection rejected: Missing or invalid Authorization header");
                throw new IllegalArgumentException("Missing or invalid Authorization header");
            }
            String jwt = authHeader.substring(7);
            if(!jwtUtils.validateToken(jwt)){
                log.error("WebSocket connection rejected: Invalid JWT token");
                throw new IllegalArgumentException("Invalid JWT token");
            }
            Long userId = jwtUtils.getUserIdFromToken(jwt);
            accessor.setUser(new StompPrincipal(userId.toString()));
            log.info("WebSocket connection authenticated for userId: {}", userId);
        }
        return message;
    }
}
