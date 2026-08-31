package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.EncounterType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "visit_departments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_visit_department", columnNames = {"visit_id", "department_id"})
        }
)
public class VisitDepartment {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    /**
     * The department profile explicitly applied to this visit department (via
     * addVisitDepartment/createVisit with a profileId, or
     * changeVisitDepartmentProfile). Never auto-applied: null when added without
     * a profile. Products added from the profile carry source=PROFILE and are
     * managed by the profile (they cannot be removed individually).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id")
    private DepartmentProfile profile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VisitDepartmentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "encounter_type", nullable = false, columnDefinition = "varchar(255) default 'OUTPATIENT'")
    private EncounterType encounterType;

    @Column
    private LocalDateTime completedAt;

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<VisitDepartmentDiagnosis> diagnostics = new ArrayList<>();

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<VisitDepartmentMedication> medications = new ArrayList<>();

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<VisitPreInstruction> preInstructions = new ArrayList<>();

    @OneToMany(mappedBy = "visitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @org.hibernate.annotations.BatchSize(size = 20)
    private List<VisitDepartmentNote> notes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_visit_department_id")
    private VisitDepartment parentVisitDepartment;

    @OneToMany(mappedBy = "parentVisitDepartment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitDepartment> childVisitDepartments = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "visit_department_processors",
            joinColumns = @JoinColumn(name = "visit_department_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id")
    )
    private List<Worker> processors = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_edit_source_status", length = 32)
    private VisitDepartmentStatus billingEditSourceStatus;

    @Column(name = "answer_id")
    private UUID answerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.encounterType == null) {
            this.encounterType = EncounterType.OUTPATIENT;
        }
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

    public Visit getVisit() {
        return visit;
    }

    public void setVisit(Visit visit) {
        this.visit = visit;
    }

    public Department getDepartment() {
        return department;
    }

    public void setDepartment(Department department) {
        this.department = department;
    }

    public DepartmentProfile getProfile() {
        return profile;
    }

    public void setProfile(DepartmentProfile profile) {
        this.profile = profile;
    }

    public VisitDepartmentStatus getStatus() {
        return status;
    }

    public void setStatus(VisitDepartmentStatus status) {
        this.status = status;
        if (status == VisitDepartmentStatus.COMPLETED && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<VisitDepartmentDiagnosis> getDiagnostics() {
        return diagnostics;
    }

    public void setDiagnostics(List<VisitDepartmentDiagnosis> diagnostics) {
        this.diagnostics = diagnostics;
    }

    public List<VisitDepartmentMedication> getMedications() {
        return medications;
    }

    public void setMedications(List<VisitDepartmentMedication> medications) {
        this.medications = medications;
    }

    public List<VisitPreInstruction> getPreInstructions() {
        return preInstructions;
    }

    public void setPreInstructions(List<VisitPreInstruction> preInstructions) {
        this.preInstructions = preInstructions;
    }

    public List<VisitDepartmentNote> getNotes() {
        return notes;
    }

    public void setNotes(List<VisitDepartmentNote> notes) {
        this.notes = notes == null ? new ArrayList<>() : new ArrayList<>(notes);
    }

    public VisitDepartment getParentVisitDepartment() {
        return parentVisitDepartment;
    }

    public void setParentVisitDepartment(VisitDepartment parentVisitDepartment) {
        this.parentVisitDepartment = parentVisitDepartment;
    }

    public List<VisitDepartment> getChildVisitDepartments() {
        return childVisitDepartments;
    }

    public void setChildVisitDepartments(List<VisitDepartment> childVisitDepartments) {
        this.childVisitDepartments = childVisitDepartments == null ? new ArrayList<>() : new ArrayList<>(childVisitDepartments);
    }

    public List<Worker> getProcessors() {
        return processors;
    }

    public void setProcessors(List<Worker> processors) {
        this.processors = processors == null ? new ArrayList<>() : new ArrayList<>(processors);
    }

    public EncounterType getEncounterType() {
        return encounterType;
    }

    public void setEncounterType(EncounterType encounterType) {
        this.encounterType = encounterType;
    }

    public VisitDepartmentStatus getBillingEditSourceStatus() {
        return billingEditSourceStatus;
    }

    public void setBillingEditSourceStatus(VisitDepartmentStatus billingEditSourceStatus) {
        this.billingEditSourceStatus = billingEditSourceStatus;
    }

    public UUID getAnswerId() {
        return answerId;
    }

    public void setAnswerId(UUID answerId) {
        this.answerId = answerId;
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

