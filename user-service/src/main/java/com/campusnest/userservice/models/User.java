package com.campusnest.userservice.models;

import com.campusnest.userservice.enums.UserRole;
import com.campusnest.userservice.enums.VerificationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    @JsonIgnore
    private String password;

    private String universityDomain; // Extract from email: "stanford.edu"

    @Column(nullable = false)
    private Boolean emailVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verificationStatus = VerificationStatus.PENDING_EMAIL;

    @Column(nullable = false)
    private Boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.STUDENT;

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Column
    private Instant lastLoginAt;
    
    // Additional fields for UserDetails account management
    @Column(nullable = false)
    private Boolean accountLocked = false;
    
    @Column
    private Instant passwordExpiresAt;
    
    @Column
    private Instant accountExpiresAt;

    // ========== UserDetails Implementation ==========
    
    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
            new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    @JsonIgnore
    public String getUsername() {
        return email; // Email is the username
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password; // Return the encrypted password
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return accountExpiresAt == null || accountExpiresAt.isAfter(Instant.now());
    }

    @Override
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return !accountLocked;
    }

    @Override
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return passwordExpiresAt == null || passwordExpiresAt.isAfter(Instant.now());
    }

    @Override
    @JsonIgnore
    public boolean isEnabled() {
        return active && emailVerified;
    }

    // ========== Utility Methods ==========

    @JsonIgnore // This will prevent the method from being serialized as a property
    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonIgnore
    public void updateLastLogin() {
        this.lastLoginAt = Instant.now();
    }
    
    @JsonIgnore
    public void lockAccount() {
        this.accountLocked = true;
    }
    
    @JsonIgnore
    public void unlockAccount() {
        this.accountLocked = false;
    }
    
    @JsonIgnore
    public void setPasswordExpirationDate(Instant expirationDate) {
        this.passwordExpiresAt = expirationDate;
    }
    
    @JsonIgnore
    public void setAccountExpirationDate(Instant expirationDate) {
        this.accountExpiresAt = expirationDate;
    }
    
    @JsonIgnore
    public boolean isAdmin() {
        return role != null && role == UserRole.ADMIN;
    }
    
    @JsonIgnore
    public boolean isStudent() {
        return role != null && role == UserRole.STUDENT;
    }
    
    // ========== ESSENTIAL GETTERS/SETTERS (Lombok not working) ==========
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public void setPassword(String password) { this.password = password; }
    
    public String getUniversityDomain() { return universityDomain; }
    public void setUniversityDomain(String universityDomain) { this.universityDomain = universityDomain; }
    
    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }
    
    public VerificationStatus getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(VerificationStatus verificationStatus) { this.verificationStatus = verificationStatus; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    
    public Instant getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public Boolean getAccountLocked() { return accountLocked; }
    public void setAccountLocked(Boolean accountLocked) { this.accountLocked = accountLocked; }
    
    public Instant getPasswordExpiresAt() { return passwordExpiresAt; }
    public void setPasswordExpiresAt(Instant passwordExpiresAt) { this.passwordExpiresAt = passwordExpiresAt; }
    
    public Instant getAccountExpiresAt() { return accountExpiresAt; }
    public void setAccountExpiresAt(Instant accountExpiresAt) { this.accountExpiresAt = accountExpiresAt; }
}
