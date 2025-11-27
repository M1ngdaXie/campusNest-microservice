package com.campusnest.messagingservice.exceptions;

public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }

    public UnauthorizedAccessException(Long userId, Long conversationId) {
        super("User " + userId + " is not authorized to access conversation " + conversationId);
    }
}