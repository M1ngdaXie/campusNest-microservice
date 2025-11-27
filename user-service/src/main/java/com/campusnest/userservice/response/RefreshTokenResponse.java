package com.campusnest.userservice.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RefreshTokenResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    // === NEW TOKENS ===
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token")
    private String refreshToken; // New refresh token (token rotation)
    
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private Long expiresIn; // Access token expiration in seconds
    
    // === USER INFO ===
    @JsonProperty("user")
    private UserResponse user; // Updated user info
    
    // === METADATA ===
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @JsonProperty("session_id")
    private String sessionId; // For tracking
    
    // === STATIC FACTORY METHODS ===
    
    /**
     * Successful token refresh
     */
    public static RefreshTokenResponse success(String accessToken, String refreshToken, 
                                             UserResponse user, Long expiresIn, String sessionId) {
        return RefreshTokenResponse.builder()
                .success(true)
                .message("Token refreshed successfully")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .user(user)
                .sessionId(sessionId)
                .build();
    }
    
    /**
     * Token refresh failed - invalid token
     */
    public static RefreshTokenResponse invalidToken() {
        return RefreshTokenResponse.builder()
                .success(false)
                .message("Invalid or expired refresh token. Please log in again.")
                .build();
    }
    
    /**
     * Token refresh failed - user account issues
     */
    public static RefreshTokenResponse accountIssue() {
        return RefreshTokenResponse.builder()
                .success(false)
                .message("Account not active or verification status changed. Please log in again.")
                .build();
    }
    
    /**
     * Token refresh failed - too many attempts
     */
    public static RefreshTokenResponse rateLimited() {
        return RefreshTokenResponse.builder()
                .success(false)
                .message("Too many refresh attempts. Please wait before trying again.")
                .build();
    }
}