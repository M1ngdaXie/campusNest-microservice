package com.campusnest.userservice.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogoutResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("tokens_invalidated")
    private Integer tokensInvalidated;
    
    @JsonProperty("logout_time")
    private String logoutTime;
    
    public static LogoutResponse success() {
        return LogoutResponse.builder()
                .success(true)
                .message("Logged out successfully")
                .tokensInvalidated(1)
                .logoutTime(java.time.Instant.now().toString())
                .build();
    }
    
    public static LogoutResponse successAllDevices(int tokensInvalidated) {
        return LogoutResponse.builder()
                .success(true)
                .message("Logged out from all devices successfully")
                .tokensInvalidated(tokensInvalidated)
                .logoutTime(java.time.Instant.now().toString())
                .build();
    }
    
    public static LogoutResponse partialSuccess(int tokensInvalidated) {
        return LogoutResponse.builder()
                .success(true)
                .message("Logout completed with some tokens already expired")
                .tokensInvalidated(tokensInvalidated)
                .logoutTime(java.time.Instant.now().toString())
                .build();
    }
    
    public static LogoutResponse alreadyLoggedOut() {
        return LogoutResponse.builder()
                .success(true)
                .message("Already logged out or token not found")
                .tokensInvalidated(0)
                .logoutTime(java.time.Instant.now().toString())
                .build();
    }
}