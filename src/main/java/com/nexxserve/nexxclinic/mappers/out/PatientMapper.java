package com.nexxserve.nexxclinic.mappers.out;

import com.nexxserve.nexxclinic.dto.out.PatientDto;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {PatientInsuranceMapper.class} // Tells MapStruct to inject and use your insurance mapper for lists
)
public interface PatientMapper {

    // 1. Tell MapStruct exactly where to map fields when multiple sources are provided
    @Mapping(source = "patient.id", target = "id")
    @Mapping(source = "patient.patientIdentifier", target = "patientIdentifier")
    @Mapping(source = "patient.createdAt", target = "createdAt")
    @Mapping(source = "patient.updatedAt", target = "updatedAt")
    @Mapping(source = "insurances", target = "patientInsurances")
    // 2. Call your custom calculation method using a Java expression
    @Mapping(target = "age", expression = "java(calculateAge(patient))")
    PatientDto toDto(Patient patient, List<PatientInsurance> insurances);

    // 3. Keep your custom logic cleanly tucked inside a default method
    default Integer calculateAge(Patient patient) {
        if (patient == null || patient.getDateOfBirth() == null) {
            return null;
        }
        return Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
    }
}
