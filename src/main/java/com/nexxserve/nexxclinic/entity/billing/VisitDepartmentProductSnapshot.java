package com.nexxserve.nexxclinic.entity.billing;

import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visit_department_product_snapshots")
public class VisitDepartmentProductSnapshot {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billing_version_id", nullable = false)
    private VisitBillingVersion billingVersion;

    /**
     * Original VisitDepartmentProduct id at the time of snapshot.
     * We store it as a UUID (not FK) to keep historical versions immutable
     * even if the live row is removed.
     */
    @Column(nullable = false)
    private UUID visitDepartmentProductId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_patient_insurance_id")
    private PatientInsurance appliedPatientInsurance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_worker_id")
    private Worker addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_by_worker_id")
    private Worker billedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VisitBillingVersion getBillingVersion() {
        return billingVersion;
    }

    public void setBillingVersion(VisitBillingVersion billingVersion) {
        this.billingVersion = billingVersion;
    }

    public UUID getVisitDepartmentProductId() {
        return visitDepartmentProductId;
    }

    public void setVisitDepartmentProductId(UUID visitDepartmentProductId) {
        this.visitDepartmentProductId = visitDepartmentProductId;
    }

    public VisitDepartment getVisitDepartment() {
        return visitDepartment;
    }

    public void setVisitDepartment(VisitDepartment visitDepartment) {
        this.visitDepartment = visitDepartment;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public VisitProductStatus getStatus() {
        return status;
    }

    public void setStatus(VisitProductStatus status) {
        this.status = status;
    }

    public PatientInsurance getAppliedPatientInsurance() {
        return appliedPatientInsurance;
    }

    public void setAppliedPatientInsurance(PatientInsurance appliedPatientInsurance) {
        this.appliedPatientInsurance = appliedPatientInsurance;
    }

    public Worker getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(Worker addedBy) {
        this.addedBy = addedBy;
    }

    public Worker getBilledBy() {
        return billedBy;
    }

    public void setBilledBy(Worker billedBy) {
        this.billedBy = billedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
