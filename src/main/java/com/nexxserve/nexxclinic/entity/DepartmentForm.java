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
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "department_forms",
        indexes = {
                @Index(name = "idx_department_forms_department_id", columnList = "department_id"),
                @Index(name = "idx_department_forms_updated_at", columnList = "updatedAt")
        }
)
public class DepartmentForm {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "department_id", nullable = true)
    private Department department;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private FormStatus status;

    @Column(nullable = false, length = 50)
    private String currentVersionNumber;

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
        if (this.currentVersionNumber == null || this.currentVersionNumber.isBlank()) {
            this.currentVersionNumber = "1.0.0";
        }
        if (this.formData == null || this.formData.isBlank()) {
            this.formData = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = FormStatus.DRAFT;
        }
        if (this.currentVersionNumber == null || this.currentVersionNumber.isBlank()) {
            this.currentVersionNumber = "1.0.0";
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
