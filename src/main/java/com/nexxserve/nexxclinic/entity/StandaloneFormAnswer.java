package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.config.JsonbType;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "form_answers",
        indexes = {
                @Index(name = "idx_form_answers_form_version_id", columnList = "form_version_id"),
                @Index(name = "idx_form_answers_patient_id", columnList = "patient_id"),
                @Index(name = "idx_form_answers_visit_id", columnList = "visit_id")
        }
)
public class StandaloneFormAnswer {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_version_id", nullable = false)
    private StandaloneFormVersion formVersion;


    @Type(JsonbType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String answers; // Key-value map: { "block_id": "value" }

    @Column(precision = 19, scale = 4)
    private BigDecimal score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnswerStatus status = AnswerStatus.DRAFT;

    @Column(name = "patient_id")
    private UUID patientId;

    @Column(name = "visit_id")
    private UUID visitId;

    @Column(name = "submitted_by")
    private UUID submittedBy; // Worker ID who filled the form

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.answers == null) {
            this.answers = "{}";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StandaloneFormVersion getFormVersion() {
        return formVersion;
    }

    public void setFormVersion(StandaloneFormVersion formVersion) {
        this.formVersion = formVersion;
    }


    public String getAnswers() {
        return answers;
    }

    public void setAnswers(String answers) {
        this.answers = answers;
    }

    public BigDecimal getScore() {
        return score;
    }

    public void setScore(BigDecimal score) {
        this.score = score;
    }

    public AnswerStatus getStatus() {
        return status;
    }

    public void setStatus(AnswerStatus status) {
        this.status = status;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public void setPatientId(UUID patientId) {
        this.patientId = patientId;
    }

    public UUID getVisitId() {
        return visitId;
    }

    public void setVisitId(UUID visitId) {
        this.visitId = visitId;
    }

    public UUID getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(UUID submittedBy) {
        this.submittedBy = submittedBy;
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
