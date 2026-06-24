package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.config.JsonbType;
import com.nexxserve.nexxclinic.model.FormStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "form_versions",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"form_id", "major_version", "minor_version"})
        }
)
public class StandaloneFormVersion {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "form_id", nullable = false)
    private StandaloneForm form;

    @Column(name = "major_version", nullable = false)
    private int majorVersion = 0;

    @Column(name = "minor_version", nullable = false)
    private int minorVersion = 0;

    @Column(name = "version_label", nullable = false)
    private String versionLabel; // Generated: major.minor (e.g., "0.5")

    @Type(JsonbType.class)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String blocks; // Array of FormBlock objects

    @Type(JsonbType.class)
    @Column(columnDefinition = "jsonb")
    private String theme; // FormTheme object

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormStatus status = FormStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.versionLabel == null) {
            this.versionLabel = this.majorVersion + "." + this.minorVersion;
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StandaloneForm getForm() {
        return form;
    }

    public void setForm(StandaloneForm form) {
        this.form = form;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    public String getVersionLabel() {
        return versionLabel;
    }

    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }

    public String getBlocks() {
        return blocks;
    }

    public void setBlocks(String blocks) {
        this.blocks = blocks;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public FormStatus getStatus() {
        return status;
    }

    public void setStatus(FormStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
