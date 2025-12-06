package com.campusnest.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private List<String> publicEndpoints = new ArrayList<>();
    private List<String> publicReadEndpoints = new ArrayList<>();
    private List<String> fullyPublicEndpoints = new ArrayList<>();
    private List<String> protectedEndpoints = new ArrayList<>();

    public List<String> getPublicEndpoints() {
        return publicEndpoints;
    }

    public void setPublicEndpoints(List<String> publicEndpoints) {
        this.publicEndpoints = publicEndpoints;
    }

    public List<String> getPublicReadEndpoints() {
        return publicReadEndpoints;
    }

    public void setPublicReadEndpoints(List<String> publicReadEndpoints) {
        this.publicReadEndpoints = publicReadEndpoints;
    }

    public List<String> getFullyPublicEndpoints() {
        return fullyPublicEndpoints;
    }

    public void setFullyPublicEndpoints(List<String> fullyPublicEndpoints) {
        this.fullyPublicEndpoints = fullyPublicEndpoints;
    }

    public List<String> getProtectedEndpoints() {
        return protectedEndpoints;
    }

    public void setProtectedEndpoints(List<String> protectedEndpoints) {
        this.protectedEndpoints = protectedEndpoints;
    }
}