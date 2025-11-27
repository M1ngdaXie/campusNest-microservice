package com.campusnest.userservice.response;

import com.campusnest.userservice.models.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Don't include null fields in JSON
public class RegisterResponse {

    // === CORE RESPONSE DATA ===
    @JsonProperty("success")
    private Boolean success;

    @JsonProperty("message")
    private String message;

    // === USER INFORMATION ===
    @JsonProperty("user")
    private UserResponse user; // Only basic, safe user info

    // === NEXT STEPS GUIDANCE ===
    @JsonProperty("next_step")
    private String nextStep; // "EMAIL_VERIFICATION", "COMPLETE_PROFILE", etc.

    @JsonProperty("verification_email_sent")
    private Boolean verificationEmailSent;

    // === METADATA ===
    @JsonProperty("timestamp")
    @Builder.Default
    private Instant timestamp = Instant.now();

    @JsonProperty("university_info")
    private UniversityInfo universityInfo; // University details

    // === STATIC FACTORY METHODS FOR COMMON SCENARIOS ===

    /**
     * Successful registration - email verification sent
     */
    public static RegisterResponse success(User user, String universityName) {
        return RegisterResponse.builder()
                .success(true)
                .message("Registration successful! Please check your university email to verify your account.")
                .user(UserResponse.from(user))
                .nextStep("EMAIL_VERIFICATION")
                .verificationEmailSent(true)
                .universityInfo(UniversityInfo.builder()
                        .name(universityName)
                        .domain(user.getUniversityDomain())
                        .build())
                .build();
    }

    /**
     * Registration successful but email service failed
     */
    public static RegisterResponse successWithEmailWarning(User user, String universityName) {
        return RegisterResponse.builder()
                .success(true)
                .message("Registration successful! However, we couldn't send the verification email. Please try the 'Resend Verification' option.")
                .user(UserResponse.from(user))
                .nextStep("RESEND_VERIFICATION")
                .verificationEmailSent(false)
                .universityInfo(UniversityInfo.builder()
                        .name(universityName)
                        .domain(user.getUniversityDomain())
                        .build())
                .build();
    }

    /**
     * Registration failed - user already exists
     */
    public static RegisterResponse userAlreadyExists(String email) {
        return RegisterResponse.builder()
                .success(false)
                .message("An account already exists with this email address. Please try logging in or use password reset if needed.")
                .nextStep("LOGIN")
                .verificationEmailSent(false)
                .build();
    }

    /**
     * Registration failed - invalid university domain
     */
    public static RegisterResponse invalidUniversity(String email, String domain) {
        return RegisterResponse.builder()
                .success(false)
                .message(String.format("We don't currently support %s. Please use your official university email address or contact support to add your university.", domain))
                .nextStep("CONTACT_SUPPORT")
                .verificationEmailSent(false)
                .build();
    }
}