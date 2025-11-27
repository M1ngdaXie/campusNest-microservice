package com.campusnest.userservice.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UniversityInfo {
    
    @JsonProperty("name")
    private String name; // "Stanford University"
    
    @JsonProperty("domain")
    private String domain; // "stanford.edu"
    
    @JsonProperty("logo_url")
    private String logoUrl; // University logo for branding
    
    @JsonProperty("support_contact")
    private String supportContact; // University-specific support
    
    @JsonProperty("verification_info")
    private String verificationInfo; // Custom verification instructions
}