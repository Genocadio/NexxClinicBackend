package com.nexxserve.nexxclinic.entity;

import com.nexxserve.nexxclinic.model.ClinicContactType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

@Embeddable
public class ClinicContact {

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 32)
    private ClinicContactType contactType;

    @Column(name = "contact_value", nullable = false, length = 200)
    private String value;

    @Column(length = 200)
    private String description;

    public ClinicContactType getContactType() {
        return contactType;
    }

    public void setContactType(ClinicContactType contactType) {
        this.contactType = contactType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}