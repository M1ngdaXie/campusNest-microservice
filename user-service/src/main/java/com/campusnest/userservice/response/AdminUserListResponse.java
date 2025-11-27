package com.campusnest.userservice.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListResponse {
    
    @JsonProperty("success")
    @Builder.Default
    private Boolean success = true;
    
    @JsonProperty("users")
    private List<UserResponse> users;
    
    @JsonProperty("total_count")
    private Long totalCount;
    
    @JsonProperty("requested_by")
    private String requestedBy;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    // Static factory method
    public static AdminUserListResponse create(List<UserResponse> users, String requestedBy) {
        return AdminUserListResponse.builder()
                .users(users)
                .totalCount((long) users.size())
                .requestedBy(requestedBy)
                .build();
    }
}