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
 * <p>Only the fields that are common to all notes (id, department, content,
 * creator and note type) are mapped automatically.  The {@code viewed}
 * flag is set in {@link com.nexxserve.nexxclinic.service.VisitDepartmentNoteService#visitDepartmentNoteToDto(VisitDepartmentNote, AuthenticatedUser)}.</p>
 */
@Mapper(
        componentModel = "spring",
        uses          = { WorkerMapper.class }
)
public interface VisitDepartmentNoteMapper {

    @Mapping(target = "visitDepartmentId", source = "visitDepartment.id")
    @Mapping(target = "createdBy",          source = "createdBy")   // → WorkerMapper
    @Mapping(target = "noteType",           source = "noteType")     // enum is mapped automatically
    VisitDepartmentNoteDto toDto(VisitDepartmentNote note);

    List<VisitDepartmentNoteDto> toDtoList(List<VisitDepartmentNote> notes);
}
