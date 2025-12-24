package com.campusnest.userservice.controllers;

import com.campusnest.userservice.models.User;
import com.campusnest.userservice.repository.UserRepository;
import com.campusnest.userservice.requests.ChangePasswordRequest;
import com.campusnest.userservice.response.ChangePasswordResponse;
import com.campusnest.userservice.response.PublicUserResponse;
import com.campusnest.userservice.response.UserResponse;
import com.campusnest.userservice.services.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@Slf4j
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    /**
     * Public endpoint to get user's public profile (no authentication required)
     * Used by other services to display user info in messaging, listings, etc.
     */
    @GetMapping("/public/{userId}")
    public ResponseEntity<PublicUserResponse> getPublicUserProfile(@PathVariable Long userId) {
        log.info("Public profile request for user ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return ResponseEntity.ok(PublicUserResponse.from(user));
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getProfile(@AuthenticationPrincipal User user) {
        log.info("Profile request from user: {} with role: {}", 
                maskEmail(user.getEmail()), user.getRole().name());
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "user", UserResponse.fromLogin(user),
            "permissions", Map.of(
                "can_access_admin_panel", user.getRole().isAdmin(),
                "role_display_name", user.getRole().getDisplayName()
            )
        ));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getDashboard(@AuthenticationPrincipal User user) {
        log.info("Dashboard request from user: {} with role: {}", 
                maskEmail(user.getEmail()), user.getRole().name());
        
        Map<String, Object> dashboardData;
        
        if (user.getRole().isAdmin()) {
            // Admin gets enhanced dashboard with system info
            dashboardData = Map.of(
                "type", "admin_dashboard",
                "welcome_message", "Welcome to the Admin Dashboard, " + user.getFirstName(),
                "quick_actions", new String[]{
                    "View All Users", "System Statistics", "Moderate Listings", "Generate Reports"
                },
                "admin_notifications", new String[]{
                    "3 new user registrations pending review",
                    "2 listings flagged for moderation"
                }
            );
        } else {
            // Student gets regular dashboard
            dashboardData = Map.of(
                "type", "student_dashboard",
                "welcome_message", "Welcome back, " + user.getFirstName() + "!",
                "quick_actions", new String[]{
                    "Browse Listings", "Create Listing", "My Messages", "My Favorites"
                },
                "student_notifications", new String[]{
                    "2 new messages from potential roommates",
                    "Your listing expires in 7 days"
                }
            );
        }
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "user", UserResponse.fromLogin(user),
            "dashboard", dashboardData
        ));
    }

    @PostMapping("/change-password")
    @PreAuthorize("hasRole('STUDENT') or hasRole('ADMIN')")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @AuthenticationPrincipal User user,
            @RequestBody @Valid ChangePasswordRequest request) {

        log.info("Password change request from user: {}", maskEmail(user.getEmail()));

        ChangePasswordResponse response = userService.changePassword(user, request);

        if (response.getSuccess()) {
            log.info("Password changed successfully for user: {}", maskEmail(user.getEmail()));
            return ResponseEntity.ok(response);
        } else {
            log.warn("Password change failed for user: {} - {}",
                    maskEmail(user.getEmail()), response.getMessage());

            // Return appropriate HTTP status based on error type
            HttpStatus status = response.getMessage().contains("incorrect") ?
                HttpStatus.UNAUTHORIZED : HttpStatus.BAD_REQUEST;

            return ResponseEntity.status(status).body(response);
        }
    }


    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }
}