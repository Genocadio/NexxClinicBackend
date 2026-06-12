package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.PatientInsuranceDto;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import org.mapstruct.Mapper;

@Mapper(
        componentModel = "spring",
        uses = {
                PatientMapper.class,
                InsuranceProviderMapper.class
        }
)
public interface PatientInsuranceMapper {

    PatientInsuranceDto toDto(PatientInsurance pi);
}