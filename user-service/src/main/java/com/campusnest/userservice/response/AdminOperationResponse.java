package com.campusnest.userservice.response;

import com.campusnest.userservice.response.UserResponse;
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
public class AdminOperationResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("user")
    private UserResponse user;
    
    @JsonProperty("updated_by")
    private String updatedBy;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    // Static factory methods for common responses
    
    public static AdminOperationResponse success(String message, UserResponse user, String updatedBy) {
        return AdminOperationResponse.builder()
                .success(true)
                .message(message)
                .user(user)
                .updatedBy(updatedBy)
                .build();
    }
    
    public static AdminOperationResponse failure(String message) {
        return AdminOperationResponse.builder()
                .success(false)
                .message(message)
                .build();
    }
}