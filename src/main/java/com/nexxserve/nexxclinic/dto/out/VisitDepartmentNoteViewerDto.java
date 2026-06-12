package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDateTime;
import java.util.UUID;

public record VisitDepartmentNoteViewerDto(
        WorkerDto worker,
        LocalDateTime viewedAt
) {}
