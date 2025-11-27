package com.campusnest.userservice.response;

import com.campusnest.userservice.enums.UserRole;
import com.campusnest.userservice.enums.VerificationStatus;
import com.campusnest.userservice.models.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    
    @JsonProperty("id")
    private String id;
    
    @JsonProperty("email")
    private String email;
    
    @JsonProperty("first_name")
    private String firstName;
    
    @JsonProperty("last_name")
    private String lastName;
    
    @JsonProperty("full_name")
    private String fullName; // firstName + lastName
    
    @JsonProperty("university_domain") 
    private String universityDomain;
    
    @JsonProperty("verification_status")
    private VerificationStatus verificationStatus;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    @JsonProperty("account_created")
    private Instant accountCreated;

    @JsonProperty("last_login")
    private Instant lastLogin; // Null if never logged in

    @JsonProperty("profile_complete")
    private Boolean profileComplete; // For prompting profile completion
    
    @JsonProperty("role")
    private UserRole role;
    
    // Factory method for registration context
    public static UserResponse from(User user) {
        return UserResponse.builder()
            .id(user.getId().toString())
            .email(maskEmail(user.getEmail())) // Mask for security: j***@stanford.edu
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .fullName(user.getFirstName() + " " + user.getLastName())
            .universityDomain(user.getUniversityDomain())
            .verificationStatus(user.getVerificationStatus())
            .emailVerified(user.getEmailVerified())
            .accountCreated(user.getCreatedAt())
            .role(user.getRole())
            .build();
    }
    public static UserResponse fromLogin(User user) {
        return UserResponse.builder()
                .id(user.getId().toString())
                .email(user.getEmail()) // Don't mask for successful login
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .universityDomain(user.getUniversityDomain())
                .verificationStatus(user.getVerificationStatus())
                .emailVerified(user.getEmailVerified())
                .accountCreated(user.getCreatedAt())
                .lastLogin(user.getLastLoginAt())
                .profileComplete(isProfileComplete(user)) // Check if needs profile completion
                .role(user.getRole())
                .build();
    }
    private static boolean isProfileComplete(User user) {
        // Define what constitutes a complete profile for your platform
        return user.getFirstName() != null &&
                user.getLastName() != null &&
                user.getEmailVerified() &&
                user.getVerificationStatus() == VerificationStatus.EMAIL_VERIFIED;
    }
    
    private static String maskEmail(String email) {
        if (email == null || email.length() < 3) return email;
        int atIndex = email.indexOf("@");
        if (atIndex < 2) return email;
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}