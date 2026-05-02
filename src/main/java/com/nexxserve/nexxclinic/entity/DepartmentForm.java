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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "department_forms",
    indexes = {
        @Index(name = "idx_dept_forms_department_id", columnList = "department_id"),
        @Index(name = "idx_dept_forms_status", columnList = "status"),
        @Index(name = "idx_dept_forms_created_at", columnList = "created_at")
    }
)
public class DepartmentForm {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FormStatus status;

    @Column(nullable = false, length = 50)
    private String currentVersionNumber;

    @Column(nullable = false)
    private Integer currentSchemaVersion;

    @Lob
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
        if (this.status == null) {
            this.status = FormStatus.DRAFT;
        }
        if (this.currentVersionNumber == null) {
            this.currentVersionNumber = "1.0";
        }
        if (this.currentSchemaVersion == null) {
            this.currentSchemaVersion = 1;
        }
        if (this.formData == null || this.formData.isBlank()) {
            this.formData = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
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

    public FormStatus getStatus() {
        return status;
    }

    public void setStatus(FormStatus status) {
        this.status = status;
    }

    public String getCurrentVersionNumber() {
        return currentVersionNumber;
    }

    public void setCurrentVersionNumber(String currentVersionNumber) {
        this.currentVersionNumber = currentVersionNumber;
    }

    public Integer getCurrentSchemaVersion() {
        return currentSchemaVersion;
    }

    public void setCurrentSchemaVersion(Integer currentSchemaVersion) {
        this.currentSchemaVersion = currentSchemaVersion;
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
