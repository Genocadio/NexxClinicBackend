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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visit_billing_items")
public class VisitBillingItem {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_insurance_billing_id", nullable = true)
    private DepartmentInsuranceBilling departmentInsuranceBilling;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_product_id", nullable = false)
    private VisitDepartmentProduct visitDepartmentProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_version_id")
    private com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion;

    @Column
    private UUID visitDepartmentProductSnapshotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_patient_insurance_id")
    private PatientInsurance appliedPatientInsurance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantitySnapshot;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal lineTotal;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal insuranceCoveredAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal patientPayableAmount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.unitPriceSnapshot == null || this.unitPriceSnapshot.compareTo(BigDecimal.ZERO) < 0) {
            this.unitPriceSnapshot = BigDecimal.ZERO;
        }
        if (this.quantitySnapshot == null || this.quantitySnapshot.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantitySnapshot = BigDecimal.ONE;
        }
        if (this.lineTotal == null || this.lineTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.lineTotal = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null || this.insuranceCoveredAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null || this.patientPayableAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.unitPriceSnapshot == null || this.unitPriceSnapshot.compareTo(BigDecimal.ZERO) < 0) {
            this.unitPriceSnapshot = BigDecimal.ZERO;
        }
        if (this.quantitySnapshot == null || this.quantitySnapshot.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantitySnapshot = BigDecimal.ONE;
        }
        if (this.lineTotal == null || this.lineTotal.compareTo(BigDecimal.ZERO) < 0) {
            this.lineTotal = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null || this.insuranceCoveredAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null || this.patientPayableAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DepartmentInsuranceBilling getDepartmentInsuranceBilling() {
        return departmentInsuranceBilling;
    }

    public void setDepartmentInsuranceBilling(DepartmentInsuranceBilling departmentInsuranceBilling) {
        this.departmentInsuranceBilling = departmentInsuranceBilling;
    }

    public VisitDepartmentProduct getVisitDepartmentProduct() {
        return visitDepartmentProduct;
    }

    public void setVisitDepartmentProduct(VisitDepartmentProduct visitDepartmentProduct) {
        this.visitDepartmentProduct = visitDepartmentProduct;
    }

    public com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion getBillingVersion() {
        return billingVersion;
    }

    public void setBillingVersion(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion) {
        this.billingVersion = billingVersion;
    }

    public UUID getVisitDepartmentProductSnapshotId() {
        return visitDepartmentProductSnapshotId;
    }

    public void setVisitDepartmentProductSnapshotId(UUID visitDepartmentProductSnapshotId) {
        this.visitDepartmentProductSnapshotId = visitDepartmentProductSnapshotId;
    }

    public PatientInsurance getAppliedPatientInsurance() {
        return appliedPatientInsurance;
    }

    public void setAppliedPatientInsurance(PatientInsurance appliedPatientInsurance) {
        this.appliedPatientInsurance = appliedPatientInsurance;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public BigDecimal getQuantitySnapshot() {
        return quantitySnapshot;
    }

    public void setQuantitySnapshot(BigDecimal quantitySnapshot) {
        this.quantitySnapshot = quantitySnapshot;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
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
