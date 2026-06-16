package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.NoteType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The DTO that represents a visit‑department note in the API layer.
 *
 * <p>All primitive fields are copied verbatim.  The record no longer contains
 * a list of target users or an {@code isNew} flag – visibility is handled by
 * the service layer.</p>
 */
public record VisitDepartmentNoteDto(
        /* 1 */ UUID                 id,
        /* 2 */ UUID                 visitDepartmentId,
        /* 3 */ String               content,

        /* 4 */ WorkerDto            createdBy,           // mapped via WorkerMapper
        /* 5 */ NoteType             noteType,            // enum – copied directly

        /* 6 */ boolean              viewed,              // true if current user has viewed this note
        /* 7 */ LocalDateTime        createdAt) { }
