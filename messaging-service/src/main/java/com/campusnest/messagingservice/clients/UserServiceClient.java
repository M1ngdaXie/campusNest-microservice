package com.campusnest.messagingservice.clients;

import com.campusnest.messagingservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("/api/user/public/{userId}")
    UserDTO getUserById(@PathVariable("userId") Long userId);
}