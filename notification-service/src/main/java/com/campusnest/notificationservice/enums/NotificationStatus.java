package com.campusnest.notificationservice.enums;

public enum NotificationStatus {
      PENDING,   // Waiting to be sent
      SENT,      // Successfully sent
      READ,      // User has read the notification
      FAILED,    // Failed to send
      ARCHIVED   // User archived it
  }