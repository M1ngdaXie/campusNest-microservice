package com.campusnest.userservice.response;

import com.campusnest.userservice.models.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    
    // === SUCCESS INDICATOR ===
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    // === AUTHENTICATION TOKENS ===
    @JsonProperty("access_token")
    private String accessToken;
    
    @JsonProperty("refresh_token") 
    private String refreshToken;
    
    @JsonProperty("token_type")
    @Builder.Default
    private String tokenType = "Bearer";
    
    @JsonProperty("expires_in")
    private Long expiresIn; // Seconds until access token expires
    
    // === USER INFORMATION ===
    @JsonProperty("user")
    private UserResponse user;
    
    // === SESSION INFO ===
    @JsonProperty("session_id")
    private String sessionId; // For tracking/logout
    
    @JsonProperty("login_timestamp")
    @Builder.Default
    private Instant loginTimestamp = Instant.now();
    
    @JsonProperty("first_login")
    private Boolean firstLogin; // For onboarding flow
    
    // === STATIC FACTORY METHODS ===
    
    /**
     * Successful login response
     */
    public static LoginResponse success(String accessToken, String refreshToken,
                                        User user, Long expiresIn, String sessionId) {
        return LoginResponse.builder()
            .success(true)
            .message("Login successful")
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .expiresIn(expiresIn)
            .user(UserResponse.fromLogin(user)) // Different factory method for login context
            .sessionId(sessionId)
            .firstLogin(user.getLastLoginAt() == null)
            .build();
    }
    
    /**
     * Login failed - email not verified
     */
    public static LoginResponse emailNotVerified(String email) {
        return LoginResponse.builder()
            .success(false)
            .message("Please verify your university email before logging in. Check your inbox for verification link.")
            .build();
    }
    
    /**
     * Login failed - invalid credentials
     */
    public static LoginResponse invalidCredentials() {
        return LoginResponse.builder()
            .success(false)
            .message("Invalid email or password. Please try again.")
            .build();
    }
    
    /**
     * Login failed - account suspended
     */
    public static LoginResponse accountSuspended() {
        return LoginResponse.builder()
            .success(false)
            .message("Your account has been suspended. Please contact support for assistance.")
            .build();
    }
    
    /**
     * Login failed - account locked
     */
    public static LoginResponse accountLocked(Instant unlockTime) {
        long minutesUntilUnlock = Duration.between(Instant.now(), unlockTime).toMinutes();
        return LoginResponse.builder()
            .success(false)
            .message(String.format("Account temporarily locked due to multiple failed login attempts. Try again in %d minutes.", minutesUntilUnlock))
            .build();
    }
}