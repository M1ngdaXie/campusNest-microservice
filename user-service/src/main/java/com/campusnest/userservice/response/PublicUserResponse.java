package com.campusnest.userservice.response;

import com.campusnest.userservice.models.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PublicUserResponse {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("universityDomain")
    private String universityDomain;

    // Factory method to create public profile from User entity
    public static PublicUserResponse from(User user) {
        return PublicUserResponse.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .universityDomain(user.getUniversityDomain())
            .build();
    }
}