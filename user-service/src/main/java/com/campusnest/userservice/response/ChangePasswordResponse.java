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
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePasswordResponse {

    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("email")
    private String email; // Masked email for confirmation

    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("tokens_invalidated")
    private Integer tokensInvalidated; // Number of refresh tokens invalidated

    // Static factory methods

    public static ChangePasswordResponse success(String maskedEmail, int tokensInvalidated) {
        return ChangePasswordResponse.builder()
                .success(true)
                .message("Password changed successfully. Please log in with your new password.")
                .email(maskedEmail)
                .tokensInvalidated(tokensInvalidated)
                .build();
    }

    public static ChangePasswordResponse invalidCurrentPassword() {
        return ChangePasswordResponse.builder()
                .success(false)
                .message("Current password is incorrect. Please try again.")
                .build();
    }

    public static ChangePasswordResponse passwordMismatch() {
        return ChangePasswordResponse.builder()
                .success(false)
                .message("New password and confirmation password do not match.")
                .build();
    }

    public static ChangePasswordResponse samePassword() {
        return ChangePasswordResponse.builder()
                .success(false)
                .message("New password cannot be the same as your current password.")
                .build();
    }

    public static ChangePasswordResponse weakPassword() {
        return ChangePasswordResponse.builder()
                .success(false)
                .message("Password does not meet security requirements. Please choose a stronger password.")
                .build();
    }

    public static ChangePasswordResponse error() {
        return ChangePasswordResponse.builder()
                .success(false)
                .message("An error occurred while changing your password. Please try again later.")
                .build();
    }
}
