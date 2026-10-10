package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.BillingState;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
// N4 fix: the full (visit_department_id, product_id) unique constraint was removed.
// Uniqueness is now enforced by the PARTIAL unique index created in
// docs/migrations/visit_department_products_partial_unique_index.sql
// (uk_visit_department_product_active WHERE deleted = false), which lets a re-added
// product coexist with its own soft-deleted historical row.
@Table(name = "visit_department_products")
public class VisitDepartmentProduct {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    /**
     * Legacy billing status (PENDING/UNPAID/CORRECTION_PENDING/BILLED/EXEMPTED/
     * PATIENT_SHARE_EXEMPTED). Retained as a mirrored column so existing reports,
     * queries and the GraphQL {@code status} field keep working unchanged — but it
     * is no longer the source of truth: writes go through the setters below, which
     * keep this column consistent with {@link #billingState} + {@link #exemptionMode}.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VisitProductStatus status;

    /**
     * Billing lifecycle state — the source of truth for "was this billed?".
     * See {@link BillingState}: UNBILLED, BILLED or CORRECTING (under active
     * billing correction).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_state", nullable = false, length = 16)
    private BillingState billingState;

    /**
     * How the line was exempted — only meaningful when {@link #billingState}
     * is {@link BillingState#BILLED}. NONE = normal charge, FULL = line zeroed
     * (legacy EXEMPTED), PATIENT_SHARE = patient share waived but insurance still
     * covered (legacy PATIENT_SHARE_EXEMPTED).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "exemption_mode", nullable = false, length = 16)
    private ExemptionType exemptionMode;

    /**
     * How this product was added to the visit department:
     * {@link VisitDepartmentProductSource#USER} (manual) or
     * {@link VisitDepartmentProductSource#PROFILE} (added from an explicitly
     * applied department profile). Profile-sourced products cannot be removed
     * individually — change the visit department's profile instead.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 16)
    private VisitDepartmentProductSource source;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_worker_id")
    private Worker addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_by_worker_id")
    private Worker billedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by_worker_id")
    private Worker confirmedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_confirmation_status", nullable = false, length = 32)
    private com.nexxserve.nexxclinic.model.BillingConfirmationStatus billingConfirmationStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processor_worker_id")
    private Worker processor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.deleted = false;
        if (this.source == null) {
            this.source = VisitDepartmentProductSource.USER;
        }
        applyBillingDefaults();
        if (this.billingConfirmationStatus == null) {
            this.billingConfirmationStatus = com.nexxserve.nexxclinic.model.BillingConfirmationStatus.CONFIRMED;
        }
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = BigDecimal.ONE;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        applyBillingDefaults();
        if (this.billingConfirmationStatus == null) {
            this.billingConfirmationStatus = com.nexxserve.nexxclinic.model.BillingConfirmationStatus.CONFIRMED;
        }
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = BigDecimal.ONE;
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public VisitProductStatus getStatus() {
        return status;
    }

    /**
     * Sets the legacy status and re-derives {@link #billingState} and
     * {@link #exemptionMode} from it, so every existing writer
     * (persist stamping, correction flow, restore paths) automatically keeps the
     * new columns in sync:
     * <ul>
     *   <li>PENDING/UNPAID → UNBILLED + NONE</li>
     *   <li>CORRECTION_PENDING → CORRECTING, keeping the mode recorded when the
     *       line was billed (the correction transaction re-bills it fresh)</li>
     *   <li>BILLED → BILLED + NONE, EXEMPTED → BILLED + FULL,
     *       PATIENT_SHARE_EXEMPTED → BILLED + PATIENT_SHARE</li>
     * </ul>
     */
    public void setStatus(VisitProductStatus status) {
        this.status = status;
        this.billingState = billingStateOf(status);
        if (status == VisitProductStatus.CORRECTION_PENDING) {
            if (this.exemptionMode == null) {
                this.exemptionMode = ExemptionType.NONE;
            }
        } else {
            this.exemptionMode = exemptionModeOf(status);
        }
    }

    public BillingState getBillingState() {
        return billingState;
    }

    /** Sets the billing state and mirrors the legacy {@link #status} column. */
    public void setBillingState(BillingState billingState) {
        this.billingState = billingState;
        this.status = legacyStatusOf(billingState, this.exemptionMode);
    }

    public ExemptionType getExemptionMode() {
        return exemptionMode;
    }

    /** Sets the exemption mode and mirrors the legacy {@link #status} column. */
    public void setExemptionMode(ExemptionType exemptionMode) {
        this.exemptionMode = exemptionMode;
        this.status = legacyStatusOf(this.billingState, exemptionMode);
    }

    /** Fill any missing billing fields before persist (legacy rows or new entities). */
    private void applyBillingDefaults() {
        if (this.billingState == null) {
            this.billingState = billingStateOf(this.status);
        }
        if (this.exemptionMode == null) {
            this.exemptionMode = exemptionModeOf(this.status);
        }
        if (this.status == null) {
            this.status = legacyStatusOf(this.billingState, this.exemptionMode);
        }
    }

    private static BillingState billingStateOf(VisitProductStatus status) {
        if (status == null) {
            return BillingState.UNBILLED;
        }
        return switch (status) {
            case BILLED, EXEMPTED, PATIENT_SHARE_EXEMPTED -> BillingState.BILLED;
            case CORRECTION_PENDING -> BillingState.CORRECTING;
            case PENDING, UNPAID -> BillingState.UNBILLED;
        };
    }

    private static ExemptionType exemptionModeOf(VisitProductStatus status) {
        if (status == VisitProductStatus.EXEMPTED) {
            return ExemptionType.FULL;
        }
        if (status == VisitProductStatus.PATIENT_SHARE_EXEMPTED) {
            return ExemptionType.PATIENT_SHARE;
        }
        return ExemptionType.NONE;
    }

    private static VisitProductStatus legacyStatusOf(BillingState billingState, ExemptionType exemptionMode) {
        if (billingState == null) {
            return VisitProductStatus.PENDING;
        }
        return switch (billingState) {
            case UNBILLED -> VisitProductStatus.PENDING;
            case CORRECTING -> VisitProductStatus.CORRECTION_PENDING;
            case BILLED -> switch (exemptionMode == null ? ExemptionType.NONE : exemptionMode) {
                case FULL -> VisitProductStatus.EXEMPTED;
                case PATIENT_SHARE -> VisitProductStatus.PATIENT_SHARE_EXEMPTED;
                case NONE -> VisitProductStatus.BILLED;
            };
        };
    }

    public VisitDepartmentProductSource getSource() {
        return source;
    }

    public void setSource(VisitDepartmentProductSource source) {
        this.source = source;
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

    public Worker getConfirmedBy() {
        return confirmedBy;
    }

    public void setConfirmedBy(Worker confirmedBy) {
        this.confirmedBy = confirmedBy;
    }

    public com.nexxserve.nexxclinic.model.BillingConfirmationStatus getBillingConfirmationStatus() {
        return billingConfirmationStatus;
    }

    public void setBillingConfirmationStatus(com.nexxserve.nexxclinic.model.BillingConfirmationStatus billingConfirmationStatus) {
        this.billingConfirmationStatus = billingConfirmationStatus;
    }

    public Worker getProcessor() {
        return processor;
    }

    public void setProcessor(Worker processor) {
        this.processor = processor;
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

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
