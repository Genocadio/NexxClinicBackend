package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.VisitBillingStatus;
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

@Entity
@Table(name = "visit_billings")
public class VisitBilling {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_by_worker_id")
    private Worker billedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VisitBillingStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal insuranceCoveredAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal patientPayableAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal paidAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(nullable = false)
    private boolean fullyBilledVisit;

    @Column
    private LocalDateTime billingDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = VisitBillingStatus.UNPAID;
        }
        if (this.totalAmount == null || this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null || this.insuranceCoveredAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null || this.patientPayableAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
        if (this.paidAmount == null || this.paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.outstandingAmount == null || this.outstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.outstandingAmount = BigDecimal.ZERO;
        }
        if (this.billingDate == null && this.visit != null) {
            this.billingDate = this.visit.getVisitDate();
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VisitBillingStatus.UNPAID;
        }
        if (this.totalAmount == null || this.totalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null || this.insuranceCoveredAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null || this.patientPayableAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
        if (this.paidAmount == null || this.paidAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.outstandingAmount == null || this.outstandingAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.outstandingAmount = BigDecimal.ZERO;
        }
        if (this.billingDate == null && this.visit != null) {
            this.billingDate = this.visit.getVisitDate();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Visit getVisit() {
        return visit;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    public Worker getBilledBy() {
        return billedBy;
    }

    public void setBilledBy(Worker billedBy) {
        this.billedBy = billedBy;
    }

    public VisitBillingStatus getStatus() {
        return status;
    }

    public void setStatus(VisitBillingStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getInsuranceCoveredAmount() {
        return insuranceCoveredAmount;
    }

    public void setInsuranceCoveredAmount(BigDecimal insuranceCoveredAmount) {
        this.insuranceCoveredAmount = insuranceCoveredAmount;
    }

    public BigDecimal getPatientPayableAmount() {
        return patientPayableAmount;
    }

    public void setPatientPayableAmount(BigDecimal patientPayableAmount) {
        this.patientPayableAmount = patientPayableAmount;
    }

    public BigDecimal getPaidAmount() {
        return paidAmount;
    }

    public void setPaidAmount(BigDecimal paidAmount) {
        this.paidAmount = paidAmount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public boolean isFullyBilledVisit() {
        return fullyBilledVisit;
    }

    public void setFullyBilledVisit(boolean fullyBilledVisit) {
        this.fullyBilledVisit = fullyBilledVisit;
    }

    public LocalDateTime getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDateTime billingDate) {
        this.billingDate = billingDate;
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
