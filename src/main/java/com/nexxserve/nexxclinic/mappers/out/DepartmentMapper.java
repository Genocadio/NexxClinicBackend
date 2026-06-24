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
    @Mapping(target = "standaloneForms", ignore = true)
    @Mapping(target = "defaultStandaloneForm", ignore = true)
    DepartmentDto toDto(Department department);

    @Mapping(target = "id", source = "department.id")
    @Mapping(target = "name", source = "department.name")
    @Mapping(target = "insurancePolicyMode", source = "department.insurancePolicyMode")
    @Mapping(target = "nursing", source = "department.nursing")
    @Mapping(target = "supportRequests", source = "department.supportRequests")
    @Mapping(target = "requestsProducts", source = "department.requestsProducts")
    @Mapping(target = "createdAt", source = "department.createdAt")
    @Mapping(target = "updatedAt", source = "department.updatedAt")
    @Mapping(target = "insurancePolicies", source = "insurancePolicies")
    @Mapping(target = "defaultProducts", source = "defaultProducts")
    @Mapping(target = "standaloneForms", source = "standaloneForms")
    @Mapping(target = "defaultStandaloneForm", source = "defaultStandaloneForm")
    DepartmentDto toDtoWithDetails(
            Department department,
            List<com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto> insurancePolicies,
            List<com.nexxserve.nexxclinic.dto.out.ProductDto> defaultProducts,
            List<com.nexxserve.nexxclinic.dto.out.StandaloneFormDto> standaloneForms,
            com.nexxserve.nexxclinic.dto.out.StandaloneFormDto defaultStandaloneForm
    );

    List<DepartmentDto> toDtoList(List<Department> departments);
}