package com.campusnest.userservice.response;

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
public class AdminUserDetailResponse {
    
    @JsonProperty("success")
    @Builder.Default
    private Boolean success = true;
    
    @JsonProperty("user")
    private UserResponse user;
    
    @JsonProperty("requested_by")
    private String requestedBy;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    // Static factory method
    public static AdminUserDetailResponse create(UserResponse user, String requestedBy) {
        return AdminUserDetailResponse.builder()
                .user(user)
                .requestedBy(requestedBy)
                .build();
    }
}