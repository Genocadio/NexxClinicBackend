// ──────────────────────────────────────────────────────────────
// File: VisitDepartmentNoteViewerMapper.java
package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.VisitDepartmentNoteViewerDto;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNoteViewer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * Maps {@link VisitDepartmentNoteViewer} entities → {@link VisitDepartmentNoteViewerDto}s.
 *
 * <p>Only the {@code viewer} (a {@link com.nexxserve.nexxclinic.entity.Worker}) is mapped
 * to the DTO’s {@code worker}.  MapStruct will automatically delegate that field to
 * {@link WorkerMapper}.  All other fields match by name and need no explicit mapping.</p>
 */
@Mapper(
        componentModel = "spring",
        uses          = { WorkerMapper.class }   // required for mapping a Worker → WorkerDto
)
public interface VisitDepartmentNoteViewerMapper {

    /**
     * Convert a single entity to its DTO.
     *
     * @param viewer the {@link VisitDepartmentNoteViewer} to convert
     * @return a populated {@link VisitDepartmentNoteViewerDto}
     */
    @Mapping(target = "worker", source = "viewer")   // <‑ correct: entity has `getViewer()`
    VisitDepartmentNoteViewerDto toDto(VisitDepartmentNoteViewer viewer);

    /**
     * Convert a list of entities.
     *
     * @param viewers the collection to convert
     * @return a list of DTOs
     */
    List<VisitDepartmentNoteViewerDto> toDtoList(List<VisitDepartmentNoteViewer> viewers);
}
