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
public class ResetPasswordResponse {
    
    @JsonProperty("success")
    private Boolean success;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("email")
    private String email; // Masked email for confirmation
    
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();
    
    @JsonProperty("auto_login")
    private Boolean autoLogin; // Whether user is automatically logged in
    
    @JsonProperty("redirect_url")
    private String redirectUrl; // Where to redirect after successful reset
    
    // Static factory methods
    
    public static ResetPasswordResponse success(String maskedEmail) {
        return ResetPasswordResponse.builder()
                .success(true)
                .message("Password reset successfully. You can now log in with your new password.")
                .email(maskedEmail)
                .autoLogin(false)
                .redirectUrl("/login")
                .build();
    }
    
    public static ResetPasswordResponse successWithAutoLogin(String maskedEmail) {
        return ResetPasswordResponse.builder()
                .success(true)
                .message("Password reset successfully. You have been logged in automatically.")
                .email(maskedEmail)
                .autoLogin(true)
                .redirectUrl("/dashboard")
                .build();
    }
    
    public static ResetPasswordResponse invalidToken() {
        return ResetPasswordResponse.builder()
                .success(false)
                .message("Invalid or expired password reset token. Please request a new reset link.")
                .build();
    }
    
    public static ResetPasswordResponse tokenExpired() {
        return ResetPasswordResponse.builder()
                .success(false)
                .message("Password reset token has expired. Please request a new reset link.")
                .build();
    }
    
    public static ResetPasswordResponse passwordMismatch() {
        return ResetPasswordResponse.builder()
                .success(false)
                .message("New password and confirmation password do not match.")
                .build();
    }
    
    public static ResetPasswordResponse samePassword() {
        return ResetPasswordResponse.builder()
                .success(false)
                .message("New password cannot be the same as your current password.")
                .build();
    }
    
    public static ResetPasswordResponse weakPassword() {
        return ResetPasswordResponse.builder()
                .success(false)
                .message("Password does not meet security requirements. Please choose a stronger password.")
                .build();
    }
}