package com.campusnest.housingservice.services;

import com.campusnest.housingservice.dto.CoordinatesDTO;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class GeocodingService {
    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    private final GeoApiContext geoApiContext;

    public GeocodingService(@Value("${google.maps.api.key}") String apiKey) {
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
        logger.info("GeoApiContext initialized with provided API key.");
    }

    @Cacheable(value = "geocoding", key = "#address")
    public CoordinatesDTO geocode(String address) {
        try {
            logger.info("Getting address : {}", address);
            GeocodingResult[] results = GeocodingApi.geocode(geoApiContext, address).await();
            if (results != null && results.length > 0) {
                LatLng location = results[0].geometry.location;
                String formattedAddress = results[0].formattedAddress;
                logger.info("Geocoding successful : {} -> ({}, {})", address, location.lat, location.lng);
                return new CoordinatesDTO(
                        BigDecimal.valueOf(location.lat),
                        BigDecimal.valueOf(location.lng),
                        true,
                        formattedAddress
                );
            } else {
                logger.warn("No geocoding results for address: {}", address);
                return new CoordinatesDTO(null, null, false, null);
            }
        } catch (Exception e) {
            logger.error("Error geocoding address: {}", address, e);
            return new CoordinatesDTO(null, null, false, null);
        }
    }
    @Cacheable(value = "reverse-geocoding", key = "#latitude + ',' + #longitude")
    public java.util.Optional<String> reverseGeocode(Double latitude, Double longitude) {
        try {
            if (latitude == null || longitude == null) {
                logger.warn("Cannot reverse geocode with null coordinates");
                return java.util.Optional.empty();
            }

            LatLng location = new LatLng(latitude, longitude);
            GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext, location).await();

            if (results != null && results.length > 0) {
                logger.info("Reverse geocoding successful: ({}, {}) -> {}", latitude, longitude, results[0].formattedAddress);
                return java.util.Optional.of(results[0].formattedAddress);
            }

            logger.warn("No reverse geocoding results for: ({}, {})", latitude, longitude);
            return java.util.Optional.empty();

        } catch (Exception e) {
            logger.error("Error reverse geocoding: ({}, {})", latitude, longitude, e);
            return java.util.Optional.empty();
        }
    }
}