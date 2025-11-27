package com.campusnest.userservice.enums;

public enum VerificationStatus {
    PENDING_EMAIL,        // Just registered
    EMAIL_VERIFIED, // Email confirmed - can use platform
    SUSPENDED       // For safety (add later)
}
