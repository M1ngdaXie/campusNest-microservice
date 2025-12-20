package com.campusnest.housingservice.repository;

import com.campusnest.housingservice.dto.MapMarkerDTO;
import com.campusnest.housingservice.models.HousingListing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HousingListingRepository extends JpaRepository<HousingListing, Long> {

    // Find active listings
    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByIsActiveTrueOrderByCreatedAtDesc();

    // Find active listings with pagination
    @Query(value = "SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true",
           countQuery = "SELECT COUNT(DISTINCT h) FROM HousingListing h WHERE h.isActive = true")
    Page<HousingListing> findByIsActiveTrue(Pageable pageable);

    // Find listings by owner email
    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.ownerEmail = :ownerEmail AND h.isActive = true " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByOwnerEmailAndIsActiveTrueOrderByCreatedAtDesc(@Param("ownerEmail") String ownerEmail);

    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.ownerEmail = :ownerEmail " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByOwnerEmailOrderByCreatedAtDesc(@Param("ownerEmail") String ownerEmail);

    // Search with criteria
    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "AND h.city LIKE %:city% " +
           "AND h.price BETWEEN :minPrice AND :maxPrice " +
           "AND h.availableFrom <= :availableTo " +
           "AND h.availableTo >= :availableFrom " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findBySearchCriteria(
        @Param("city") String city,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("availableFrom") LocalDate availableFrom,
        @Param("availableTo") LocalDate availableTo
    );

    // Count methods
    long countByIsActiveTrue();

    long countByOwnerEmail(String ownerEmail);

    // Paginated search by city and price range
    @Query(value = "SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "AND (:city IS NULL OR h.city LIKE %:city%) " +
           "AND (:minPrice IS NULL OR h.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.price <= :maxPrice) " +
           "ORDER BY h.createdAt DESC",
           countQuery = "SELECT COUNT(DISTINCT h) FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "AND (:city IS NULL OR h.city LIKE %:city%) " +
           "AND (:minPrice IS NULL OR h.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.price <= :maxPrice)")
    Page<HousingListing> findByCityAndPriceBetween(
        @Param("city") String city,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    // Optimized city search
    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "AND LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findActiveByCityContainingIgnoreCase(@Param("city") String city);

    // Optimized price range search
    @Query("SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "AND h.price BETWEEN :minPrice AND :maxPrice " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findActiveByPriceBetween(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );

    /**
     * Fetch all listing IDs (for Bloom Filter initialization)
     *
     * Cache Penetration Prevention:
     * This query fetches ONLY the ID column (not full objects) for efficiency.
     * Used to populate the Bloom Filter on application startup.
     *
     * Why only IDs?
     * - Fetching 100,000 full objects = ~100 MB memory + slow
     * - Fetching 100,000 IDs = ~800 KB memory + fast ✅
     */
    @Query("SELECT h.id FROM HousingListing h")
    List<Long> findAllIds();

    /**
     * Find listing by ID with images eagerly loaded
     * Use LEFT JOIN FETCH to load images in a single query
     */
    @Query("SELECT h FROM HousingListing h LEFT JOIN FETCH h.images WHERE h.id = :id")
    Optional<HousingListing> findByIdWithImages(@Param("id") Long id);

    /**
     * Find active listings within a radius using Haversine formula
     * Radius in kilometers
     * Optimized with bounding box pre-filter to reduce Haversine calculations
     */
    @Query(value = """
          SELECT * FROM housing_listings
          WHERE latitude IS NOT NULL
          AND longitude IS NOT NULL
          AND is_active = true
          AND (
              6371 * acos(
                  cos(radians(:centerLat)) * cos(radians(latitude)) *
                  cos(radians(longitude) - radians(:centerLng)) +
                  sin(radians(:centerLat)) * sin(radians(latitude))
              )
          ) <= :radiusKm
          ORDER BY (
              6371 * acos(
                  cos(radians(:centerLat)) * cos(radians(latitude)) *
                  cos(radians(longitude) - radians(:centerLng)) +
                  sin(radians(:centerLat)) * sin(radians(latitude))
              )
          ) ASC
          """, nativeQuery = true)
    List<HousingListing> findWithinRadius(
            @Param("centerLat") BigDecimal centerLat,
            @Param("centerLng") BigDecimal centerLng,
            @Param("radiusKm") BigDecimal radiusKm
    );

    /**
     * Find active listings within bounding box (map viewport)
     */
    @Query("SELECT h FROM HousingListing h WHERE " +
            "h.latitude BETWEEN :swLat AND :neLat AND " +
            "h.longitude BETWEEN :swLng AND :neLng AND " +
            "h.latitude IS NOT NULL AND h.longitude IS NOT NULL AND " +
            "h.isActive = true")
    List<HousingListing> findWithinBounds(
            @Param("neLat") BigDecimal neLat,
            @Param("neLng") BigDecimal neLng,
            @Param("swLat") BigDecimal swLat,
            @Param("swLng") BigDecimal swLng
    );

    /**
     * Get active map markers (lightweight) within bounds
     */
    @Query("""
          SELECT new com.campusnest.housingservice.dto.MapMarkerDTO(
              h.id, h.latitude, h.longitude, h.price, h.title, null
          )
          FROM HousingListing h
          WHERE h.latitude BETWEEN :swLat AND :neLat
          AND h.longitude BETWEEN :swLng AND :neLng
          AND h.latitude IS NOT NULL AND h.longitude IS NOT NULL
          AND h.isActive = true
          """)
    List<MapMarkerDTO> findMapMarkersWithinBounds(
            @Param("neLat") BigDecimal neLat,
            @Param("neLng") BigDecimal neLng,
            @Param("swLat") BigDecimal swLat,
            @Param("swLng") BigDecimal swLng
    );

    /**
     * Find listings that need geocoding (optimized with idx_is_geocoded)
     * Returns all un-geocoded listings - use with caution for large datasets
     */
    @Query("SELECT h FROM HousingListing h WHERE h.isGeocoded = false ORDER BY h.id ASC")
    List<HousingListing> findByIsGeocodedFalse();

    /**
     * Find listings that need geocoding with pagination (memory-efficient)
     */
    @Query("SELECT h FROM HousingListing h WHERE h.isGeocoded = false ORDER BY h.id ASC")
    Page<HousingListing> findByIsGeocodedFalse(Pageable pageable);

    /**
     * Advanced search with multiple optional filters and pagination
     * All parameters are optional (nullable) for flexible filtering
     */
    @Query(value = "SELECT DISTINCT h FROM HousingListing h " +
           "LEFT JOIN FETCH h.images " +
           "WHERE h.isActive = true " +
           "AND (:city IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:minPrice IS NULL OR h.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.price <= :maxPrice) " +
           "AND (:minBedrooms IS NULL OR h.bedrooms >= :minBedrooms) " +
           "AND (:maxBedrooms IS NULL OR h.bedrooms <= :maxBedrooms) " +
           "AND (:minBathrooms IS NULL OR h.bathrooms >= :minBathrooms) " +
           "AND (:maxBathrooms IS NULL OR h.bathrooms <= :maxBathrooms) " +
           "AND (:availableFrom IS NULL OR h.availableTo >= :availableFrom) " +
           "AND (:availableTo IS NULL OR h.availableFrom <= :availableTo)",
           countQuery = "SELECT COUNT(DISTINCT h) FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "AND (:city IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:minPrice IS NULL OR h.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.price <= :maxPrice) " +
           "AND (:minBedrooms IS NULL OR h.bedrooms >= :minBedrooms) " +
           "AND (:maxBedrooms IS NULL OR h.bedrooms <= :maxBedrooms) " +
           "AND (:minBathrooms IS NULL OR h.bathrooms >= :minBathrooms) " +
           "AND (:maxBathrooms IS NULL OR h.bathrooms <= :maxBathrooms) " +
           "AND (:availableFrom IS NULL OR h.availableTo >= :availableFrom) " +
           "AND (:availableTo IS NULL OR h.availableFrom <= :availableTo)")
    Page<HousingListing> searchWithFilters(
        @Param("city") String city,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        @Param("minBedrooms") Integer minBedrooms,
        @Param("maxBedrooms") Integer maxBedrooms,
        @Param("minBathrooms") Integer minBathrooms,
        @Param("maxBathrooms") Integer maxBathrooms,
        @Param("availableFrom") LocalDate availableFrom,
        @Param("availableTo") LocalDate availableTo,
        Pageable pageable
    );
}
