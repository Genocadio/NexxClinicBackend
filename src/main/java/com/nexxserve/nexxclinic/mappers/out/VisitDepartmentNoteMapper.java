// ──────────────────────────────────────────────────────────────
// File: VisitDepartmentNoteMapper.java
package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.VisitDepartmentNoteDto;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

/**
 * MapStruct mapper that converts {@link VisitDepartmentNote} entities into
 * {@link VisitDepartmentNoteDto}s.
 *
 * <p>All nested objects are resolved through other mappers:</p>
 * <ul>
 *     <li>{@code WorkerMapper}   – for {@code createdBy} and {@code targetUsers}</li>
 *     <li>{@code ViewerMapper}  – for the {@code viewers} list</li>
 * </ul>
 *
 * <p>Because {@link VisitDepartmentNoteDto} is a Java record, MapStruct
 * automatically creates an instance with all fields via its canonical
 * constructor.</p>
 */
@Mapper(
        componentModel = "spring",
        uses          = { WorkerMapper.class, VisitDepartmentNoteViewerMapper.class }
)
public interface VisitDepartmentNoteMapper {

    /**
     * Single‑entity mapping.
     *
     * @param note the source entity (may be {@code null})
     * @return a DTO or {@code null} if input is {@code null}
     */
    @Mapping(target = "visitDepartmentId", source = "visitDepartment.id")
    @Mapping(target = "createdBy",          source = "createdBy")   // → WorkerMapper
    @Mapping(target = "viewers",            source = "viewers")      // → ViewerMapper
    @Mapping(target = "targetUsers",        source = "targetUsers")  // → WorkerMapper
    @Mapping(target = "noteType",           source = "noteType")     // enum is mapped automatically
    @Mapping(target = "isNew",              expression = "java(false)")  // adjust to your logic
    VisitDepartmentNoteDto toDto(VisitDepartmentNote note);

    /**
     * List‑to‑list mapping – MapStruct will delegate each element.
     *
     * @param notes a list of entities (may be {@code null} or empty)
     * @return a list of DTOs (never {@code null})
     */
    List<VisitDepartmentNoteDto> toDtoList(List<VisitDepartmentNote> notes);
}
