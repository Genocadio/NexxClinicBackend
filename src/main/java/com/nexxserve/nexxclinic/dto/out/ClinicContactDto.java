package com.nexxserve.nexxclinic.dto.out;

public record ClinicContactDto(
        String contactType,
        String value,
        String description
) {}