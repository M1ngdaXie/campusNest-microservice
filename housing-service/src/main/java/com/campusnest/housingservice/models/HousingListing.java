package com.campusnest.housingservice.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "housing_listings", indexes = {
    @Index(name = "idx_city", columnList = "city"),
    @Index(name = "idx_price", columnList = "price"),
    @Index(name = "idx_is_active", columnList = "isActive"),
    @Index(name = "idx_created_at", columnList = "createdAt"),
    @Index(name = "idx_available_from", columnList = "availableFrom"),
    @Index(name = "idx_available_to", columnList = "availableTo"),
    @Index(name = "idx_city_price_active", columnList = "city, price, isActive"),
    @Index(name = "idx_owner_id", columnList = "ownerId")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HousingListing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private Integer bedrooms;

    @Column(nullable = false)
    private Integer bathrooms;

    @Column(nullable = false)
    private LocalDate availableFrom;

    @Column(nullable = false)
    private LocalDate availableTo;

    @Column(nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Store owner ID instead of full User entity to avoid cross-service dependency
    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    // Store owner email for queries and display (denormalized for microservice architecture)
    @Column(name = "owner_email", nullable = false)
    private String ownerEmail;

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"listing"})
    private List<ListingImage> images = new ArrayList<>();
}