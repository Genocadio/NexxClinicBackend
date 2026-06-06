package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.DepartmentDto;
import com.nexxserve.nexxclinic.entity.Department;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProductMapper.class, InsuranceProviderMapper.class})
public interface DepartmentMapper {

    @Mapping(target = "insurancePolicies", ignore = true) // Handled dynamically/manually via custom logic in WorkerService if required
    @Mapping(target = "defaultProducts", ignore = true)     // Handled dynamically/manually via custom logic in WorkerService if required
    DepartmentDto toDto(Department department);

    List<DepartmentDto> toDtoList(List<Department> departments);
}