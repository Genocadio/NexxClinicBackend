package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.DrugAdministrationFrequency;
import com.nexxserve.nexxclinic.model.MustPrescribedBy;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "product_insurance_coverages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_product_coverage_provider", columnNames = {"product_id", "insurance_provider_id"})
        },
        indexes = {
                @Index(name = "idx_product_coverage_product", columnList = "product_id"),
                @Index(name = "idx_product_coverage_insurance", columnList = "insurance_provider_id")
        }
)
public class ProductInsuranceCoverage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_provider_id", nullable = false)
    private InsuranceProvider insuranceProvider;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal cost;

    @Column(nullable = false)
    private boolean covered;

    @Column(nullable = false)
    private boolean notPaid;

    @Column(nullable = false)
    private boolean requireMedicalAdvisor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MustPrescribedBy mustPrescribedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DrugAdministrationFrequency drugAdministrationFrequency;

    @ElementCollection
    @CollectionTable(name = "product_coverage_authorization_reasons", joinColumns = @JoinColumn(name = "coverage_id"))
    @Column(name = "reason", nullable = false, length = 120)
    private List<String> authorizationRequestReasons = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.cost == null) {
            this.cost = BigDecimal.ZERO;
        }
        this.covered = this.cost.compareTo(BigDecimal.ZERO) > 0;
        if (this.mustPrescribedBy == null) {
            this.mustPrescribedBy = MustPrescribedBy.ALL;
        }
        if (this.drugAdministrationFrequency == null) {
            this.drugAdministrationFrequency = DrugAdministrationFrequency.CUSTOM_HOURS;
        }
        if (this.authorizationRequestReasons == null) {
            this.authorizationRequestReasons = new ArrayList<>();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.cost == null) {
            this.cost = BigDecimal.ZERO;
        }
        this.covered = this.cost.compareTo(BigDecimal.ZERO) > 0;
        if (this.mustPrescribedBy == null) {
            this.mustPrescribedBy = MustPrescribedBy.ALL;
        }
        if (this.drugAdministrationFrequency == null) {
            this.drugAdministrationFrequency = DrugAdministrationFrequency.CUSTOM_HOURS;
        }
        if (this.authorizationRequestReasons == null) {
            this.authorizationRequestReasons = new ArrayList<>();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public InsuranceProvider getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(InsuranceProvider insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public boolean isCovered() {
        return covered;
    }

    public void setCovered(boolean covered) {
        this.covered = covered;
    }

    public boolean isNotPaid() {
        return notPaid;
    }

    public void setNotPaid(boolean notPaid) {
        this.notPaid = notPaid;
    }

    public boolean isRequireMedicalAdvisor() {
        return requireMedicalAdvisor;
    }

    public void setRequireMedicalAdvisor(boolean requireMedicalAdvisor) {
        this.requireMedicalAdvisor = requireMedicalAdvisor;
    }

    public MustPrescribedBy getMustPrescribedBy() {
        return mustPrescribedBy;
    }

    public void setMustPrescribedBy(MustPrescribedBy mustPrescribedBy) {
        this.mustPrescribedBy = mustPrescribedBy;
    }

    public DrugAdministrationFrequency getDrugAdministrationFrequency() {
        return drugAdministrationFrequency;
    }

    public void setDrugAdministrationFrequency(DrugAdministrationFrequency drugAdministrationFrequency) {
        this.drugAdministrationFrequency = drugAdministrationFrequency;
    }

    public List<String> getAuthorizationRequestReasons() {
        return authorizationRequestReasons;
    }

    public void setAuthorizationRequestReasons(List<String> authorizationRequestReasons) {
        this.authorizationRequestReasons = authorizationRequestReasons;
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
