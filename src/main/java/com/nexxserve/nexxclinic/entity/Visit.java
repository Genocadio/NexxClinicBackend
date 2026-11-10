package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.VisitStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "visits")
public class Visit {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false)
    private LocalDateTime visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Pre-billing estimated total (sum of all product line totals).
     * Nullable: only set when at least one product exists on the visit.
     */
    @Column(name = "estimated_total", precision = 19, scale = 2)
    private BigDecimal estimatedTotal;

    /**
     * Pre-billing estimated insurance-covered amount (sum of insurance shares).
     * Nullable: only meaningful when at least one product is covered by insurance.
     */
    @Column(name = "estimated_insurance_pay", precision = 19, scale = 2)
    private BigDecimal estimatedInsurancePay;

    /**
     * Pre-billing estimated patient-payable amount (sum of patient shares).
     * Nullable: only set when at least one product exists on the visit.
     */
    @Column(name = "estimated_patient_pay", precision = 19, scale = 2)
    private BigDecimal estimatedPatientPay;

    @Column(name = "current_locations", columnDefinition = "TEXT")
    private String currentLocations;

    @Column(name = "active_episode_count")
    private Integer activeEpisodeCount = 0;

    @Column(name = "is_discharged")
    private Boolean isDischarged = false;

    @Column(name = "has_returned_from_billing")
    private Boolean hasReturnedFromBilling = false;

    @Column(name = "last_return_reason", columnDefinition = "TEXT")
    private String lastReturnReason;

    @Column(name = "has_pending_results")
    private Boolean hasPendingResults = false;

    @Column(name = "last_flow_activity_at")
    private LocalDateTime lastFlowActivityAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = VisitStatus.CREATED;
        }
        if (this.visitDate == null) {
            this.visitDate = now;
        }
        if (this.isDischarged == null) {
            this.isDischarged = false;
        }
        if (this.lastFlowActivityAt == null) {
            this.lastFlowActivityAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VisitStatus.CREATED;
        }
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

    public LocalDateTime getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDateTime visitDate) {
        this.visitDate = visitDate;
    }

    public VisitStatus getStatus() {
        return status;
    }

    public void setStatus(VisitStatus status) {
        this.status = status;
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

    public BigDecimal getEstimatedTotal() {
        return estimatedTotal;
    }

    public void setEstimatedTotal(BigDecimal estimatedTotal) {
        this.estimatedTotal = estimatedTotal;
    }

    public BigDecimal getEstimatedInsurancePay() {
        return estimatedInsurancePay;
    }

    public void setEstimatedInsurancePay(BigDecimal estimatedInsurancePay) {
        this.estimatedInsurancePay = estimatedInsurancePay;
    }

    public BigDecimal getEstimatedPatientPay() {
        return estimatedPatientPay;
    }

    public void setEstimatedPatientPay(BigDecimal estimatedPatientPay) {
        this.estimatedPatientPay = estimatedPatientPay;
    }

    public String getCurrentLocations() {
        return currentLocations;
    }

    public void setCurrentLocations(String currentLocations) {
        this.currentLocations = currentLocations;
    }

    public Integer getActiveEpisodeCount() {
        return activeEpisodeCount != null ? activeEpisodeCount : 0;
    }

    public void setActiveEpisodeCount(Integer activeEpisodeCount) {
        this.activeEpisodeCount = activeEpisodeCount;
    }

    public Boolean getIsDischarged() {
        return isDischarged != null && isDischarged;
    }

    public void setIsDischarged(Boolean isDischarged) {
        this.isDischarged = isDischarged;
    }

    public Boolean getHasReturnedFromBilling() {
        return hasReturnedFromBilling != null && hasReturnedFromBilling;
    }

    public void setHasReturnedFromBilling(Boolean hasReturnedFromBilling) {
        this.hasReturnedFromBilling = hasReturnedFromBilling;
    }

    public String getLastReturnReason() {
        return lastReturnReason;
    }

    public void setLastReturnReason(String lastReturnReason) {
        this.lastReturnReason = lastReturnReason;
    }

    public Boolean getHasPendingResults() {
        return hasPendingResults != null && hasPendingResults;
    }

    public void setHasPendingResults(Boolean hasPendingResults) {
        this.hasPendingResults = hasPendingResults;
    }

    public LocalDateTime getLastFlowActivityAt() {
        return lastFlowActivityAt;
    }

    public void setLastFlowActivityAt(LocalDateTime lastFlowActivityAt) {
        this.lastFlowActivityAt = lastFlowActivityAt;
    }
}
