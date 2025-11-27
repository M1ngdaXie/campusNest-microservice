package com.campusnest.userservice.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UserRole {
    
    STUDENT("Student", "Regular university student user"),
    ADMIN("Administrator", "Platform administrator with full access");

    private final String displayName;
    private final String description;
    
    public boolean isAdmin() {
        return this == ADMIN;
    }
    
    public boolean isStudent() {
        return this == STUDENT;
    }
}