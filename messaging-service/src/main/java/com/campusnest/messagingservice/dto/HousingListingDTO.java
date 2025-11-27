package com.campusnest.messagingservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HousingListingDTO {
    private Long id;
    private String title;
    private String address;
    private BigDecimal price;
    private String propertyType;
    private Integer bedrooms;
    private Integer bathrooms;
    private String mainImageUrl;
}