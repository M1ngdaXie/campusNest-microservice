package com.campusnest.housingservice.requests;

import com.campusnest.housingservice.validation.ValidDateRange;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@ValidDateRange
public class CreateHousingListingRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must be less than 255 characters")
    private String title;
    
    @Size(max = 2000, message = "Description must be less than 2000 characters")
    private String description;
    
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @DecimalMax(value = "50000.0", message = "Price must be less than $50,000")
    private BigDecimal price;
    
    @NotBlank(message = "Address is required")
    @Size(max = 500, message = "Address must be less than 500 characters")
    private String address;
    
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must be less than 100 characters")
    private String city;
    
    @NotNull(message = "Number of bedrooms is required")
    @Min(value = 0, message = "Bedrooms must be 0 or greater")
    @Max(value = 10, message = "Bedrooms must be 10 or less")
    private Integer bedrooms;
    
    @NotNull(message = "Number of bathrooms is required")
    @Min(value = 1, message = "Bathrooms must be 1 or greater")
    @Max(value = 10, message = "Bathrooms must be 10 or less")
    private Integer bathrooms;
    
    @NotNull(message = "Available from date is required")
    @Future(message = "Available from date must be in the future")
    private LocalDate availableFrom;
    
    @NotNull(message = "Available to date is required")
    @Future(message = "Available to date must be in the future")
    private LocalDate availableTo;
    
    @Size(max = 10, message = "Maximum 10 images allowed")
    private List<String> s3Keys;
    
    // Custom validation method
    public boolean isAvailableToAfterAvailableFrom() {
        return availableTo != null && availableFrom != null && 
               availableTo.isAfter(availableFrom);
    }
}