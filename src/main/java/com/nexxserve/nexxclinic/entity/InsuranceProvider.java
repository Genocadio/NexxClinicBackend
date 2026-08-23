package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "insurance_providers")
public class InsuranceProvider {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, unique = true)
    private String insuranceName;

    @Column(unique = true)
    private String acronym;

    /**
     * Patient share coverages for this provider. At least one base coverage
     * (department=null, encounterType=null) is required — it defines the default
     * patient share percentage. Additional conditional coverages can target
     * specific departments or encounter types.
     */
    @OneToMany(mappedBy = "insuranceProvider", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InsuranceCoverage> coverages = new ArrayList<>();

    @Column(nullable = false)
    private boolean supportedByClinic;

    private String iconUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInsuranceName() {
        return insuranceName;
    }

    public void setInsuranceName(String insuranceName) {
        this.insuranceName = insuranceName;
    }

    public String getAcronym() {
        return acronym;
    }

    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public List<InsuranceCoverage> getCoverages() {
        return coverages;
    }

    public void setCoverages(List<InsuranceCoverage> coverages) {
        this.coverages = coverages;
    }

    public void addCoverage(InsuranceCoverage coverage) {
        coverages.add(coverage);
        coverage.setInsuranceProvider(this);
    }

    public void removeCoverage(InsuranceCoverage coverage) {
        coverages.remove(coverage);
        coverage.setInsuranceProvider(null);
    }

    /**
     * Returns the base patient share percentage (from the base coverage with no conditions).
     * This is equivalent to the old defaultPatientSharePercentage.
     * Returns null if no base coverage exists.
     */
    public Integer getBasePatientSharePercentage() {
        if (coverages == null) return null;
        return coverages.stream()
            .filter(c -> c.getDepartment() == null && c.getEncounterType() == null)
            .map(InsuranceCoverage::getPatientSharePercentage)
            .findFirst()
            .orElse(null);
    }

    public boolean isSupportedByClinic() {
        return supportedByClinic;
    }

    public void setSupportedByClinic(boolean supportedByClinic) {
        this.supportedByClinic = supportedByClinic;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
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
