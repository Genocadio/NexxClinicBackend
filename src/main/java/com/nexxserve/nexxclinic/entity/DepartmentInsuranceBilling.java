package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "visit_billings")
public class DepartmentInsuranceBilling {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "visit_department_billing_id", nullable = true)
    private VisitDepartmentBilling visitDepartmentBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_version_id")
    private com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_insurance_id")
    private PatientInsurance patientInsurance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
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

    /**
     * Classifies the unpaid outstanding portion:
     * LOAN = patient still owes, GIVEAWAY = clinic absorbs.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "outstanding_type", length = 16)
    private com.nexxserve.nexxclinic.model.OutstandingType outstandingType;

    @Column(name = "outstanding_reason", length = 500)
    private String outstandingReason;

    @Column
    private String invoiceUrl;

    /**
     * The date/time shown on the invoice. Defaults to the record creation time
     * but can be overridden by an admin or manager. When changed, the existing
     * invoice PDF is invalidated so a fresh one is generated on next access.
     */
    private LocalDateTime billingDate;

    @OneToMany(mappedBy = "departmentInsuranceBilling", cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    private List<VisitBillingItem> items = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.billingDate == null) {
            this.billingDate = now;
        }
        if (this.status == null) {
            this.status = VisitBillingStatus.UNPAID;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.outstandingAmount == null) {
            this.outstandingAmount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VisitBillingStatus.UNPAID;
        }
        if (this.totalAmount == null) {
            this.totalAmount = BigDecimal.ZERO;
        }
        if (this.insuranceCoveredAmount == null) {
            this.insuranceCoveredAmount = BigDecimal.ZERO;
        }
        if (this.patientPayableAmount == null) {
            this.patientPayableAmount = BigDecimal.ZERO;
        }
        if (this.paidAmount == null) {
            this.paidAmount = BigDecimal.ZERO;
        }
        if (this.outstandingAmount == null) {
            this.outstandingAmount = BigDecimal.ZERO;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VisitDepartmentBilling getVisitDepartmentBilling() {
        return visitDepartmentBilling;
    }

    public void setVisitDepartmentBilling(VisitDepartmentBilling visitDepartmentBilling) {
        this.visitDepartmentBilling = visitDepartmentBilling;
    }

    public com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion getBillingVersion() {
        return billingVersion;
    }

    public void setBillingVersion(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion) {
        this.billingVersion = billingVersion;
    }

    public PatientInsurance getPatientInsurance() {
        return patientInsurance;
    }

    public void setPatientInsurance(PatientInsurance patientInsurance) {
        this.patientInsurance = patientInsurance;
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

    public String getInvoiceUrl() {
        return invoiceUrl;
    }

    public void setInvoiceUrl(String invoiceUrl) {
        this.invoiceUrl = invoiceUrl;
    }

    public com.nexxserve.nexxclinic.model.OutstandingType getOutstandingType() {
        return outstandingType;
    }

    public void setOutstandingType(com.nexxserve.nexxclinic.model.OutstandingType outstandingType) {
        this.outstandingType = outstandingType;
    }

    public String getOutstandingReason() {
        return outstandingReason;
    }

    public void setOutstandingReason(String outstandingReason) {
        this.outstandingReason = outstandingReason;
    }

    public LocalDateTime getBillingDate() {
        return billingDate;
    }

    public void setBillingDate(LocalDateTime billingDate) {
        this.billingDate = billingDate;
    }

    public List<VisitBillingItem> getItems() {
        return items;
    }

    public void setItems(List<VisitBillingItem> items) {
        this.items = items;
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
