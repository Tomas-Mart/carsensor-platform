package com.carsensor.car.domain.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cars", indexes = {
        @Index(name = "idx_cars_brand_model", columnList = "brand, model"),
        @Index(name = "idx_cars_year", columnList = "year"),
        @Index(name = "idx_cars_price", columnList = "price"),
        @Index(name = "idx_cars_parsed_at", columnList = "parsed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Car {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(nullable = false)
    private Integer year;

    @Column(nullable = false)
    private Integer mileage;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(length = 1000)
    private String description;

    @Column(name = "original_brand", length = 100)
    private String originalBrand;

    @Column(name = "original_model", length = 100)
    private String originalModel;

    @Column(name = "exterior_color", length = 50)
    private String exteriorColor;

    @Column(name = "interior_color", length = 50)
    private String interiorColor;

    @Column(name = "engine_capacity", length = 20)
    private String engineCapacity;

    @Column(name = "transmission", length = 30)
    private String transmission;

    @Column(name = "drive_type", length = 30)
    private String driveType;

    @Column(name = "photo_urls", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private List<String> photoUrls = new ArrayList<>();

    @Column(name = "main_photo_url")
    private String mainPhotoUrl;

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "external_id", length = 100)
    private String externalId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    @Builder.Default
    private Long version = 0L;
}