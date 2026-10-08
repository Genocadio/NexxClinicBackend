package com.nexxserve.nexxclinic.entity;

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
import java.time.LocalDateTime;
import java.util.UUID;
import com.nexxserve.nexxclinic.model.VisitDepartmentDiagnosisType;

@Entity
@Table(name = "visit_department_diagnostics")
public class VisitDepartmentDiagnosis {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @Column(nullable = false)
    private String diagnosisName;

    @Column(name = "icd11_code")
    private String icd11Code;

    @Enumerated(EnumType.STRING)
    @Column(name = "diagnosis_type", nullable = false, length = 16)
    private VisitDepartmentDiagnosisType type = VisitDepartmentDiagnosisType.FINAL;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String notes = "";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
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

    public String getDiagnosisName() {
        return diagnosisName;
    }

    public void setDiagnosisName(String diagnosisName) {
        this.diagnosisName = diagnosisName;
    }

    public String getIcd11Code() {
        return icd11Code;
    }

    public void setIcd11Code(String icd11Code) {
        this.icd11Code = icd11Code;
    }

    public VisitDepartmentDiagnosisType getType() {
        return type;
    }

    public void setType(VisitDepartmentDiagnosisType type) {
        this.type = type == null ? VisitDepartmentDiagnosisType.FINAL : type;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes;
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
