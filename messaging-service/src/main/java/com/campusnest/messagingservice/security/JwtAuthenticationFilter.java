package com.campusnest.messagingservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // First, check if headers from API Gateway are present (preferred)
            String userIdHeader = request.getHeader("X-User-Id");
            String emailHeader = request.getHeader("X-User-Email");

            if (userIdHeader != null && emailHeader != null) {
                // Use headers from API Gateway (already validated)
                Long userId = Long.parseLong(userIdHeader);
                String email = emailHeader;

                if (SecurityContextHolder.getContext().getAuthentication() == null) {
                    WebSocketAuthenticationHandler.UserPrincipal userPrincipal =
                            new WebSocketAuthenticationHandler.UserPrincipal(userId, email);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userPrincipal, null, new ArrayList<>());

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    log.debug("Authenticated user from gateway headers: {} (ID: {})", email, userId);
                }
            } else {
                // Fallback: parse JWT token directly (for direct calls or WebSocket)
                String authHeader = request.getHeader("Authorization");

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    String email = jwtTokenService.extractEmail(token);
                    Long userId = jwtTokenService.extractUserId(token);

                    if (email != null && userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        if (jwtTokenService.isTokenValid(token, email)) {
                            WebSocketAuthenticationHandler.UserPrincipal userPrincipal =
                                    new WebSocketAuthenticationHandler.UserPrincipal(userId, email);

                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(userPrincipal, null, new ArrayList<>());

                            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(authentication);

                            log.debug("Authenticated user from JWT: {} (ID: {})", email, userId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Cannot set user authentication: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}