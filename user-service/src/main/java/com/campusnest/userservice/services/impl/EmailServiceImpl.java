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

    @Override
    public void sendPasswordChangeConfirmationEmail(String email, String firstName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);

            helper.setTo(email);
            helper.setSubject("Your CampusNest password was changed");
            helper.setFrom("noreply@campusnest.com");

            String htmlContent = buildPasswordChangeConfirmationTemplate(firstName);
            helper.setText(htmlContent, true);

            mailSender.send(message);

            log.info("Password change confirmation email sent successfully to: {}", email);

        } catch (MessagingException e) {
            log.error("Failed to send password change confirmation email to: {}", email, e);
            // Don't throw exception - this is a notification, not critical to the flow
            // The password has already been changed successfully
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

    private String buildPasswordChangeConfirmationTemplate(String firstName) {
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>Password Changed Successfully</title>
            </head>
            <body style="margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background-color: #f4f6f9;">
                <table role="presentation" style="width: 100%%; border-collapse: collapse; background-color: #f4f6f9; padding: 40px 20px;">
                    <tr>
                        <td align="center">
                            <table role="presentation" style="max-width: 600px; width: 100%%; background-color: #ffffff; border-radius: 12px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); overflow: hidden;">
                                <!-- Header with gradient -->
                                <tr>
                                    <td style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%; padding: 40px 30px; text-align: center;">
                                        <div style="background-color: rgba(255,255,255,0.2); width: 80px; height: 80px; border-radius: 50%%; margin: 0 auto 20px; display: flex; align-items: center; justify-content: center;">
                                            <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                                                <path d="M9 12L11 14L15 10M21 12C21 16.9706 16.9706 21 12 21C7.02944 21 3 16.9706 3 12C3 7.02944 7.02944 3 12 3C16.9706 3 21 7.02944 21 12Z" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                                            </svg>
                                        </div>
                                        <h1 style="color: #ffffff; margin: 0; font-size: 28px; font-weight: 600;">Password Changed Successfully</h1>
                                    </td>
                                </tr>

                                <!-- Content -->
                                <tr>
                                    <td style="padding: 40px 30px;">
                                        <p style="color: #2d3748; font-size: 16px; line-height: 1.6; margin: 0 0 20px;">Hi <strong>%s</strong>,</p>

                                        <p style="color: #2d3748; font-size: 16px; line-height: 1.6; margin: 0 0 20px;">
                                            Your CampusNest account password has been changed successfully. You can now use your new password to log in.
                                        </p>

                                        <!-- Info Box -->
                                        <div style="background-color: #ebf8ff; border-left: 4px solid #4299e1; padding: 16px 20px; margin: 30px 0; border-radius: 4px;">
                                            <p style="color: #2c5282; font-size: 14px; margin: 0; line-height: 1.5;">
                                                <strong>Security Notice:</strong> All active sessions on other devices have been logged out automatically for your security.
                                            </p>
                                        </div>

                                        <p style="color: #2d3748; font-size: 16px; line-height: 1.6; margin: 30px 0 20px;">
                                            <strong>Change Details:</strong>
                                        </p>

                                        <table style="width: 100%%; border-collapse: collapse; margin: 0 0 30px;">
                                            <tr>
                                                <td style="padding: 12px; background-color: #f7fafc; border-bottom: 1px solid #e2e8f0; color: #718096; font-size: 14px; width: 40%%;">Time:</td>
                                                <td style="padding: 12px; background-color: #f7fafc; border-bottom: 1px solid #e2e8f0; color: #2d3748; font-size: 14px;">Just now</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 12px; background-color: #ffffff; border-bottom: 1px solid #e2e8f0; color: #718096; font-size: 14px;">Action:</td>
                                                <td style="padding: 12px; background-color: #ffffff; border-bottom: 1px solid #e2e8f0; color: #2d3748; font-size: 14px;">Password Changed</td>
                                            </tr>
                                            <tr>
                                                <td style="padding: 12px; background-color: #f7fafc; color: #718096; font-size: 14px;">Sessions Invalidated:</td>
                                                <td style="padding: 12px; background-color: #f7fafc; color: #2d3748; font-size: 14px;">All devices</td>
                                            </tr>
                                        </table>

                                        <!-- Warning Box -->
                                        <div style="background-color: #fff5f5; border-left: 4px solid #f56565; padding: 16px 20px; margin: 30px 0; border-radius: 4px;">
                                            <p style="color: #c53030; font-size: 14px; margin: 0 0 8px; font-weight: 600;">
                                                Didn't make this change?
                                            </p>
                                            <p style="color: #742a2a; font-size: 14px; margin: 0; line-height: 1.5;">
                                                If you did not request this password change, please contact our support team immediately at <a href="mailto:support@campusnest.com" style="color: #c53030; text-decoration: underline;">support@campusnest.com</a>
                                            </p>
                                        </div>

                                        <p style="color: #718096; font-size: 14px; line-height: 1.6; margin: 30px 0 0;">
                                            Best regards,<br>
                                            <strong style="color: #2d3748;">The CampusNest Team</strong>
                                        </p>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td style="background-color: #f7fafc; padding: 30px; text-align: center; border-top: 1px solid #e2e8f0;">
                                        <p style="color: #a0aec0; font-size: 12px; margin: 0 0 10px; line-height: 1.5;">
                                            This is an automated security notification from CampusNest
                                        </p>
                                        <p style="color: #a0aec0; font-size: 12px; margin: 0; line-height: 1.5;">
                                            © 2024 CampusNest. All rights reserved.
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(firstName);
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
