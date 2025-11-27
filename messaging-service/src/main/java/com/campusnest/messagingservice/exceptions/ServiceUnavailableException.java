package com.campusnest.messagingservice.exceptions;

public class ServiceUnavailableException extends RuntimeException {
    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String serviceName, Throwable cause) {
        super("Service " + serviceName + " is unavailable: " + cause.getMessage(), cause);
    }
}