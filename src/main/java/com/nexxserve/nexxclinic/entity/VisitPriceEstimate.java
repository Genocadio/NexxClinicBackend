package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.PatientShareSource;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Per-product-line pre-billing price estimate.
 * <p>
 * Created automatically whenever products or insurances change on a visit,
 * giving a live price preview before a biller touches the visit. One row
 * per active (non-deleted) {@link VisitDepartmentProduct}.
 * <p>
 * Deleted when billing is created ({@code billVisit}) — these are estimates
 * only, not financial records.
 */
@Entity
@Table(
    name = "visit_price_estimates",
    indexes = {
        @Index(name = "idx_visit_price_estimates_visit", columnList = "visit_id")
    }
)
public class VisitPriceEstimate {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_product_id", nullable = false)
    private VisitDepartmentProduct visitDepartmentProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_patient_insurance_id")
    private PatientInsurance appliedPatientInsurance;

    @Column(name = "resolved_patient_share_pct", nullable = false)
    private Integer resolvedPatientSharePct;

    @Enumerated(EnumType.STRING)
    @Column(name = "patient_share_source", length = 20)
    private PatientShareSource patientShareSource;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

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
        if (this.resolvedPatientSharePct == null) {
            this.resolvedPatientSharePct = 0;
        }
        if (this.unitPrice == null) this.unitPrice = BigDecimal.ZERO;
        if (this.quantity == null) this.quantity = BigDecimal.ONE;
        if (this.lineTotal == null) this.lineTotal = BigDecimal.ZERO;
        if (this.insuranceCoveredAmount == null) this.insuranceCoveredAmount = BigDecimal.ZERO;
        if (this.patientPayableAmount == null) this.patientPayableAmount = BigDecimal.ZERO;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.resolvedPatientSharePct == null) {
            this.resolvedPatientSharePct = 0;
        }
        if (this.unitPrice == null) this.unitPrice = BigDecimal.ZERO;
        if (this.quantity == null) this.quantity = BigDecimal.ONE;
        if (this.lineTotal == null) this.lineTotal = BigDecimal.ZERO;
        if (this.insuranceCoveredAmount == null) this.insuranceCoveredAmount = BigDecimal.ZERO;
        if (this.patientPayableAmount == null) this.patientPayableAmount = BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public Visit getVisit() { return visit; }
    public void setVisit(Visit visit) { this.visit = visit; }

    public VisitDepartmentProduct getVisitDepartmentProduct() { return visitDepartmentProduct; }
    public void setVisitDepartmentProduct(VisitDepartmentProduct visitDepartmentProduct) { this.visitDepartmentProduct = visitDepartmentProduct; }

    public PatientInsurance getAppliedPatientInsurance() { return appliedPatientInsurance; }
    public void setAppliedPatientInsurance(PatientInsurance appliedPatientInsurance) { this.appliedPatientInsurance = appliedPatientInsurance; }

    public Integer getResolvedPatientSharePct() { return resolvedPatientSharePct; }
    public void setResolvedPatientSharePct(Integer resolvedPatientSharePct) { this.resolvedPatientSharePct = resolvedPatientSharePct; }

    public PatientShareSource getPatientShareSource() { return patientShareSource; }
    public void setPatientShareSource(PatientShareSource patientShareSource) { this.patientShareSource = patientShareSource; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getLineTotal() { return lineTotal; }
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }

    public BigDecimal getInsuranceCoveredAmount() { return insuranceCoveredAmount; }
    public void setInsuranceCoveredAmount(BigDecimal insuranceCoveredAmount) { this.insuranceCoveredAmount = insuranceCoveredAmount; }

    public BigDecimal getPatientPayableAmount() { return patientPayableAmount; }
    public void setPatientPayableAmount(BigDecimal patientPayableAmount) { this.patientPayableAmount = patientPayableAmount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
