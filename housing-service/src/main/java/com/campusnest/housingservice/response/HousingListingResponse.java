package com.campusnest.housingservice.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class HousingListingResponse {

    private Long id;

    private String title;

    private String description;

    private BigDecimal price;

    private String address;

    private String city;

    private Integer bedrooms;

    private Integer bathrooms;

    private LocalDate availableFrom;

    private LocalDate availableTo;

    private Boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // Owner information (denormalized for microservices architecture)
    private Long ownerId;
    private String ownerEmail;

    private List<ImageInfo> images;

    @Data
    public static class ImageInfo {
        private Long id;
        private String s3Key;
        private String imageUrl; // Signed URL - will be generated when needed
        private Boolean isPrimary;
        private Integer displayOrder;
    }
}