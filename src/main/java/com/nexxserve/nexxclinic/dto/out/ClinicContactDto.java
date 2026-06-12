package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.ClinicContactType;

public record ClinicContactDto(
        ClinicContactType contactType,
        String value,
        String description
) {}