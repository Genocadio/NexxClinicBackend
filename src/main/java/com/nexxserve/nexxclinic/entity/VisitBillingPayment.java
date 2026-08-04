package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.model.PaymentMethod;
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
@Table(name = "visit_billing_payments")
public class VisitBillingPayment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_billing_id", nullable = false)
    private VisitDepartmentBilling visitDepartmentBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_insurance_billing_id")
    private DepartmentInsuranceBilling departmentInsuranceBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_version_id")
    private com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PaymentMethod paymentMethod;

    @Column
    private String reference;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.amount == null) {
            this.amount = BigDecimal.ZERO;
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

    public DepartmentInsuranceBilling getDepartmentInsuranceBilling() {
        return departmentInsuranceBilling;
    }

    public void setDepartmentInsuranceBilling(DepartmentInsuranceBilling departmentInsuranceBilling) {
        this.departmentInsuranceBilling = departmentInsuranceBilling;
    }

    public com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion getBillingVersion() {
        return billingVersion;
    }

    public void setBillingVersion(com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion) {
        this.billingVersion = billingVersion;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
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
