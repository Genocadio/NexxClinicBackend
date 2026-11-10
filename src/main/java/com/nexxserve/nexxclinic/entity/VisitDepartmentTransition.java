package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "visit_department_transitions",
        indexes = {
                @Index(name = "idx_vdt_visit", columnList = "visit_id, occurred_at"),
                @Index(name = "idx_vdt_dept", columnList = "visit_department_id, occurred_at")
        }
)
public class VisitDepartmentTransition {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_id", nullable = false)
    private Visit visit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "visit_department_id", nullable = false)
    private VisitDepartment visitDepartment;

    @Column(name = "from_status", nullable = false, length = 32)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 32)
    private String toStatus;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType; // DISCHARGE, SEND_BACK, SEND_TO_NEXT, START, PAYMENT_COMPLETE, REOPEN

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_department_id")
    private Department targetDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_department_id")
    private Department previousDepartment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Worker actor;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @Column(length = 500)
    private String note;

    @PrePersist
    void onCreate() {
        if (this.occurredAt == null) {
            this.occurredAt = LocalDateTime.now();
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

    public VisitDepartment getVisitDepartment() {
        return visitDepartment;
    }

    public void setVisitDepartment(VisitDepartment visitDepartment) {
        this.visitDepartment = visitDepartment;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Department getTargetDepartment() {
        return targetDepartment;
    }

    public void setTargetDepartment(Department targetDepartment) {
        this.targetDepartment = targetDepartment;
    }

    public Department getPreviousDepartment() {
        return previousDepartment;
    }

    public void setPreviousDepartment(Department previousDepartment) {
        this.previousDepartment = previousDepartment;
    }

    public Worker getActor() {
        return actor;
    }

    public void setActor(Worker actor) {
        this.actor = actor;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
