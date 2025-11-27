package com.campusnest.userservice.controllers;

import com.campusnest.userservice.models.User;
import com.campusnest.userservice.requests.ForgotPasswordRequest;
import com.campusnest.userservice.requests.LoginRequest;
import com.campusnest.userservice.requests.LogoutRequest;
import com.campusnest.userservice.requests.RefreshTokenRequest;
import com.campusnest.userservice.requests.ResendVerificationRequest;
import com.campusnest.userservice.requests.ResetPasswordRequest;
import com.campusnest.userservice.response.*;
import com.campusnest.userservice.requests.RegisterRequest;
import com.campusnest.userservice.services.AuthService;
import com.campusnest.userservice.services.EmailVerificationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
@Slf4j
public class AuthController {
    @Autowired
    private AuthService authService;
    
    @Autowired
    private EmailVerificationService emailVerificationService;


    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@RequestBody @Valid RegisterRequest request) {
        log.info("Registration attempt for email: {}",
                maskEmailForLogs(request.getEmail()));
        RegisterResponse response = authService.registerUser(request);

        if (response.getSuccess()) {
            log.info("Registration successful for email: {}",
                    maskEmailForLogs(request.getEmail()));
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            log.info("Registration failed for email: {} - {}",
                    maskEmailForLogs(request.getEmail()), response.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        log.info("Login attempt for email: {}",
                maskEmailForLogs(request.getEmail()));
        LoginResponse response = authService.login(request);

        if (response.getSuccess()) {
            log.info("Login successful for email: {}",
                    maskEmailForLogs(request.getEmail()));
            return ResponseEntity.ok(response);
        } else {
            log.info("Login failed for email: {} - {}",
                    maskEmailForLogs(request.getEmail()), response.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }
    @GetMapping("/verify-email")
    public ResponseEntity<VerificationResponse> verifyEmail(@RequestParam String token) {
        log.info("Email verification attempt with token: {}",
                token.substring(0, Math.min(token.length(), 10)) + "...");

        try {
            User user = emailVerificationService.verifyToken(token);

            log.info("Email verification successful for user: {}",
                    maskEmailForLogs(user.getEmail()));

            VerificationResponse response = VerificationResponse.builder()
                    .success(true)
                    .message("Email verified successfully! You can now log in to your account.")
                    .email(maskEmailForLogs(user.getEmail()))
                    .verificationStatus(user.getVerificationStatus().name())
                    .redirectUrl("http://localhost:8080/login")
                    .build();

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("Email verification failed: {}", e.getMessage());

            VerificationResponse response = VerificationResponse.builder()
                    .success(false)
                    .message(e.getMessage())
                    .redirectUrl("https://yourapp.com/verification-error") // Error page URL
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ResendVerificationResponse> resendVerification(
            @RequestBody ResendVerificationRequest request) {

        log.info("Resend verification attempt for email: {}",
                maskEmailForLogs(request.getEmail()));

        try {
            emailVerificationService.sendVerificationEmail(request.getEmail());

            ResendVerificationResponse response = ResendVerificationResponse.builder()
                    .success(true)
                    .message("Verification email sent! Please check your inbox.")
                    .email(maskEmailForLogs(request.getEmail()))
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to resend verification email for: {}",
                    maskEmailForLogs(request.getEmail()), e);

            ResendVerificationResponse response = ResendVerificationResponse.builder()
                    .success(false)
                    .message("Failed to send verification email. Please try again later.")
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken(
            @RequestBody @Valid RefreshTokenRequest request) {

        log.info("Token refresh attempt with device: {}",
                request.getDeviceInfo() != null ? request.getDeviceInfo().getDeviceType() : "Unknown");

        try {
            RefreshTokenResponse response = authService.refreshAccessToken(request);

            if (response.getSuccess()) {
                log.info("Token refresh successful");
                return ResponseEntity.ok(response);
            } else {
                log.warn("Token refresh failed: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            log.error("Token refresh error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(RefreshTokenResponse.invalidToken());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(@RequestBody @Valid LogoutRequest request) {
        log.info("Logout attempt - logoutAllDevices: {}", request.isLogoutAllDevices());
        
        try {
            LogoutResponse response = authService.logout(request);
            
            if (response.getSuccess()) {
                log.info("Logout successful - {} tokens invalidated", response.getTokensInvalidated());
            } else {
                log.info("Logout completed - {}", response.getMessage());
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Logout error: {}", e.getMessage());
            return ResponseEntity.ok(LogoutResponse.alreadyLoggedOut()); // Always return success for logout
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(
            @RequestBody @Valid ForgotPasswordRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("Forgot password request for email: {}", maskEmailForLogs(request.getEmail()));
        
        try {
            String ipAddress = getClientIpAddress(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            
            ForgotPasswordResponse response = authService.forgotPassword(request, ipAddress, userAgent);
            
            if (response.getSuccess()) {
                log.info("Forgot password request processed successfully for email: {}", 
                        maskEmailForLogs(request.getEmail()));
                return ResponseEntity.ok(response);
            } else {
                HttpStatus status = response.getMessage().contains("rate limited") ? 
                    HttpStatus.TOO_MANY_REQUESTS : HttpStatus.BAD_REQUEST;
                    
                log.warn("Forgot password request failed for email: {} - {}", 
                        maskEmailForLogs(request.getEmail()), response.getMessage());
                        
                return ResponseEntity.status(status).body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in forgot password for email: {}", 
                    maskEmailForLogs(request.getEmail()), e);
                    
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ForgotPasswordResponse.emailSendFailure());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResetPasswordResponse> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        log.info("Password reset request with token: {}", 
                request.getToken().substring(0, Math.min(8, request.getToken().length())) + "...");
        
        try {
            ResetPasswordResponse response = authService.resetPassword(request);
            
            if (response.getSuccess()) {
                log.info("Password reset successful for user: {}", response.getEmail());
                return ResponseEntity.ok(response);
            } else {
                log.warn("Password reset failed: {}", response.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
            
        } catch (Exception e) {
            log.error("Unexpected error in password reset: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ResetPasswordResponse.invalidToken());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    private String maskEmailForLogs(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }
}
