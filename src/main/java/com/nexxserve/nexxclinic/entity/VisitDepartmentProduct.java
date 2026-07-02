package com.nexxserve.nexxclinic.entity;

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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "visit_department_products",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_visit_department_product", columnNames = {"visit_department_id", "product_id"})
        }
)
public class VisitDepartmentProduct {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private VisitProductStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by_worker_id")
    private Worker addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billed_by_worker_id")
    private Worker billedBy;

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
        if (this.status == null) {
            this.status = VisitProductStatus.PENDING;
        }
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = BigDecimal.ONE;
        }
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) < 0) {
            this.price = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = VisitProductStatus.PENDING;
        }
        if (this.quantity == null || this.quantity.compareTo(BigDecimal.ZERO) <= 0) {
            this.quantity = BigDecimal.ONE;
        }
        if (this.price == null || this.price.compareTo(BigDecimal.ZERO) < 0) {
            this.price = BigDecimal.ZERO;
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public VisitProductStatus getStatus() {
        return status;
    }

    public void setStatus(VisitProductStatus status) {
        this.status = status;
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
