package com.campusnest.housingservice.services;

import com.campusnest.housingservice.config.BloomFilterConfig;
import com.campusnest.housingservice.models.HousingListing;
import com.campusnest.housingservice.repository.HousingListingRepository;
import com.campusnest.housingservice.repository.ListingImageRepository;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
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
import java.util.concurrent.TimeUnit;

@Service("housingListingService")
@Transactional
@Slf4j
public class HousingListingServiceImpl implements HousingListingService {

    @Autowired
    private HousingListingRepository housingListingRepository;

    @Autowired
    private ListingImageRepository listingImageRepository;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    @Autowired
    private CacheMetricsService cacheMetricsService;

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

        HousingListing savedListing = housingListingRepository.save(listing);

        // Add new listing ID to Bloom Filter (Cache Penetration Prevention)
        // This ensures future queries for this ID won't be blocked
        bloomFilterConfig.addId(savedListing.getId());
        log.debug("Added new listing ID {} to Bloom Filter", savedListing.getId());

        return savedListing;
    }

    /**
     * Find housing listing by ID with FULL Cache Protection
     *
     * Three-Layer Defense Strategy:
     *
     * 1. BLOOM FILTER CHECK (Cache Penetration Prevention 缓存穿透):
     *    - Blocks 99% of queries for non-existent IDs BEFORE hitting cache/DB
     *    - If Bloom Filter says "ID doesn't exist" → return empty immediately
     *    - Cost: <1 microsecond memory lookup
     *
     * 2. CACHE CHECK (handled by Spring @Cacheable):
     *    - If in cache → return immediately (1-2ms)
     *    - If not in cache → proceed to step 3
     *
     * 3. DATABASE QUERY with Distributed Lock (Cache Breakdown Prevention 缓存击穿):
     *    - Spring's @Cacheable handles basic caching
     *    - For hot keys, use findByIdWithLock() for distributed lock protection
     *
     * Attack Scenario Blocked:
     * Attacker: "Give me IDs 999999, 999998, 999997... " (1000 fake IDs)
     * Bloom Filter: "Not in set" → 0 DB queries ✅
     *
     * Normal Flow:
     * User: "Give me listing #1"
     * Bloom Filter: "Might exist" → Check cache → Cache HIT → Return instantly
     */
    @Override
    @Cacheable(value = "housing-listings", key = "#id")
    @Transactional(readOnly = true)
    public Optional<HousingListing> findById(Long id) {
        // STEP 1: Bloom Filter Check (Cache Penetration Prevention)
        // Block non-existent IDs BEFORE hitting cache or database
        if (!bloomFilterConfig.mightContain(id)) {
            log.info("Bloom Filter blocked query for non-existent listing ID: {}", id);
            cacheMetricsService.recordBloomFilterBlock(); // Track Bloom Filter block
            return Optional.empty();  // ID doesn't exist
        }

        // Bloom Filter says ID might exist
        cacheMetricsService.recordBloomFilterHit();

        // STEP 2: Track cache hit/miss
        // Check if value is in cache
        org.springframework.cache.Cache cache = cacheManager.getCache("housing-listings");
        boolean isInCache = false;
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper cached = cache.get(id);
            if (cached != null) {
                isInCache = true;
            }
        }

        if (isInCache) {
            log.debug("Cache HIT for listing {}", id);
            cacheMetricsService.recordCacheHit();
        } else {
            log.debug("Cache MISS for listing {}", id);
            cacheMetricsService.recordCacheMiss();
        }

        // STEP 3: Query database with images eagerly loaded (will be cached by @Cacheable if not already cached)
        return housingListingRepository.findByIdWithImages(id);
    }

    /**
     * Alternative implementation with explicit distributed lock
     * Use this for hot keys that need extra protection against cache breakdown
     */
    @Transactional(readOnly = true)
    public Optional<HousingListing> findByIdWithLock(Long id) {
        String cacheKey = "housing-listings::" + id;
        String lockKey = "lock:housing-listing:" + id;

        // Try to get from cache first (manual check)
        org.springframework.cache.Cache cache = cacheManager.getCache("housing-listings");
        if (cache != null) {
            org.springframework.cache.Cache.ValueWrapper cached = cache.get(id);
            if (cached != null) {
                log.debug("Cache HIT for listing {}", id);
                return Optional.ofNullable((HousingListing) cached.get());
            }
        }

        log.debug("Cache MISS for listing {}, acquiring distributed lock", id);

        // Cache miss - acquire distributed lock
        RLock lock = redissonClient.getLock(lockKey);

        try {
            // Try to acquire lock (wait max 5 seconds, auto-release after 10 seconds)
            boolean locked = lock.tryLock(5, 10, TimeUnit.SECONDS);

            if (locked) {
                try {
                    // Double-check cache (another thread might have populated it while we waited)
                    if (cache != null) {
                        org.springframework.cache.Cache.ValueWrapper cached = cache.get(id);
                        if (cached != null) {
                            log.debug("Cache populated by another thread for listing {}", id);
                            return Optional.ofNullable((HousingListing) cached.get());
                        }
                    }

                    // Still not in cache - query database with images
                    log.info("Lock acquired, querying database for listing {}", id);
                    Optional<HousingListing> result = housingListingRepository.findByIdWithImages(id);

                    // Cache the result (even if empty, to prevent repeated queries)
                    if (cache != null && result.isPresent()) {
                        cache.put(id, result.get());
                        log.debug("Cached listing {}", id);
                    }

                    return result;

                } finally {
                    lock.unlock();
                    log.debug("Lock released for listing {}", id);
                }
            } else {
                // Couldn't acquire lock within 5 seconds
                // Another thread is probably loading it, wait a bit then check cache
                log.warn("Could not acquire lock for listing {} within 5 seconds, waiting for cache", id);
                Thread.sleep(500);  // Wait 500ms

                // Check cache one more time
                if (cache != null) {
                    org.springframework.cache.Cache.ValueWrapper cached = cache.get(id);
                    if (cached != null) {
                        return Optional.ofNullable((HousingListing) cached.get());
                    }
                }

                // Last resort - query database without lock (with images)
                log.warn("Fallback: querying database without lock for listing {}", id);
                return housingListingRepository.findByIdWithImages(id);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for lock on listing {}", id, e);
            // Fallback to database query with images
            return housingListingRepository.findByIdWithImages(id);
        }
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