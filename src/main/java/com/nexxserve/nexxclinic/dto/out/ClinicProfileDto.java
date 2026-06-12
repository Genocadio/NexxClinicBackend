package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.graphql.input.ClinicMetadataDto;

import java.util.List;
import java.util.UUID;

public record ClinicProfileDto(
        UUID id,
        String name,
        String username,
        String address,
        List<ClinicContactDto> contacts,
        String tinNumber,
        String logoUrl,
        List<ClinicMetadataDto> metadata,
        String createdAt,
        String updatedAt
) {}