package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.NoteType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "visit_department_notes")
public class VisitDepartmentNote {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private Worker createdBy;

    @NotNull
    @Enumerated(EnumType.STRING)
    private NoteType noteType;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "note_target_users",
            joinColumns   = @JoinColumn(name = "note_id"),
            inverseJoinColumns = @JoinColumn(name = "worker_id")
    )
    private List<Worker> targetUsers = new ArrayList<>();

    @OneToMany(mappedBy = "note", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VisitDepartmentNoteViewer> viewers = new ArrayList<>();

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Worker getCreatedBy() {
        return createdBy;
    }


    public void setCreatedBy(Worker createdBy) {
        this.createdBy = createdBy;
    }


    public NoteType getNoteType() {return noteType; }
    public void setNoteType(NoteType noteType) { this.noteType = noteType; }

    public void addTargetUser(Worker w) {
        if (w == null) throw new IllegalArgumentException("target user cannot be null");
        targetUsers.add(w);
    }

    public List<Worker> getTargetUsers() {
        return Collections.unmodifiableList(targetUsers);
    }

    public void setTargetUsers(List<Worker> users) {
        this.targetUsers = users != null ? new ArrayList<>(users) : new ArrayList<>();
    }

    public List<VisitDepartmentNoteViewer> getViewers() {
        return viewers;
    }

    public void setViewers(List<VisitDepartmentNoteViewer> viewers) {
        this.viewers = viewers == null ? new ArrayList<>() : new ArrayList<>(viewers);
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
