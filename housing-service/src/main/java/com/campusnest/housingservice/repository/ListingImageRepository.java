package com.campusnest.housingservice.repository;

import com.campusnest.housingservice.models.HousingListing;
import com.campusnest.housingservice.models.ListingImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListingImageRepository extends JpaRepository<ListingImage, Long> {
    List<ListingImage> findByListingOrderByDisplayOrder(HousingListing listing);
    
    Optional<ListingImage> findByListingAndIsPrimaryTrue(HousingListing listing);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM ListingImage li WHERE li.listing = :listing")
    void deleteByListing(@Param("listing") HousingListing listing);
    
    long countByListing(HousingListing listing);
    
    List<ListingImage> findByListingIdOrderByDisplayOrder(Long listingId);
}