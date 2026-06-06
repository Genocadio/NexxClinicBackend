package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.WorkerDocumentDto;
import com.nexxserve.nexxclinic.dto.out.WorkerDto;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.entity.WorkerDocument;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {DepartmentMapper.class})
public interface WorkerMapper {

    @Mapping(target = "activeSessions", ignore = true)    // Manually appended via SessionTokenService
    @Mapping(target = "maxActiveSessions", ignore = true) // Manually appended via SessionTokenService
    WorkerDto toDto(Worker worker);

    List<WorkerDto> toDtoList(List<Worker> workers);

    WorkerDocumentDto toDto(WorkerDocument document);

    List<WorkerDocumentDto> toDocumentDtoList(List<WorkerDocument> documents);
}