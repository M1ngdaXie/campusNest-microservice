package com.campusnest.messagingservice.config;

import com.campusnest.messagingservice.security.WebSocketAuthenticationHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthenticationHandler webSocketAuthenticationHandler;

    @Value("${websocket.allowed.origins}")
    private String[] allowedOrigins;

    @Value("${spring.rabbitmq.host}")
    private String rabbitmqHost;

    @Value("${spring.rabbitmq.port}")
    private int rabbitmqPort;

    @Value("${spring.rabbitmq.username}")
    private String rabbitmqUsername;

    @Value("${spring.rabbitmq.password}")
    private String rabbitmqPassword;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // CRITICAL: Use RabbitMQ STOMP Broker Relay for horizontal scaling
        // This enables multiple messaging-service instances to share WebSocket connections
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(rabbitmqHost)
                .setRelayPort(61613)  // STOMP port
                .setClientLogin(rabbitmqUsername)
                .setClientPasscode(rabbitmqPassword)
                .setSystemLogin(rabbitmqUsername)
                .setSystemPasscode(rabbitmqPassword)
                .setSystemHeartbeatSendInterval(5000)
                .setSystemHeartbeatReceiveInterval(4000);

        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback (for web browsers)
        // Note: We allow all origins here but suppress CORS headers since API Gateway handles it
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setSuppressCors(true);  // Suppress CORS headers from SockJS

        // Register native STOMP endpoint (for iOS/mobile clients)
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthenticationHandler);
    }
}