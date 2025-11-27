package com.campusnest.housingservice.services;

import com.campusnest.housingservice.models.HousingListing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HousingListingService {
    
    // Basic CRUD operations
    HousingListing createListing(HousingListing listing, String ownerEmail);
    
    Optional<HousingListing> findById(Long id);
    
    List<HousingListing> findAllActive();
    
    HousingListing updateListing(Long id, HousingListing updatedListing, String requesterEmail);
    
    void deleteListing(Long id, String requesterEmail);
    
    // Owner-specific operations
    List<HousingListing> findByOwner(String ownerEmail);
    
    List<HousingListing> findActiveByOwner(String ownerEmail);
    
    // Search operations
    List<HousingListing> searchListings(String city, BigDecimal minPrice, BigDecimal maxPrice, 
                                       LocalDate availableFrom, LocalDate availableTo);
    
    List<HousingListing> searchByCity(String city);
    
    List<HousingListing> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
    
    // Security operations
    boolean isOwnerOrAdmin(Long listingId, String requesterEmail);
    
    boolean isOwner(Long listingId, String requesterEmail);
    
    void verifyOwnershipOrAdmin(Long listingId, String requesterEmail);
    
    // Admin operations
    List<HousingListing> findAll();
    
    HousingListing toggleListingStatus(Long id, String adminEmail);
    
    // Statistics
    long getTotalActiveListings();
    
    long getTotalListingsByOwner(String ownerEmail);
}