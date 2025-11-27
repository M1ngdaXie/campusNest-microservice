package com.campusnest.userservice.response;

import lombok.Data;

@Data
public class UserSummaryResponse {
    
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String universityDomain;
    
    public static UserSummaryResponse fromUser(com.campusnest.userservice.models.User user) {
        if (user == null) return null;
        
        UserSummaryResponse response = new UserSummaryResponse();
        response.setId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setEmail(user.getEmail());
        response.setUniversityDomain(user.getUniversityDomain());
        return response;
    }
    
    public String getFullName() {
        return firstName + " " + lastName;
    }
}