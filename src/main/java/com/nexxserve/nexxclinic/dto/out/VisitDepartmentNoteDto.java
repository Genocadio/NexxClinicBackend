// ──────────────────────────────────────────────────────────────
// File: VisitDepartmentNoteDto.java
package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.NoteType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The DTO that represents a visit‑department note in the API layer.
 *
 * <p>All primitive fields are copied verbatim.  Collections are mapped via
 * dedicated mappers: {@code WorkerMapper} for {@code targetUsers}
 * and {@code VisitDepartmentNoteViewerMapper} for {@code viewers}.
 */
public record VisitDepartmentNoteDto(
        UUID                 id,
        UUID                 visitDepartmentId,
        String               content,

        /* ----------------------------------------------- *
         *  Nested objects – resolved by other mappers      *
         * ----------------------------------------------- */

        WorkerDto            createdBy,           // mapped via WorkerMapper
        List<VisitDepartmentNoteViewerDto> viewers,   // mapped via ViewerMapper

        /* ----------------------------------------------- *
         *  Additional payloads                           *
         * ----------------------------------------------- */

        NoteType             noteType,            // enum – copied directly
        List<WorkerDto>      targetUsers,         // mapped via WorkerMapper

        boolean              isNew,
        LocalDateTime        createdAt) { }
