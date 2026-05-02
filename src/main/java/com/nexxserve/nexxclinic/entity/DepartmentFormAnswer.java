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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "department_form_answers",
    indexes = {
        @Index(name = "idx_dept_form_answers_consultation_id", columnList = "consultation_id"),
        @Index(name = "idx_dept_form_answers_visit_id", columnList = "visit_id"),
        @Index(name = "idx_dept_form_answers_patient_id", columnList = "patient_id"),
        @Index(name = "idx_dept_form_answers_department_id", columnList = "department_id"),
        @Index(name = "idx_dept_form_answers_form_id", columnList = "form_id"),
        @Index(name = "idx_dept_form_answers_status", columnList = "status")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_consultation_form", columnNames = {"consultation_id", "form_id"})
    }
)
public class DepartmentFormAnswer {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String consultationId;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String visitId;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String patientId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private DepartmentForm form;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_version_id", nullable = false)
    private DepartmentFormVersion formVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnswerStatus status;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String answers;

    @Column(nullable = true)
    private LocalDateTime submittedAt;

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
            this.status = AnswerStatus.DRAFT;
        }
        if (this.answers == null || this.answers.isBlank()) {
            this.answers = "{}";
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

    public String getConsultationId() {
        return consultationId;
    }

    public void setConsultationId(String consultationId) {
        this.consultationId = consultationId;
    }

    public String getVisitId() {
        return visitId;
    }

    public void setVisitId(String visitId) {
        this.visitId = visitId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public DepartmentForm getForm() {
        return form;
    }

    public void setForm(DepartmentForm form) {
        this.form = form;
    }

    public DepartmentFormVersion getFormVersion() {
        return formVersion;
    }

    public void setFormVersion(DepartmentFormVersion formVersion) {
        this.formVersion = formVersion;
    }

    public AnswerStatus getStatus() {
        return status;
    }

    public void setStatus(AnswerStatus status) {
        this.status = status;
    }

    public String getAnswers() {
        return answers;
    }

    public void setAnswers(String answers) {
        this.answers = answers;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
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
