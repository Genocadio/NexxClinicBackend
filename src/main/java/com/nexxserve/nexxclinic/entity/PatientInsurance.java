package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "patient_insurances")
public class PatientInsurance {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insurance_provider_id", nullable = false)
    private InsuranceProvider insuranceProvider;

    @Column(nullable = false)
    private String insuranceCardNumber;

    private String providingCompanyOrEmployer;

    @Column(nullable = false)
    private boolean principalMember;

    private String principalMemberName;
    private String principalMemberPhoneNumber;

    @Column(nullable = false)
    private LocalDate validFrom;

    @Column(nullable = false)
    private LocalDate validUntil;

    @Column(nullable = false)
    private boolean deactivated;

    @Column(name = "patient_share_percentage")
    private Integer patientSharePercentage;

    /**
     * Reference to an InsuranceCoverage record whose patientSharePercentage
     * defines the patient's share for this insurance. Replaces the free-form
     * integer — the backend resolves the actual percentage from the linked
     * record to prevent tampering.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_share_coverage_id")
    private InsuranceCoverage patientShareCoverage;

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

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public InsuranceProvider getInsuranceProvider() {
        return insuranceProvider;
    }

    public void setInsuranceProvider(InsuranceProvider insuranceProvider) {
        this.insuranceProvider = insuranceProvider;
    }

    public String getInsuranceCardNumber() {
        return insuranceCardNumber;
    }

    public void setInsuranceCardNumber(String insuranceCardNumber) {
        this.insuranceCardNumber = insuranceCardNumber;
    }

    public String getProvidingCompanyOrEmployer() {
        return providingCompanyOrEmployer;
    }

    public void setProvidingCompanyOrEmployer(String providingCompanyOrEmployer) {
        this.providingCompanyOrEmployer = providingCompanyOrEmployer;
    }

    public boolean isPrincipalMember() {
        return principalMember;
    }

    public void setPrincipalMember(boolean principalMember) {
        this.principalMember = principalMember;
    }

    public String getPrincipalMemberName() {
        return principalMemberName;
    }

    public void setPrincipalMemberName(String principalMemberName) {
        this.principalMemberName = principalMemberName;
    }

    public String getPrincipalMemberPhoneNumber() {
        return principalMemberPhoneNumber;
    }

    public void setPrincipalMemberPhoneNumber(String principalMemberPhoneNumber) {
        this.principalMemberPhoneNumber = principalMemberPhoneNumber;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(boolean deactivated) {
        this.deactivated = deactivated;
    }

    public Integer getPatientSharePercentage() {
        return patientSharePercentage;
    }

    public void setPatientSharePercentage(Integer patientSharePercentage) {
        this.patientSharePercentage = patientSharePercentage;
    }

    public InsuranceCoverage getPatientShareCoverage() {
        return patientShareCoverage;
    }

    public void setPatientShareCoverage(InsuranceCoverage patientShareCoverage) {
        this.patientShareCoverage = patientShareCoverage;
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
