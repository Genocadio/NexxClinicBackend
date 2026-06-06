package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record WorkerDocumentDto(
        UUID id,
        String name,
        String fileKey,
        String fileUrl,
        String mimeType,
        Long fileSize,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}