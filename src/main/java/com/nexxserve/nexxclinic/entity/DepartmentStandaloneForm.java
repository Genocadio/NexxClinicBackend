package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "department_standalone_forms", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"department_id", "standalone_form_id"})
})
public class DepartmentStandaloneForm {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "standalone_form_id", nullable = false)
    private StandaloneForm standaloneForm;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    // ADD THIS FIELD - it was missing!
    @Column(name = "added_at", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime addedAt;

    // Getters and setters...

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

    public StandaloneForm getStandaloneForm() {
        return standaloneForm;
    }

    public void setStandaloneForm(StandaloneForm standaloneForm) {
        this.standaloneForm = standaloneForm;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    // ADD THIS GETTER/SETTER
    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}