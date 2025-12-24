package com.campusnest.notificationservice.enums;

public enum NotificationType {
    // User-related notifications
    USER_REGISTERED("Welcome to CampusNest!"),
    EMAIL_VERIFIED("Email Verified Successfully"),
    PASSWORD_CHANGED("Password Changed"),

    // Messaging notifications
    MESSAGE_RECEIVED("New Message"),
    CONVERSATION_STARTED("New Conversation"),

    // Housing notifications
    LISTING_CREATED("Your Listing is Live"),
    LISTING_UPDATED("Listing Updated"),
    LISTING_INQUIRY("New Inquiry on Your Listing"),
    NEW_LISTING_IN_AREA("New Listing in Your Area"),
    PRICE_DROP_ALERT("Price Drop Alert"),

    // System notifications
    SYSTEM_ANNOUNCEMENT("System Announcement"),
    MAINTENANCE_SCHEDULED("Scheduled Maintenance");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
