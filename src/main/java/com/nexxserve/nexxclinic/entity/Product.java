package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "products",
        indexes = {
                @Index(name = "idx_products_name", columnList = "name"),
                @Index(name = "idx_products_type", columnList = "type"),
                @Index(name = "idx_products_code", columnList = "code")
        }
)
public class Product {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String genericName;

    @Column(nullable = false, unique = true, length = 64)
    private String code;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ProductUnit unit;

    @Lob
    @Column(nullable = false)
    private String metadata;

    @Column(precision = 19, scale = 2)
    private BigDecimal privateRhicPrice;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductInsuranceCoverage> insuranceCoverages;

    @Column(precision = 19, scale = 2)
    private BigDecimal clinicPrice;

    @Column(nullable = false)
    private boolean notPaid;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.metadata == null || this.metadata.isBlank()) {
            this.metadata = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.metadata == null || this.metadata.isBlank()) {
            this.metadata = "{}";
        }
    }


    // getter & setter
    public List<ProductInsuranceCoverage> getInsuranceCoverages() {
        return insuranceCoverages;
    }

    public void setInsuranceCoverages(List<ProductInsuranceCoverage> insuranceCoverages) {
        this.insuranceCoverages = insuranceCoverages;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ProductType getType() {
        return type;
    }

    public void setType(ProductType type) {
        this.type = type;
    }

    public ProductUnit getUnit() {
        return unit;
    }

    public void setUnit(ProductUnit unit) {
        this.unit = unit;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public BigDecimal getPrivateRhicPrice() {
        return privateRhicPrice;
    }

    public void setPrivateRhicPrice(BigDecimal privateRhicPrice) {
        this.privateRhicPrice = privateRhicPrice;
    }

    public BigDecimal getClinicPrice() {
        return clinicPrice;
    }

    public void setClinicPrice(BigDecimal clinicPrice) {
        this.clinicPrice = clinicPrice;
    }

    public boolean isNotPaid() {
        return notPaid;
    }

    public void setNotPaid(boolean notPaid) {
        this.notPaid = notPaid;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
