package com.campusnest.userservice.services.impl;

import com.campusnest.userservice.services.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Override
    public void sendVerificationEmail(String email, String verificationUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(email);
            helper.setSubject("Verify your CampusNest account");
            helper.setFrom("noreply@campusnest.com");

            String htmlContent = buildVerificationEmailTemplate(verificationUrl, email);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("Failed to send verification email to: {}", email, e);
            throw new RuntimeException("Failed to send verification email");
        }
    }

    @Override
    public void sendChangePasswordEmail(String email, String firstName, String resetUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(email);
            helper.setSubject("Change your CampusNest password");
            helper.setFrom("noreply@campusnest.com");

            String htmlContent = buildChangePasswordTemplate(firstName, resetUrl);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            log.error("Failed to send change password email to: {}", email, e);
            throw new RuntimeException("Failed to send change password email");
        }
    }

    private String buildChangePasswordTemplate(String firstName, String resetUrl) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Change Your CampusNest Password</title>
            </head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center;">
                    <h1 style="color: #333;">CampusNest Password Reset</h1>
                    <p>Hi %s,</p>
                    <p>We received a request to reset your password. Click the button below to change your password.</p>
                    <div style="margin: 30px 0;">
                        <a href="%s"
                           style="background-color: #007bff; color: white; padding: 12px 30px;
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            Change Password
                        </a>
                    </div>
                    <p style="color: #666; font-size: 14px;">
                        If the button doesn't work, copy and paste this link in your browser:<br>
                        <a href="%s">%s</a>
                    </p>
                    <p style="color: #666; font-size: 12px;">
                        This password reset link expires in 1 hour.<br>
                        If you didn't request a password reset, you can safely ignore this email.
                    </p>
                </div>
            </body>
            </html>
                """.formatted(firstName, resetUrl, resetUrl, resetUrl);
    }

    private String buildVerificationEmailTemplate(String verificationUrl, String email) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Verify Your CampusNest Account</title>
            </head>
            <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
                <div style="background-color: #f8f9fa; padding: 20px; text-align: center;">
                    <h1 style="color: #333;">Welcome to CampusNest!</h1>
                    <p>Hi there! Please verify your university email to complete your registration.</p>
                    
                    <div style="margin: 30px 0;">
                        <a href="%s" 
                           style="background-color: #007bff; color: white; padding: 12px 30px; 
                                  text-decoration: none; border-radius: 5px; display: inline-block;">
                            Verify Email Address
                        </a>
                    </div>
                    
                    <p style="color: #666; font-size: 14px;">
                        If the button doesn't work, copy and paste this link in your browser:<br>
                        <a href="%s">%s</a>
                    </p>
                    
                    <p style="color: #666; font-size: 12px;">
                        This verification link expires in 24 hours.<br>
                        If you didn't create an account, you can safely ignore this email.
                    </p>
                </div>
            </body>
            </html>
            """.formatted(verificationUrl, verificationUrl, verificationUrl);
    }
}
