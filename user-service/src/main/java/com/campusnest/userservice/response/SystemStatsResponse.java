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
public class SystemStatsResponse {
    
    @JsonProperty("success")
    @Builder.Default
    private Boolean success = true;
    
    @JsonProperty("stats")
    private SystemStats stats;
    
    @JsonProperty("requested_by")
    private String requestedBy;
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemStats {
        
        @JsonProperty("total_users")
        private Long totalUsers;
        
        @JsonProperty("active_users")
        private Long activeUsers;
        
        @JsonProperty("verified_users")
        private Long verifiedUsers;
        
        @JsonProperty("inactive_users")
        private Long inactiveUsers;
        
        @JsonProperty("unverified_users")
        private Long unverifiedUsers;
    }
    
    // Static factory method
    public static SystemStatsResponse create(
            Long totalUsers, 
            Long activeUsers, 
            Long verifiedUsers, 
            String requestedBy) {
        
        SystemStats stats = SystemStats.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .verifiedUsers(verifiedUsers)
                .inactiveUsers(totalUsers - activeUsers)
                .unverifiedUsers(totalUsers - verifiedUsers)
                .build();
                
        return SystemStatsResponse.builder()
                .stats(stats)
                .requestedBy(requestedBy)
                .build();
    }
}