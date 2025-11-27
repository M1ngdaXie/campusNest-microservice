package com.campusnest.messagingservice.exceptions;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException(String message) {
        super(message);
    }

    public MessageNotFoundException(Long messageId) {
        super("Message not found with ID: " + messageId);
    }
}