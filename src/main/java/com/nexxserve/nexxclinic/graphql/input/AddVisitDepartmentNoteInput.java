package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.NoteType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AddVisitDepartmentNoteInput(
        @NotNull(message = "visitDepartmentId is required")
        UUID visitDepartmentId,

        List<UUID> targetUserId,
        @Size(min = 1, max = 100)
        NoteType noteType,

        @NotBlank(message = "content is required")
        @Size(max = 5000, message = "content must not exceed 5000 characters")
        String content
) {}
