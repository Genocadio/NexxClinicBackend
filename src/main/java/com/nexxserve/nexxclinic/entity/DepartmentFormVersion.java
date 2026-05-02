package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "department_form_versions",
    indexes = {
        @Index(name = "idx_dept_form_versions_form_id", columnList = "form_id"),
        @Index(name = "idx_dept_form_versions_version_number", columnList = "version_number")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_form_version", columnNames = {"form_id", "version_number"})
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
    @Column(nullable = false, length = 20)
    private FormStatus status;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String formData;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = FormStatus.DRAFT;
        }
        if (this.formData == null || this.formData.isBlank()) {
            this.formData = "{}";
        }
    }

    // Getters and Setters
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
}
