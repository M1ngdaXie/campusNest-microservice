package com.campusnest.userservice.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRoleRequest {
    
    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "^(ADMIN|STUDENT)$", 
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "Role must be either ADMIN or STUDENT"
    )
    private String role;
}