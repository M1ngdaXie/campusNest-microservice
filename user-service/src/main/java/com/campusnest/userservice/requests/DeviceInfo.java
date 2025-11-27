package com.campusnest.userservice.requests;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceInfo {
    private String deviceType;
    private String deviceId;
    private String appVersion; // "1.0.0"
    private String osVersion;  // "iOS 17.1"
    private String userAgent;  // Browser user agent
}