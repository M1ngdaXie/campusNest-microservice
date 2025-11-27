package com.campusnest.userservice.services.impl;

import com.campusnest.userservice.enums.VerificationStatus;
import com.campusnest.userservice.models.EmailVerification;
import com.campusnest.userservice.models.User;
import com.campusnest.userservice.repository.EmailVerificationRepository;
import com.campusnest.userservice.repository.UserRepository;
import com.campusnest.userservice.services.EmailService;
import com.campusnest.userservice.services.EmailVerificationService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
@Transactional
public class EmailVerificationServiceImpl implements EmailVerificationService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Value("${app.backend.url:http://localhost:8081}")
    private String backendUrl;

    @Override
    public void sendVerificationEmail(String email) {
       emailVerificationRepository.deleteByEmail(email);
       String token = generateToken();
        EmailVerification emailVerification = new EmailVerification(
                email,
                token,
                Instant.now().plus(24, ChronoUnit.HOURS)
        );
        emailVerificationRepository.save(emailVerification);
        String verificationUrl = buildVerificationUrl(token);
        emailService.sendVerificationEmail(email, verificationUrl);

        log.info("Verification email sent to: {}", email);
    }

    @Override
    public User verifyToken(String token) {
        EmailVerification emailVerification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid token"));
        if (emailVerification.getExpiresAt().isBefore(Instant.now())) {
            throw new IllegalArgumentException("Token has expired");
        }
        if(emailVerification.getUsed()) {
            throw new IllegalArgumentException("Token has already been used");
        }
        User user = userRepository.findByEmail(emailVerification.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setEmailVerified(true);
        user.setVerificationStatus(VerificationStatus.EMAIL_VERIFIED);
        User savedUser = userRepository.save(user);

        emailVerification.setUsed(true);
        emailVerificationRepository.save(emailVerification);

        log.info("Email verified successfully for user: {}", user.getEmail());
        return savedUser;
    }
    private String buildVerificationUrl(String token) {
        return backendUrl + "/api/auth/verify-email?token=" + token;
    }

    private String generateToken() {
        return UUID.randomUUID().toString() + "-" +
                System.currentTimeMillis() + "-" +
                UUID.randomUUID().toString();
    }

}
