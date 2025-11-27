package com.campusnest.userservice.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 255)
    private String token;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private Instant expiryDate;
    
    @Column(nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    
    @Column
    private Instant usedAt; // When the token was used (null if not used)
    
    @Column(length = 45)
    private String ipAddress; // IP address of the requester for security
    
    @Column(length = 500)
    private String userAgent; // User agent for security tracking
    
    public boolean isExpired() {
        return Instant.now().isAfter(expiryDate);
    }
    
    public boolean isUsed() {
        return usedAt != null;
    }
    
    public void markAsUsed() {
        this.usedAt = Instant.now();
    }
}