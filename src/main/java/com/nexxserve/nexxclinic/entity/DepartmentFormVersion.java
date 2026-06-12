package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.FormStatus;
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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "department_form_versions",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_department_form_version", columnNames = {"form_id", "versionNumber"})
        },
        indexes = {
        @Index(name = "idx_department_form_versions_form_id", columnList = "form_id")
        }
)
public class DepartmentFormVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private DepartmentForm form;

    @Column(nullable = false, length = 50)
    private String versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FormStatus status;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String formData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.versionNumber == null || this.versionNumber.isBlank()) {
            this.versionNumber = "1.0.0";
        }
        if (this.status == null) {
            this.status = FormStatus.DRAFT;
        }
        if (this.formData == null || this.formData.isBlank()) {
            this.formData = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.versionNumber == null || this.versionNumber.isBlank()) {
            this.versionNumber = "1.0.0";
        }
        if (this.status == null) {
            this.status = FormStatus.DRAFT;
        }
        if (this.formData == null || this.formData.isBlank()) {
            this.formData = "{}";
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DepartmentForm getForm() {
        return form;
    }

    public void setForm(DepartmentForm form) {
        this.form = form;
    }

    public String getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(String versionNumber) {
        this.versionNumber = versionNumber;
    }

    public FormStatus getStatus() {
        return status;
    }

    public void setStatus(FormStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
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
