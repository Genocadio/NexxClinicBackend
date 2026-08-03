package com.nexxserve.nexxclinic.entity.billing;

import com.nexxserve.nexxclinic.entity.Visit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
// A1 fix: (visit_id, version) must be unique so createNextBillingVersion can never mint
// two version-N rows for the same visit under concurrency (which would make the
// "latest version" guards in payments/invoices non-deterministic). On existing databases
// the same index is created by docs/migrations/visit_billing_versions_unique_version.sql.
@Table(
    name = "visit_billing_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = { "visit_id", "version" })
)
public class VisitBillingVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @Column(nullable = false)
    private int version;

    @Column
    private UUID supersedesVersionId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.version <= 0) {
            this.version = 1;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public UUID getSupersedesVersionId() {
        return supersedesVersionId;
    }

    public void setSupersedesVersionId(UUID supersedesVersionId) {
        this.supersedesVersionId = supersedesVersionId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
