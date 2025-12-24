package com.campusnest.notificationservice.services;

import com.campusnest.notificationservice.enums.NotificationStatus;
import com.campusnest.notificationservice.models.Notification;
import com.campusnest.notificationservice.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
public class EmailChannelHandler {
    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationRepository notificationRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void send(Notification notification) {
        try {
            // Get email directly from notification object
            String userEmail = notification.getUserEmail();

            if (userEmail == null) {
                log.warn("No email found for userId: {}. Email notifications require userEmail in the notification.",
                        notification.getUserId());
                notification.setStatus(NotificationStatus.FAILED);
                notificationRepository.save(notification);
                return;
            }

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(userEmail);
            message.setSubject(notification.getTitle());
            message.setText(buildEmailBody(notification));

            mailSender.send(message);

            notification.setStatus(NotificationStatus.SENT);
            notification.setSendAt(LocalDateTime.now());
            notificationRepository.save(notification);

            log.info("Email sent successfully to userId: {}", notification.getUserId());
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage(), e);
            notification.setStatus(NotificationStatus.FAILED);
            notificationRepository.save(notification);
        }
    }

    private String buildEmailBody(Notification notification) {
        StringBuilder body = new StringBuilder();
        body.append(notification.getMessage()).append("\n\n");

        if (notification.getActionUrl() != null) {
            body.append("Click here to view: ").append(notification.getActionUrl()).append("\n\n");
        }

        body.append("---\n");
        body.append("CampusNest Team\n");
        body.append("If you wish to unsubscribe from these emails, please update your notification preferences.");

        return body.toString();
    }
}
