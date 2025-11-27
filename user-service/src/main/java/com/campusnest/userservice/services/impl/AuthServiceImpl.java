package com.campusnest.userservice.services.impl;

import com.campusnest.userservice.enums.VerificationStatus;
import com.campusnest.userservice.models.PasswordResetToken;
import com.campusnest.userservice.models.RefreshToken;
import com.campusnest.userservice.models.User;
import com.campusnest.userservice.repository.PasswordResetTokenRepository;
import com.campusnest.userservice.repository.RefreshTokenRepository;
import com.campusnest.userservice.repository.UserRepository;
import com.campusnest.userservice.requests.DeviceInfo;
import com.campusnest.userservice.requests.ForgotPasswordRequest;
import com.campusnest.userservice.requests.LoginRequest;
import com.campusnest.userservice.requests.LogoutRequest;
import com.campusnest.userservice.requests.RefreshTokenRequest;
import com.campusnest.userservice.requests.ResetPasswordRequest;
import com.campusnest.userservice.response.ForgotPasswordResponse;
import com.campusnest.userservice.response.LoginResponse;
import com.campusnest.userservice.requests.RegisterRequest;
import com.campusnest.userservice.response.LogoutResponse;
import com.campusnest.userservice.response.RefreshTokenResponse;
import com.campusnest.userservice.response.RegisterResponse;
import com.campusnest.userservice.response.ResetPasswordResponse;
import com.campusnest.userservice.response.UserResponse;
import com.campusnest.userservice.services.AuthService;
import com.campusnest.userservice.services.EmailVerificationService;
import com.campusnest.userservice.services.JwtTokenService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationService emailVerificationService;

    @Autowired
    private UniversityValidator universityService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenService jwtTokenService;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    public RegisterResponse registerUser(RegisterRequest request) {

        try {
            // 1. Validate university domain
            String domain = extractDomain(request.getEmail());

            if (!universityService.isValidUniversityDomain(domain)) {
                return RegisterResponse.invalidUniversity(request.getEmail(), domain);
            }
            
            // 2. Check if user already exists
            if (userRepository.existsByEmail(request.getEmail())) {
                return RegisterResponse.userAlreadyExists(request.getEmail());
            }

            // 3. Create user entity
            User user = createUserFromRequest(request, domain);
            User savedUser = userRepository.save(user);

            // 4. Send verification email
            String universityName = universityService.validateAndGetName(domain);

            try {
                emailVerificationService.sendVerificationEmail(savedUser.getEmail());
                return RegisterResponse.success(savedUser, universityName);

            } catch (RuntimeException e) {
                // User created but email failed - still success but warn user
                log.warn("Email verification failed for user: {}", savedUser.getEmail(), e);
                return RegisterResponse.successWithEmailWarning(savedUser, universityName);
            }

        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            throw new RuntimeException("Registration failed. Please try again.");
        }
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        try{
            User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
            if(user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())){
                return LoginResponse.invalidCredentials();
            }
            if (!user.getActive()) {
                return LoginResponse.accountSuspended();
            }

            // 4. Check email verification
            if (!user.getEmailVerified()) {
                return LoginResponse.emailNotVerified(request.getEmail());
            }
            String accessToken = jwtTokenService.generateAccessToken(user);
            String refreshToken = jwtTokenService.generateRefreshToken(user, request.getDeviceInfo());
            String sessionId = UUID.randomUUID().toString();

            // Create response BEFORE updating lastLoginAt to preserve firstLogin check
            LoginResponse response = LoginResponse.success(
                    accessToken,
                    refreshToken,
                    user,
                    jwtTokenService.getAccessTokenExpiration(),
                    sessionId
            );

            // 6. Update user login info AFTER creating response
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);
            
            return response;

        } catch (Exception e) {
            return LoginResponse.builder()
                    .success(false)
                    .message("Login failed. Please try again.")
                    .build();
        }
    }

    private User createUserFromRequest(RegisterRequest request, String domain) {
        User user = new User();
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setFirstName(request.getFirstName().trim());
        user.setCreatedAt(Instant.now());
        user.setLastName(request.getLastName().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setUniversityDomain(domain);
        user.setVerificationStatus(VerificationStatus.PENDING_EMAIL);
        user.setEmailVerified(false);
        user.setActive(true);
        return user;
    }

    @Override
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request) {
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

            // Check expiration
            if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
                refreshTokenRepository.delete(refreshToken);
                return RefreshTokenResponse.invalidToken();
            }

            User user = refreshToken.getUser();

            // Check user status
            if (!user.getActive() || !user.getEmailVerified()) {
                return RefreshTokenResponse.accountIssue();
            }

            // Optional: Validate device consistency
            if (request.getDeviceInfo() != null && refreshToken.getDeviceId() != null) {
                if (!refreshToken.getDeviceId().equals(request.getDeviceInfo().getDeviceId())) {
                    log.warn("Device mismatch for refresh token - possible security issue");
                    // Could reject or just log warning
                }
            }

            // Generate new tokens (TOKEN ROTATION)
            String newAccessToken = jwtTokenService.generateAccessToken(user);
            String newRefreshToken = jwtTokenService.generateRefreshToken(user, request.getDeviceInfo());

            // Delete old refresh token (important for security)
            refreshTokenRepository.delete(refreshToken);

            // Update last login
            user.setLastLoginAt(Instant.now());
            userRepository.save(user);

            return RefreshTokenResponse.success(
                    newAccessToken,
                    newRefreshToken,
                    UserResponse.from(user),
                    jwtTokenService.getAccessTokenExpiration(),
                    UUID.randomUUID().toString()
            );

        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            return RefreshTokenResponse.invalidToken();
        }
    }

    @Override
    public LogoutResponse logout(LogoutRequest request) {
        try {
            String refreshTokenValue = request.getRefreshToken();
            boolean logoutAllDevices = request.isLogoutAllDevices();
            
            if (logoutAllDevices) {
                return logoutAllDevices(refreshTokenValue, request.getDeviceInfo());
            } else {
                return logoutSingleDevice(refreshTokenValue, request.getDeviceInfo());
            }
            
        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            // Always return success for logout from user perspective
            return LogoutResponse.alreadyLoggedOut();
        }
    }
    
    private LogoutResponse logoutSingleDevice(String refreshTokenValue, DeviceInfo deviceInfo) {
        try {
            RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenValue)
                    .orElse(null);
            
            if (refreshToken == null) {
                log.info("Logout attempt with non-existent or already expired token");
                return LogoutResponse.alreadyLoggedOut();
            }
            
            // Optional: Validate device consistency for additional security
            if (deviceInfo != null && refreshToken.getDeviceId() != null) {
                if (!refreshToken.getDeviceId().equals(deviceInfo.getDeviceId())) {
                    log.warn("Device mismatch during logout - possible security issue. Token: {}, Request: {}",
                            refreshToken.getDeviceId(), deviceInfo.getDeviceId());
                }
            }
            
            // Delete the refresh token
            refreshTokenRepository.delete(refreshToken);
            
            log.info("User logged out successfully: {}", 
                    maskEmailForLogs(refreshToken.getUser().getEmail()));
                    
            return LogoutResponse.success();
            
        } catch (Exception e) {
            log.error("Error during single device logout: {}", e.getMessage());
            return LogoutResponse.alreadyLoggedOut();
        }
    }
    
    private LogoutResponse logoutAllDevices(String refreshTokenValue, DeviceInfo deviceInfo) {
        try {
            // First find the user from the provided refresh token
            RefreshToken currentToken = refreshTokenRepository.findByToken(refreshTokenValue)
                    .orElse(null);
                    
            if (currentToken == null) {
                log.info("Logout all devices attempt with non-existent token");
                return LogoutResponse.alreadyLoggedOut();
            }
            
            User user = currentToken.getUser();
            
            // Find all refresh tokens for this user
            List<RefreshToken> userTokens = refreshTokenRepository.findByUser(user);
            
            if (userTokens.isEmpty()) {
                return LogoutResponse.alreadyLoggedOut();
            }
            
            // Delete all refresh tokens for this user
            refreshTokenRepository.deleteAll(userTokens);
            
            log.info("User logged out from all devices: {} - {} tokens invalidated", 
                    maskEmailForLogs(user.getEmail()), userTokens.size());
                    
            return LogoutResponse.successAllDevices(userTokens.size());
            
        } catch (Exception e) {
            log.error("Error during logout all devices: {}", e.getMessage());
            return LogoutResponse.alreadyLoggedOut();
        }
    }
    
    private String maskEmailForLogs(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }

    @Override
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request, String ipAddress, String userAgent) {
        try {
            String email = request.getEmail().toLowerCase().trim();
            log.info("Password reset request for email: {}", maskEmailForLogs(email));
            
            // Rate limiting - check recent requests by IP first (max 10 per hour)
            Instant oneHourAgo = Instant.now().minusSeconds(3600);
            long recentIpRequests = passwordResetTokenRepository.countRecentTokensByIpAddress(ipAddress, oneHourAgo);
            if (recentIpRequests >= 10) {
                log.warn("Rate limited password reset for IP: {} (requests: {})", ipAddress, recentIpRequests);
                return ForgotPasswordResponse.rateLimited();
            }
            
            // Find user by email
            User user = userRepository.findByEmail(email).orElse(null);
            
            if (user == null) {
                // Security: Don't reveal if email exists, but log the attempt
                log.warn("Password reset attempted for non-existent email: {}", maskEmailForLogs(email));
                return ForgotPasswordResponse.userNotFound();
            }
            
            // Check if user's email is verified
            if (!user.getEmailVerified()) {
                log.warn("Password reset attempted for unverified email: {}", maskEmailForLogs(email));
                return ForgotPasswordResponse.emailNotVerified();
            }
            
            // Rate limiting - check recent requests by user (max 3 per hour)
            long recentUserRequests = passwordResetTokenRepository.countRecentTokensByUser(user, oneHourAgo);
            if (recentUserRequests >= 3) {
                log.warn("Rate limited password reset for user: {} (requests: {})", maskEmailForLogs(email), recentUserRequests);
                return ForgotPasswordResponse.rateLimited();
            }
            
            // Invalidate only UNUSED tokens for this user (keep used/expired ones for rate limiting)
            passwordResetTokenRepository.deleteUnusedTokensByUser(user, Instant.now());
            
            // Generate secure token (UUID + timestamp for uniqueness)
            String resetToken = UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
            
            // Create password reset token (expires in 1 hour)
            PasswordResetToken passwordResetToken = PasswordResetToken.builder()
                    .token(resetToken)
                    .user(user)
                    .expiryDate(Instant.now().plusSeconds(3600)) // 1 hour
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
                    
            passwordResetTokenRepository.save(passwordResetToken);
            
            // Send password reset email
            try {
                sendPasswordResetEmail(user, resetToken);
                log.info("Password reset email sent successfully to: {}", maskEmailForLogs(email));
                return ForgotPasswordResponse.success(maskEmailForLogs(email), 3600L);
            } catch (Exception e) {
                log.error("Failed to send password reset email to: {}", maskEmailForLogs(email), e);
                return ForgotPasswordResponse.emailSendFailure();
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in forgotPassword: {}", e.getMessage(), e);
            return ForgotPasswordResponse.emailSendFailure();
        }
    }
    
    @Override
    public ResetPasswordResponse resetPassword(ResetPasswordRequest request) {
        try {
            String token = request.getToken();
            String newPassword = request.getNewPassword();
            String confirmPassword = request.getConfirmPassword();
            
            log.info("Password reset attempt with token: {}", token.substring(0, 8) + "...");
            
            // Validate password match
            if (!newPassword.equals(confirmPassword)) {
                log.warn("Password reset failed: passwords do not match");
                return ResetPasswordResponse.passwordMismatch();
            }
            
            // Find and validate token
            PasswordResetToken passwordResetToken = passwordResetTokenRepository.findByToken(token)
                    .orElse(null);
                    
            if (passwordResetToken == null) {
                log.warn("Password reset attempted with invalid token: {}", token.substring(0, 8) + "...");
                return ResetPasswordResponse.invalidToken();
            }
            
            // Check if token is expired
            if (passwordResetToken.isExpired()) {
                log.warn("Password reset attempted with expired token for user: {}", 
                        maskEmailForLogs(passwordResetToken.getUser().getEmail()));
                passwordResetTokenRepository.delete(passwordResetToken);
                return ResetPasswordResponse.tokenExpired();
            }
            
            // Check if token was already used
            if (passwordResetToken.isUsed()) {
                log.warn("Password reset attempted with already used token for user: {}", 
                        maskEmailForLogs(passwordResetToken.getUser().getEmail()));
                return ResetPasswordResponse.invalidToken();
            }
            
            User user = passwordResetToken.getUser();
            
            // Validate new password is different from current
            if (passwordEncoder.matches(newPassword, user.getPassword())) {
                log.warn("User attempted to reset to same password: {}", maskEmailForLogs(user.getEmail()));
                return ResetPasswordResponse.samePassword();
            }
            
            // Update user password
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setLastLoginAt(Instant.now()); // Update last activity
            userRepository.save(user);
            
            // Mark token as used
            passwordResetToken.markAsUsed();
            passwordResetTokenRepository.save(passwordResetToken);
            
            // Invalidate all refresh tokens for security (user must log in again)
            List<RefreshToken> userTokens = refreshTokenRepository.findByUser(user);
            if (!userTokens.isEmpty()) {
                refreshTokenRepository.deleteAll(userTokens);
                log.info("Invalidated {} refresh tokens after password reset for user: {}", 
                        userTokens.size(), maskEmailForLogs(user.getEmail()));
            }
            
            log.info("Password reset successful for user: {}", maskEmailForLogs(user.getEmail()));
            return ResetPasswordResponse.success(maskEmailForLogs(user.getEmail()));
            
        } catch (Exception e) {
            log.error("Unexpected error in resetPassword: {}", e.getMessage(), e);
            return ResetPasswordResponse.invalidToken();
        }
    }
    
    private void sendPasswordResetEmail(User user, String resetToken) {
        // TODO: Implement actual email sending
        // This would typically use the same email service as registration
        // For now, we'll log the reset link
        String resetLink = "http://localhost:8080/reset-password?token=" + resetToken;
        log.info("Password reset link for {}: {}", maskEmailForLogs(user.getEmail()), resetLink);
        
        // In a real implementation:
        // emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), resetLink);
    }

    private String extractDomain(String email) {
        return email.substring(email.indexOf("@") + 1).toLowerCase();
    }
}
