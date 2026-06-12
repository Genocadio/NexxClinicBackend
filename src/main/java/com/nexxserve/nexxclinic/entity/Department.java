package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "departments")
public class Department {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DepartmentInsurancePolicyMode insurancePolicyMode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean nursing = false;

    @Column(nullable = false)
    private boolean supportRequests = false;

    @Column(columnDefinition = "boolean default true")
    private Boolean requestsProducts = true;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.insurancePolicyMode == null) {
            this.insurancePolicyMode = DepartmentInsurancePolicyMode.ALL;
        }
        if (this.requestsProducts == null) {
            this.requestsProducts = true;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.insurancePolicyMode == null) {
            this.insurancePolicyMode = DepartmentInsurancePolicyMode.ALL;
        }
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

    public DepartmentInsurancePolicyMode getInsurancePolicyMode() {
        return insurancePolicyMode;
    }

    public void setInsurancePolicyMode(DepartmentInsurancePolicyMode insurancePolicyMode) {
        this.insurancePolicyMode = insurancePolicyMode;
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

    public boolean isNursing() {
        return nursing;
    }

    public void setNursing(boolean nursing) {
        this.nursing = nursing;
    }

    public boolean isSupportRequests() {
        return supportRequests;
    }

    public void setSupportRequests(boolean supportRequests) {
        this.supportRequests = supportRequests;
    }

    public boolean isRequestsProducts() {
        return requestsProducts == null || requestsProducts;
    }

    public void setRequestsProducts(Boolean requestsProducts) {
        this.requestsProducts = requestsProducts;
    }
}
