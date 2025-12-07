package com.campusnest.housingservice.controllers;

import com.campusnest.housingservice.models.HousingListing;
import com.campusnest.housingservice.models.ListingImage;
import com.campusnest.housingservice.repository.HousingListingRepository;
import com.campusnest.housingservice.repository.ListingImageRepository;
import com.campusnest.housingservice.requests.CreateHousingListingRequest;
import com.campusnest.housingservice.requests.SearchHousingListingRequest;
import com.campusnest.housingservice.requests.UpdateHousingListingRequest;
import com.campusnest.housingservice.response.HousingListingResponse;
import com.campusnest.housingservice.response.HousingListingSummaryResponse;
import com.campusnest.housingservice.services.HousingListingService;
import com.campusnest.housingservice.services.S3Service;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/housing")
@Slf4j
public class HousingListingController {

    @Autowired
    private HousingListingService housingListingService;

    @Autowired
    private HousingListingRepository housingListingRepository;

    @Autowired
    private S3Service s3Service;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;


    @GetMapping("/cache-test")
    public String testRedisCache() {
        // Put a test key in Redis cache manually
        redisTemplate.opsForValue().set("test-key", "Hello Redis!");
        String value = (String) redisTemplate.opsForValue().get("test-key");
        return "Redis test value: " + value;
    }


    // Create a new housing listing
    @PostMapping
    // @PreAuthorize removed - API Gateway handles authentication
    public ResponseEntity<?> createListing(@Valid @RequestBody CreateHousingListingRequest request,
                                         BindingResult bindingResult,
                                         @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
            }

            if (!request.isAvailableToAfterAvailableFrom()) {
                Map<String, String> error = new HashMap<>();
                error.put("availableTo", "Available to date must be after available from date");
                return ResponseEntity.badRequest().body(error);
            }

            // Convert request to entity
            HousingListing listing = new HousingListing();
            listing.setTitle(request.getTitle());
            listing.setDescription(request.getDescription());
            listing.setPrice(request.getPrice());
            listing.setAddress(request.getAddress());
            listing.setCity(request.getCity());
            listing.setBedrooms(request.getBedrooms());
            listing.setBathrooms(request.getBathrooms());
            listing.setAvailableFrom(request.getAvailableFrom());
            listing.setAvailableTo(request.getAvailableTo());

            // Create the listing (userEmail comes from API Gateway header)
            if (userEmail == null || userEmail.isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Authentication required");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
            }

            HousingListing savedListing = housingListingService.createListing(listing, userEmail);

            // Handle image associations if provided
            if (request.getS3Keys() != null && !request.getS3Keys().isEmpty()) {
                associateImages(savedListing, request.getS3Keys());
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(convertToResponse(savedListing));

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get all active listings (public endpoint)
    @GetMapping
    public ResponseEntity<List<HousingListingSummaryResponse>> getAllActiveListings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<HousingListing> allListings = housingListingService.findAllActive();

            // Apply pagination
            List<HousingListing> paginatedListings = applyPagination(allListings, page, size);

            List<HousingListingSummaryResponse> response = paginatedListings.stream()
                    .map(this::convertToSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting active listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get listing by ID (public endpoint)
    @GetMapping("/{id}")
    public ResponseEntity<HousingListingResponse> getListingById(@PathVariable Long id) {
        try {
            Optional<HousingListing> listing = housingListingService.findById(id);

            if (listing.isEmpty() || !listing.get().getIsActive()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(convertToResponse(listing.get()));

        } catch (Exception e) {
            log.error("Error getting listing by id: " + id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Update listing (owner or admin only)
    @PutMapping("/{id}")
    // @PreAuthorize removed - API Gateway handles authentication
    public ResponseEntity<?> updateListing(@PathVariable Long id,
                                         @Valid @RequestBody UpdateHousingListingRequest request,
                                         BindingResult bindingResult,
                                         @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            if (bindingResult.hasErrors()) {
                return ResponseEntity.badRequest().body(getValidationErrors(bindingResult));
            }

            if (!request.isAvailableToAfterAvailableFrom()) {
                Map<String, String> error = new HashMap<>();
                error.put("availableTo", "Available to date must be after available from date");
                return ResponseEntity.badRequest().body(error);
            }

            // Convert request to entity
            HousingListing updatedListing = new HousingListing();
            updatedListing.setTitle(request.getTitle());
            updatedListing.setDescription(request.getDescription());
            updatedListing.setPrice(request.getPrice());
            updatedListing.setAddress(request.getAddress());
            updatedListing.setCity(request.getCity());
            updatedListing.setBedrooms(request.getBedrooms());
            updatedListing.setBathrooms(request.getBathrooms());
            updatedListing.setAvailableFrom(request.getAvailableFrom());
            updatedListing.setAvailableTo(request.getAvailableTo());

            if (userEmail == null || userEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }
            HousingListing saved = housingListingService.updateListing(id, updatedListing, userEmail);

            // Handle image updates if provided
            if (request.getS3Keys() != null) {
                updateImages(saved, request.getS3Keys());
            }

            return ResponseEntity.ok(convertToResponse(saved));

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Soft delete listing (owner or admin only)
    @DeleteMapping("/{id}")
    // @PreAuthorize removed - API Gateway handles authentication
    public ResponseEntity<?> deleteListing(@PathVariable Long id,
                                         @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            if (userEmail == null || userEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
            }
            housingListingService.deleteListing(id, userEmail);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Listing deactivated successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get user's own listings
    @GetMapping("/my-listings")
    // @PreAuthorize removed - API Gateway handles authentication
    public ResponseEntity<?> getMyListings(
            @RequestParam(defaultValue = "false") boolean includeInactive,
            @RequestHeader(value = "X-User-Email", required = false) String userEmail) {
        try {
            if (userEmail == null || userEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            List<HousingListing> listings;

            if (includeInactive) {
                listings = housingListingService.findByOwner(userEmail);
            } else {
                listings = housingListingService.findActiveByOwner(userEmail);
            }

            List<HousingListingSummaryResponse> response = listings.stream()
                    .map(this::convertToSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting my listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Search listings with filters - OPTIMIZED WITH DATABASE-LEVEL PAGINATION
    @PostMapping("/search")
    public ResponseEntity<List<HousingListingSummaryResponse>> searchListings(
            @Valid @RequestBody SearchHousingListingRequest request) {
        try {
            // Create Pageable for database-level pagination
            int pageNum = request.getPage() != null ? request.getPage() : 0;
            int pageSize = request.getSize() != null ? request.getSize() : 20;

            // Create Sort object based on sortBy and sortDirection
            String sortField = request.getSortBy() != null ? request.getSortBy() : "createdAt";
            Sort.Direction direction = "desc".equals(request.getSortDirection()) ?
                Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(direction, sortField));

            // Use repository paginated method - loads ONLY the requested page from DB
            Page<HousingListing> page = housingListingRepository.findByIsActiveTrue(pageable);

            // Convert to response - only processes the small page of results
            List<HousingListingSummaryResponse> response = page.getContent().stream()
                    .map(this::convertToSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error searching listings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Admin endpoint to get all listings (including inactive)
    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<HousingListingSummaryResponse>> getAllListingsForAdmin() {
        try {
            List<HousingListing> listings = housingListingService.findAll();

            List<HousingListingSummaryResponse> response = listings.stream()
                    .map(this::convertToSummaryResponse)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error getting all listings for admin", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Admin endpoint to toggle listing status
    @PostMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> toggleListingStatus(@PathVariable Long id,
                                                Authentication authentication) {
        try {
            HousingListing listing = housingListingService.toggleListingStatus(id, authentication.getName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Listing status updated successfully");
            response.put("isActive", listing.getIsActive());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // Get statistics
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStatistics(Authentication authentication) {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalActiveListings", housingListingService.getTotalActiveListings());

            if (authentication != null) {
                stats.put("userListings", housingListingService.getTotalListingsByOwner(authentication.getName()));
            }

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting statistics", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods
    private Map<String, String> getValidationErrors(BindingResult bindingResult) {
        Map<String, String> errors = new HashMap<>();
        bindingResult.getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage()));
        return errors;
    }

    private void associateImages(HousingListing listing, List<String> s3Keys) {
        if (s3Keys == null || s3Keys.isEmpty()) {
            return;
        }

        for (int i = 0; i < s3Keys.size(); i++) {
            ListingImage image = new ListingImage();
            image.setListing(listing);
            image.setS3Key(s3Keys.get(i));
            image.setDisplayOrder(i + 1);
            image.setIsPrimary(i == 0); // First image is primary by default

            listingImageRepository.save(image);
        }
    }

    @Transactional
    private void updateImages(HousingListing listing, List<String> s3Keys) {
        // Delete existing images for this listing
        listingImageRepository.deleteByListing(listing);

        // Associate new images if provided
        if (s3Keys != null && !s3Keys.isEmpty()) {
            associateImages(listing, s3Keys);
        }
    }

    private boolean filterByBedBath(HousingListing listing, SearchHousingListingRequest request) {
        if (request.getMinBedrooms() != null && listing.getBedrooms() < request.getMinBedrooms()) {
            return false;
        }
        if (request.getMaxBedrooms() != null && listing.getBedrooms() > request.getMaxBedrooms()) {
            return false;
        }
        if (request.getMinBathrooms() != null && listing.getBathrooms() < request.getMinBathrooms()) {
            return false;
        }
        if (request.getMaxBathrooms() != null && listing.getBathrooms() > request.getMaxBathrooms()) {
            return false;
        }
        return true;
    }

    private List<HousingListing> applySorting(List<HousingListing> listings, String sortBy, String sortDirection) {
        return listings.stream()
                .sorted((l1, l2) -> {
                    int comparison = 0;
                    switch (sortBy != null ? sortBy : "createdAt") {
                        case "price":
                            comparison = l1.getPrice().compareTo(l2.getPrice());
                            break;
                        case "bedrooms":
                            comparison = l1.getBedrooms().compareTo(l2.getBedrooms());
                            break;
                        case "bathrooms":
                            comparison = l1.getBathrooms().compareTo(l2.getBathrooms());
                            break;
                        case "city":
                            comparison = l1.getCity().compareTo(l2.getCity());
                            break;
                        default: // createdAt
                            comparison = l1.getCreatedAt().compareTo(l2.getCreatedAt());
                            break;
                    }
                    return "desc".equals(sortDirection) ? -comparison : comparison;
                })
                .collect(Collectors.toList());
    }

    private List<HousingListing> applyPagination(List<HousingListing> listings, Integer page, Integer size) {
        int pageNum = page != null ? page : 0;
        int pageSize = size != null ? size : 20;

        int start = pageNum * pageSize;
        int end = Math.min(start + pageSize, listings.size());

        if (start >= listings.size()) {
            return List.of();
        }

        return listings.subList(start, end);
    }

    private HousingListingResponse convertToResponse(HousingListing listing) {
        HousingListingResponse response = new HousingListingResponse();
        response.setId(listing.getId());
        response.setTitle(listing.getTitle());
        response.setDescription(listing.getDescription());
        response.setPrice(listing.getPrice());
        response.setAddress(listing.getAddress());
        response.setCity(listing.getCity());
        response.setBedrooms(listing.getBedrooms());
        response.setBathrooms(listing.getBathrooms());
        response.setAvailableFrom(listing.getAvailableFrom());
        response.setAvailableTo(listing.getAvailableTo());
        response.setIsActive(listing.getIsActive());
        response.setCreatedAt(listing.getCreatedAt());
        response.setUpdatedAt(listing.getUpdatedAt());

        // Set owner info from denormalized fields
        response.setOwnerEmail(listing.getOwnerEmail());
        response.setOwnerId(listing.getOwnerId());

        // Set images
        if (listing.getImages() != null) {
            List<HousingListingResponse.ImageInfo> imageInfos = listing.getImages().stream()
                    .map(image -> {
                        HousingListingResponse.ImageInfo imageInfo = new HousingListingResponse.ImageInfo();
                        imageInfo.setId(image.getId());
                        imageInfo.setS3Key(image.getS3Key());
                        imageInfo.setIsPrimary(image.getIsPrimary());
                        imageInfo.setDisplayOrder(image.getDisplayOrder());

                        try {
                            String signedUrl = s3Service.getSignedImageUrl(image.getS3Key());
                            imageInfo.setImageUrl(signedUrl);
                        } catch (Exception e) {
                            log.error("Error generating signed URL for S3 key: " + image.getS3Key(), e);
                            imageInfo.setImageUrl(null);
                        }
                        return imageInfo;
                    })
                    .collect(Collectors.toList());
            response.setImages(imageInfos);
        }

        return response;
    }

    private HousingListingSummaryResponse convertToSummaryResponse(HousingListing listing) {
        HousingListingSummaryResponse response = new HousingListingSummaryResponse();
        response.setId(listing.getId());
        response.setTitle(listing.getTitle());
        response.setPrice(listing.getPrice());
        response.setCity(listing.getCity());
        response.setBedrooms(listing.getBedrooms());
        response.setBathrooms(listing.getBathrooms());
        response.setAvailableFrom(listing.getAvailableFrom());
        response.setAvailableTo(listing.getAvailableTo());
        response.setCreatedAt(listing.getCreatedAt());
        response.setOwnerEmail(listing.getOwnerEmail());

        // Set primary image
        if (listing.getImages() != null && !listing.getImages().isEmpty()) {
            Optional<ListingImage> primaryImage = listing.getImages().stream()
                    .filter(ListingImage::getIsPrimary)
                    .findFirst();

            if (primaryImage.isEmpty()) {
                primaryImage = listing.getImages().stream().findFirst();
            }

            if (primaryImage.isPresent()) {
                try {
                    response.setPrimaryImageUrl(s3Service.getSignedImageUrl(primaryImage.get().getS3Key()));
                } catch (Exception e) {
                    log.error("Error generating signed URL", e);
                    response.setPrimaryImageUrl(null);
                }
            }
        }

        return response;
    }
}
