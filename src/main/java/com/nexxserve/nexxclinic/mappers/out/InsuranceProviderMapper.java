package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.InsuranceCoverageDto;
import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InsuranceProviderMapper {

    @Mapping(target = "coverages", expression = "java(toCoverageDtos(provider.getCoverages()))")
    InsuranceProviderDto toDto(InsuranceProvider provider);

    default List<InsuranceCoverageDto> toCoverageDtos(List<InsuranceCoverage> coverages) {
        if (coverages == null) return Collections.emptyList();
        return coverages.stream().map(c -> new InsuranceCoverageDto(
            c.getId(),
            c.getInsuranceProvider().getId(),
            c.getInsuranceProvider().getInsuranceName(),
            c.getDepartment() != null ? c.getDepartment().getId() : null,
            c.getDepartment() != null ? c.getDepartment().getName() : null,
            c.getEncounterType(),
            c.getPatientSharePercentage(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        )).collect(Collectors.toList());
    }
}
