package com.campusnest.housingservice.services;

import com.campusnest.housingservice.models.HousingListing;
import com.campusnest.housingservice.repository.HousingListingRepository;
import com.campusnest.housingservice.repository.ListingImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service("housingListingService")
@Transactional
@Slf4j
public class HousingListingServiceImpl implements HousingListingService {

    @Autowired
    private HousingListingRepository housingListingRepository;

    @Autowired
    private ListingImageRepository listingImageRepository;

    // TODO: In a real microservices architecture, we would call user-service via REST/gRPC
    // to get userId from email. For now, we'll use a placeholder.
    private Long getUserIdFromEmail(String email) {
        // This should be a REST call to user-service
        // For now, return a placeholder - will be handled by API Gateway passing user ID
        log.warn("getUserIdFromEmail called - should be service-to-service call in real microservices");
        return 1L; // Placeholder
    }

    @Override
    public HousingListing createListing(HousingListing listing, String ownerEmail) {
        // In a real microservices setup, the API Gateway would inject userId from JWT
        // For now, we'll just set the ownerEmail
        listing.setOwnerEmail(ownerEmail);
        listing.setOwnerId(getUserIdFromEmail(ownerEmail)); // Placeholder
        listing.setIsActive(true);
        listing.setCreatedAt(LocalDateTime.now());
        listing.setUpdatedAt(LocalDateTime.now());

        return housingListingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "housing-listings", key = "#id")
    public Optional<HousingListing> findById(Long id) {
        return housingListingRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> findAllActive() {
        return housingListingRepository.findByIsActiveTrueOrderByCreatedAtDesc();
    }

    @Override
    @CachePut(value = "housing-listings", key = "#id")
    @CacheEvict(value = "housing-search", allEntries = true)
    public HousingListing updateListing(Long id, HousingListing updatedListing, String requesterEmail) {
        verifyOwnershipOrAdmin(id, requesterEmail);

        HousingListing existingListing = housingListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        // Update fields
        existingListing.setTitle(updatedListing.getTitle());
        existingListing.setDescription(updatedListing.getDescription());
        existingListing.setPrice(updatedListing.getPrice());
        existingListing.setAddress(updatedListing.getAddress());
        existingListing.setCity(updatedListing.getCity());
        existingListing.setBedrooms(updatedListing.getBedrooms());
        existingListing.setBathrooms(updatedListing.getBathrooms());
        existingListing.setAvailableFrom(updatedListing.getAvailableFrom());
        existingListing.setAvailableTo(updatedListing.getAvailableTo());
        existingListing.setUpdatedAt(LocalDateTime.now());

        return housingListingRepository.save(existingListing);
    }

    @Override
    @CacheEvict(value = {"housing-listings", "housing-search"}, key = "#id", allEntries = true)
    public void deleteListing(Long id, String requesterEmail) {
        verifyOwnershipOrAdmin(id, requesterEmail);

        HousingListing listing = housingListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        // Soft delete - set isActive to false
        listing.setIsActive(false);
        listing.setUpdatedAt(LocalDateTime.now());
        housingListingRepository.save(listing);
    }

    // Optional: Add hard delete method for complete removal
    public void hardDeleteListing(Long id, String requesterEmail) {
        verifyOwnershipOrAdmin(id, requesterEmail);

        HousingListing listing = housingListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        // Delete all associated images first
        listingImageRepository.deleteByListing(listing);

        // Hard delete the listing
        housingListingRepository.delete(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> findByOwner(String ownerEmail) {
        return housingListingRepository.findByOwnerEmailOrderByCreatedAtDesc(ownerEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> findActiveByOwner(String ownerEmail) {
        return housingListingRepository.findByOwnerEmailAndIsActiveTrueOrderByCreatedAtDesc(ownerEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> searchListings(String city, BigDecimal minPrice, BigDecimal maxPrice,
                                              LocalDate availableFrom, LocalDate availableTo) {
        return housingListingRepository.findBySearchCriteria(city, minPrice, maxPrice, availableFrom, availableTo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> searchByCity(String city) {
        return housingListingRepository.findActiveByCityContainingIgnoreCase(city);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> searchByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
        return housingListingRepository.findActiveByPriceBetween(minPrice, maxPrice);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwnerOrAdmin(Long listingId, String requesterEmail) {
        // In real microservices, we would call user-service to check if user is admin
        // For now, we'll just check ownership
        // TODO: Add service-to-service call to check admin role
        return isOwner(listingId, requesterEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isOwner(Long listingId, String requesterEmail) {
        Optional<HousingListing> listing = housingListingRepository.findById(listingId);

        return listing.isPresent() &&
               listing.get().getOwnerEmail().equals(requesterEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOwnershipOrAdmin(Long listingId, String requesterEmail) {
        if (!isOwnerOrAdmin(listingId, requesterEmail)) {
            throw new RuntimeException("Access denied. User is not owner or admin for listing: " + listingId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<HousingListing> findAll() {
        return housingListingRepository.findAll();
    }

    @Override
    public HousingListing toggleListingStatus(Long id, String adminEmail) {
        // TODO: Verify admin role via service-to-service call
        // For now, allow the operation (API Gateway should handle authorization)

        HousingListing listing = housingListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));

        listing.setIsActive(!listing.getIsActive());
        listing.setUpdatedAt(LocalDateTime.now());

        return housingListingRepository.save(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalActiveListings() {
        return housingListingRepository.countByIsActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long getTotalListingsByOwner(String ownerEmail) {
        return housingListingRepository.countByOwnerEmail(ownerEmail);
    }

    @Cacheable(value = "housing-listings", key = "#id")
    public HousingListing getById(Long id){
        return housingListingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Listing not found: " + id));
    }

    @Cacheable(value = "housing-search",
            key = "#city + ':' + #minPrice + ':' + #maxPrice + ':' + #pageable.pageNumber")
    public Page<HousingListing> searchListings(String city, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return housingListingRepository.findByCityAndPriceBetween(city, minPrice, maxPrice, pageable);
    }

    @CachePut(value = "housing-listings", key = "#result.id")
    public HousingListing saveOrUpdate(HousingListing listing) {
        return housingListingRepository.save(listing);
    }

    @CacheEvict(value = "housing-listings", key = "#id")
    public void delete(Long id) {
        housingListingRepository.deleteById(id);
        clearSearchCache();
    }

    @CacheEvict(value = "housing-search", allEntries = true)
    public void clearSearchCache() {
        // Empty method - annotation handles cache clearing
    }
}