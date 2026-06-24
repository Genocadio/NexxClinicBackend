package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.FormStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record StandaloneFormVersionDto(
        UUID id,
        UUID formId,
        String versionLabel,
        int majorVersion,
        int minorVersion,
        Object blocks,
        Object theme,
        FormStatus status,
        LocalDateTime createdAt
) {}
