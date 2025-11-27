package com.campusnest.housingservice.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SearchHousingListingRequest {
    
    private String city;
    
    @DecimalMin(value = "0.0", message = "Minimum price must be 0 or greater")
    private BigDecimal minPrice;
    
    @DecimalMax(value = "50000.0", message = "Maximum price must be $50,000 or less")
    private BigDecimal maxPrice;
    
    private LocalDate availableFrom;
    
    private LocalDate availableTo;
    
    private Integer minBedrooms;
    
    private Integer maxBedrooms;
    
    private Integer minBathrooms;
    
    private Integer maxBathrooms;
    
    // Pagination parameters
    private Integer page = 0;
    
    private Integer size = 20;
    
    // Sorting parameters
    private String sortBy = "createdAt";
    
    private String sortDirection = "desc";

    // Getters and Setters
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public BigDecimal getMinPrice() { return minPrice; }
    public void setMinPrice(BigDecimal minPrice) { this.minPrice = minPrice; }
    
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    
    public LocalDate getAvailableFrom() { return availableFrom; }
    public void setAvailableFrom(LocalDate availableFrom) { this.availableFrom = availableFrom; }
    
    public LocalDate getAvailableTo() { return availableTo; }
    public void setAvailableTo(LocalDate availableTo) { this.availableTo = availableTo; }
    
    public Integer getMinBedrooms() { return minBedrooms; }
    public void setMinBedrooms(Integer minBedrooms) { this.minBedrooms = minBedrooms; }
    
    public Integer getMaxBedrooms() { return maxBedrooms; }
    public void setMaxBedrooms(Integer maxBedrooms) { this.maxBedrooms = maxBedrooms; }
    
    public Integer getMinBathrooms() { return minBathrooms; }
    public void setMinBathrooms(Integer minBathrooms) { this.minBathrooms = minBathrooms; }
    
    public Integer getMaxBathrooms() { return maxBathrooms; }
    public void setMaxBathrooms(Integer maxBathrooms) { this.maxBathrooms = maxBathrooms; }
    
    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }
    
    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }
    
    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    
    public String getSortDirection() { return sortDirection; }
    public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
}