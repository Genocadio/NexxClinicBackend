package com.nexxserve.nexxclinic.entity;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Lob;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "clinic_profiles")
public class ClinicProfile {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(length = 255)
    private String name;

    @Column(length = 1024)
    private String address;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "clinic_profile_contacts", joinColumns = @JoinColumn(name = "clinic_profile_id"))
    @OrderColumn(name = "contact_order")
    private List<ClinicContact> contacts = new ArrayList<>();

    @Column(name = "contacts", insertable = false, updatable = false)
    private String legacyContactsJson;

    @Column(length = 128)
    private String tinNumber;

    @Column(length = 1024)
    private String logoUrl;

    @Lob
    @Column(nullable = false)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.contacts == null) {
            this.contacts = new ArrayList<>();
        }
        if (this.metadata == null || this.metadata.isBlank()) {
            this.metadata = "{}";
        }
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (this.contacts == null) {
            this.contacts = new ArrayList<>();
        }
        if (this.metadata == null || this.metadata.isBlank()) {
            this.metadata = "{}";
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<ClinicContact> getContacts() {
        return contacts;
    }

    public void setContacts(List<ClinicContact> contacts) {
        this.contacts = contacts;
    }

    public String getLegacyContactsJson() {
        return legacyContactsJson;
    }

    public String getTinNumber() {
        return tinNumber;
    }

    public void setTinNumber(String tinNumber) {
        this.tinNumber = tinNumber;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
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
