package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.persistence.Column;
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
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A patient share percentage tier for an insurance provider.
 * Each coverage defines a percentage the patient pays (0-100), optionally
 * scoped to a department and/or encounter type.
 *
 * <ul>
 *   <li>{@code department = null, encounterType = null} — <b>base</b> coverage (required, one per provider)</li>
 *   <li>{@code department = D, encounterType = null} — department-level conditional</li>
 *   <li>{@code department = null, encounterType = E} — encounter-type-level conditional</li>
 *   <li>{@code department = D, encounterType = E} — exact match conditional</li>
 * </ul>
 *
 * Resolution order (most specific wins):
 * <ol>
 *   <li>Per-line override (from billing input)</li>
 *   <li>Exact: provider + department + encounterType</li>
 *   <li>Department: provider + department</li>
 *   <li>Encounter type: provider + encounterType</li>
 *   <li>Base: provider-wide (no conditions)</li>
 *   <li>Patient-specific default (PatientInsurance.patientSharePercentage)</li>
 * </ol>
 */
@Entity
@Table(
    name = "insurance_coverage",
    indexes = {
        @Index(name = "idx_coverage_provider", columnList = "insurance_provider_id"),
        @Index(name = "idx_coverage_dept", columnList = "department_id")
    }
)
public class InsuranceCoverage {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_provider_id", nullable = false)
    private InsuranceProvider insuranceProvider;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_type", length = 32)
    private EncounterType encounterType;

    @Column(name = "patient_share_percentage", nullable = false)
    private Integer patientSharePercentage;

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

    public InsuranceProvider getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(InsuranceProvider insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public EncounterType getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(EncounterType encounterType) {
        this.encounterType = encounterType;
    }

    public Integer getPatientSharePercentage() {
        return patientSharePercentage;
    }

    public void setPatientSharePercentage(Integer patientSharePercentage) {
        this.patientSharePercentage = patientSharePercentage;
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
