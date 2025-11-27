package com.campusnest.userservice.controllers;

import com.campusnest.userservice.models.User;
import com.campusnest.userservice.repository.UserRepository;
import com.campusnest.userservice.requests.UpdateUserRoleRequest;
import com.campusnest.userservice.requests.UpdateUserStatusRequest;
import com.campusnest.userservice.response.AdminOperationResponse;
import com.campusnest.userservice.response.AdminUserDetailResponse;
import com.campusnest.userservice.response.AdminUserListResponse;
import com.campusnest.userservice.response.SystemStatsResponse;
import com.campusnest.userservice.response.UserResponse;
import com.campusnest.userservice.enums.UserRole;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@Slf4j
@PreAuthorize("hasRole('ADMIN')")  // All endpoints require ADMIN role
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<AdminUserListResponse> getAllUsers(
            @AuthenticationPrincipal User currentUser) {
        log.info("Admin {} requesting all users", maskEmail(currentUser.getEmail()));
        
        List<User> users = userRepository.findAll();
        List<UserResponse> userResponses = users.stream()
                .map(UserResponse::from)
                .toList();
        
        AdminUserListResponse response = AdminUserListResponse.create(
            userResponses, 
            maskEmail(currentUser.getEmail())
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailResponse> getUserById(
            @PathVariable Long userId,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("Admin {} requesting user details for ID: {}", 
                maskEmail(currentUser.getEmail()), userId);
        
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        AdminUserDetailResponse response = AdminUserDetailResponse.create(
            UserResponse.from(user),
            maskEmail(currentUser.getEmail())
        );
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<AdminOperationResponse> updateUserRole(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRoleRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("Admin {} updating role for user ID: {} to: {}", 
                maskEmail(currentUser.getEmail()), userId, request.getRole());
        
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            UserRole newRole = UserRole.valueOf(request.getRole().toUpperCase());
            user.setRole(newRole);
            userRepository.save(user);
            
            log.info("Successfully updated user {} role to {}", 
                    maskEmail(user.getEmail()), request.getRole());
            
            AdminOperationResponse response = AdminOperationResponse.success(
                "User role updated successfully",
                UserResponse.from(user),
                maskEmail(currentUser.getEmail())
            );
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            AdminOperationResponse response = AdminOperationResponse.failure(
                "Invalid role: " + request.getRole()
            );
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<AdminOperationResponse> updateUserStatus(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserStatusRequest request,
            @AuthenticationPrincipal User currentUser) {
        
        log.info("Admin {} updating status for user ID: {} to: {}", 
                maskEmail(currentUser.getEmail()), userId, request.getActive() ? "active" : "inactive");
        
        User user = userRepository.findById(userId).orElse(null);
        
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        
        user.setActive(request.getActive());
        userRepository.save(user);
        
        log.info("Successfully updated user {} status to {}", 
                maskEmail(user.getEmail()), request.getActive() ? "active" : "inactive");
        
        AdminOperationResponse response = AdminOperationResponse.success(
            "User status updated successfully",
            UserResponse.from(user),
            maskEmail(currentUser.getEmail())
        );
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    public ResponseEntity<SystemStatsResponse> getSystemStats(
            @AuthenticationPrincipal User currentUser) {
        
        log.info("Admin {} requesting system statistics", maskEmail(currentUser.getEmail()));
        
        long totalUsers = userRepository.count();
        long activeUsers = userRepository.countByActiveTrue();
        long verifiedUsers = userRepository.countByEmailVerifiedTrue();
        
        SystemStatsResponse response = SystemStatsResponse.create(
            totalUsers,
            activeUsers,
            verifiedUsers,
            maskEmail(currentUser.getEmail())
        );
        
        return ResponseEntity.ok(response);
    }

    private String maskEmail(String email) {
        if (email == null) return "null";
        int atIndex = email.indexOf("@");
        return atIndex > 0 ? email.substring(0, 1) + "***" + email.substring(atIndex) : email;
    }
}