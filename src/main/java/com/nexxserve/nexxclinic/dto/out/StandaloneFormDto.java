package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record StandaloneFormDto(
        UUID id,
        String name,
        String description,
        String type,
        String category,
        boolean isTemplate,
        UUID createdBy,
        StandaloneFormVersionDto activeVersion,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
