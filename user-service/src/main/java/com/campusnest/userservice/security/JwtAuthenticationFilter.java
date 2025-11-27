package com.campusnest.userservice.security;

import com.campusnest.userservice.models.User;
import com.campusnest.userservice.repository.UserRepository;
import com.campusnest.userservice.services.JwtTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromHeader(request);
        
        if (token != null && jwtTokenService.validateAccessToken(token)) {
            try {
                String userId = jwtTokenService.getUserIdFromToken(token);
                Optional<User> userOptional = userRepository.findById(Long.parseLong(userId));
                
                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    
                    // Use UserDetails methods for account validation
                    if (user.isEnabled() && user.isAccountNonLocked() && 
                        user.isAccountNonExpired() && user.isCredentialsNonExpired()) {
                        
                        // User implements UserDetails, so we can use getAuthorities() directly
                        UsernamePasswordAuthenticationToken authentication = 
                            new UsernamePasswordAuthenticationToken(
                                user, 
                                null, 
                                user.getAuthorities()
                            );
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("Successfully authenticated user: {} with role: {}", 
                                maskEmail(user.getEmail()), user.getRole().name());
                    } else {
                        log.warn("Rejected user due to account status - user: {}, enabled: {}, unlocked: {}, non-expired: {}, credentials-valid: {}", 
                                maskEmail(user.getEmail()), user.isEnabled(), user.isAccountNonLocked(), 
                                user.isAccountNonExpired(), user.isCredentialsNonExpired());
                    }
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: {}", e.getMessage());
                SecurityContextHolder.clearContext();
            }
        }
        
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }
}