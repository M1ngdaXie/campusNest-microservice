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
public class ForgotPasswordResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("email")
    private String email; // Masked email for confirmation
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @JsonProperty("expires_in")
    private Long expiresIn; // Token expiration time in seconds
    
    // Static factory methods
    
    public static ForgotPasswordResponse success(String maskedEmail, Long expiresIn) {
        return ForgotPasswordResponse.builder()
                .success(true)
                .message("Password reset email sent successfully. Please check your inbox.")
                .email(maskedEmail)
                .expiresIn(expiresIn)
                .build();
    }
    
    public static ForgotPasswordResponse userNotFound() {
        return ForgotPasswordResponse.builder()
                .success(true) // Don't reveal if email exists for security
                .message("Looks like you don't have an account with us. Please sign up first.")
                .build();
    }
    
    public static ForgotPasswordResponse emailNotVerified() {
        return ForgotPasswordResponse.builder()
                .success(false)
                .message("Please verify your email address before requesting a password reset.")
                .build();
    }
    
    public static ForgotPasswordResponse rateLimited() {
        return ForgotPasswordResponse.builder()
                .success(false)
                .message("Request rate limited. Too many password reset requests. Please wait before trying again.")
                .build();
    }
    
    public static ForgotPasswordResponse emailSendFailure() {
        return ForgotPasswordResponse.builder()
                .success(false)
                .message("Failed to send password reset email. Please try again later.")
                .build();
    }
}