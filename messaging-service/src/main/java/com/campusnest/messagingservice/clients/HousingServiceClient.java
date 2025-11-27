package com.campusnest.messagingservice.clients;

import com.campusnest.messagingservice.dto.HousingListingDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "housing-service")
public interface HousingServiceClient {

    @GetMapping("/api/housing/{id}")
    HousingListingDTO getHousingListingById(@PathVariable("id") Long id);
}