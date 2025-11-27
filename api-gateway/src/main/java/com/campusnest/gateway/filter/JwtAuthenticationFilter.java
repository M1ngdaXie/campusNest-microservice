package com.campusnest.gateway.filter;

import com.campusnest.gateway.config.SecurityProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Autowired
    private SecurityProperties securityProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().name();

        log.debug("Processing request: {} {}", method, path);

        // Skip JWT validation for public endpoints
        if (isPublicEndpoint(path)) {
            log.debug("Public endpoint, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Allow any method to fully public endpoints
        if (isFullyPublicEndpoint(path)) {
            log.debug("Fully public endpoint, skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Allow GET requests to public read endpoints
        if ("GET".equals(method) && isPublicReadEndpoint(path)) {
            log.debug("Public read endpoint (GET), skipping JWT validation: {}", path);
            return chain.filter(exchange);
        }

        // Extract JWT token
        String token = extractToken(request);
        if (token == null) {
            log.warn("No JWT token found for protected endpoint: {} {}", method, path);
            return onError(exchange, "Missing authorization token", HttpStatus.UNAUTHORIZED);
        }

        // Validate JWT token
        try {
            Claims claims = validateToken(token);
//            String userId = claims.getSubject();
            log.info(
                    "JWT validated for user_id: {}, email: {}, role: {}",
                    claims.get("claims", Map.class).get("user_id"),
                    claims.get("claims", Map.class).get("email"),
                    claims.get("claims", Map.class).get("role")
            );


            // Add user info to request headers for downstream services
            Map<String, Object> nestedClaims = (Map<String, Object>) claims.get("claims");

            String email = (String) nestedClaims.get("email");
            String role = (String) nestedClaims.get("role");
            String userId = String.valueOf(nestedClaims.get("user_id"));

            ServerHttpRequest modifiedRequest = request.mutate()
                    .header("X-User-Email", email)
                    .header("X-User-Role", role)
                    .header("X-User-Id", userId)
                    .build();

            log.debug("JWT validated successfully for user: {}", userId);
            return chain.filter(exchange.mutate().request(modifiedRequest).build());

        } catch (Exception e) {
            log.error("JWT validation failed: {}", e.getMessage());
            return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublicEndpoint(String path) {
        return securityProperties.getPublicEndpoints().stream().anyMatch(path::startsWith);
    }

    private boolean isPublicReadEndpoint(String path) {
        return securityProperties.getPublicReadEndpoints().stream().anyMatch(path::startsWith);
    }

    private boolean isFullyPublicEndpoint(String path) {
        return securityProperties.getFullyPublicEndpoints().stream().anyMatch(path::startsWith);
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private Claims validateToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().add("Content-Type", "application/json");

        String errorBody = String.format("{\"error\":\"%s\",\"message\":\"%s\"}",
                status.getReasonPhrase(), message);

        return response.writeWith(Mono.just(response.bufferFactory()
                .wrap(errorBody.getBytes(StandardCharsets.UTF_8))));
    }

    @Override
    public int getOrder() {
        return -100; // Run before other filters
    }
}