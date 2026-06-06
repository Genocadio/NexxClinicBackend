package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InsuranceProviderMapper {
    InsuranceProviderDto toDto(InsuranceProvider provider);
}