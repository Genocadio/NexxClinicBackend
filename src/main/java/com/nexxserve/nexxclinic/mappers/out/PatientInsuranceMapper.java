package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.PatientInsuranceDto;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PatientInsuranceMapper {

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "insuranceProvider.id", target = "insuranceProviderId")
    @Mapping(source = "insuranceProvider.insuranceName", target = "insuranceProviderName")
    @Mapping(source = "insuranceProvider.acronym", target = "insuranceProviderAcronym")
    @Mapping(source = "insuranceProvider.defaultCoveragePercentage", target = "insuranceProviderDefaultCoveragePercentage")
    PatientInsuranceDto toDto(PatientInsurance pi);
}