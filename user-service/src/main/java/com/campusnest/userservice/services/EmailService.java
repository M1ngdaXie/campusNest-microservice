package com.campusnest.userservice.services;


public interface EmailService {
    void sendVerificationEmail(String email, String verificationUrl);
}
