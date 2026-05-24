package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "visit_pre_instruction_medications")
public class VisitPreInstructionMedication {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pre_instruction_id", nullable = false)
    private VisitPreInstruction preInstruction;

    @Column(nullable = false, length = 200)
    private String medName;

    @Column(length = 200)
    private String dosage;

    @Column(length = 100)
    private String route;

    @Column(length = 200)
    private String frequency;

    @Column(length = 200)
    private String duration;

    @Column(length = 100)
    private String quantity;

    @Column(length = 1000)
    private String otherInstructions;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public VisitPreInstruction getPreInstruction() {
        return preInstruction;
    }

    public void setPreInstruction(VisitPreInstruction preInstruction) {
        this.preInstruction = preInstruction;
    }

    public String getMedName() {
        return medName;
    }

    public void setMedName(String medName) {
        this.medName = medName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public String getOtherInstructions() {
        return otherInstructions;
    }

    public void setOtherInstructions(String otherInstructions) {
        this.otherInstructions = otherInstructions;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
