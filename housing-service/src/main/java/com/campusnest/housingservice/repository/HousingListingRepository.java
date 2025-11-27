package com.campusnest.housingservice.repository;

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

@Repository
public interface HousingListingRepository extends JpaRepository<HousingListing, Long> {

    // Find active listings
    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByIsActiveTrueOrderByCreatedAtDesc();

    // Find listings by owner email
    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.ownerEmail = :ownerEmail AND h.isActive = true " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByOwnerEmailAndIsActiveTrueOrderByCreatedAtDesc(@Param("ownerEmail") String ownerEmail);

    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.ownerEmail = :ownerEmail " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findByOwnerEmailOrderByCreatedAtDesc(@Param("ownerEmail") String ownerEmail);

    // Search with criteria
    @Query("SELECT h FROM HousingListing h " +
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
    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "AND (:city IS NULL OR h.city LIKE %:city%) " +
           "AND (:minPrice IS NULL OR h.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR h.price <= :maxPrice) " +
           "ORDER BY h.createdAt DESC")
    Page<HousingListing> findByCityAndPriceBetween(
        @Param("city") String city,
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice,
        Pageable pageable
    );

    // Optimized city search
    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "AND LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%')) " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findActiveByCityContainingIgnoreCase(@Param("city") String city);

    // Optimized price range search
    @Query("SELECT h FROM HousingListing h " +
           "WHERE h.isActive = true " +
           "AND h.price BETWEEN :minPrice AND :maxPrice " +
           "ORDER BY h.createdAt DESC")
    List<HousingListing> findActiveByPriceBetween(
        @Param("minPrice") BigDecimal minPrice,
        @Param("maxPrice") BigDecimal maxPrice
    );
}