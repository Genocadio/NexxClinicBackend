package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PatientDto(
        UUID id,
        String patientIdentifier,
        String firstName,
        String middleName,
        String lastName,
        String fullName,
        LocalDate dateOfBirth,
        Integer age,
        Gender gender,
        String primaryPhoneNumber,
        String alternativePhone,
        String village,
        String cell,
        String city,
        String district,
        String postalAddress,
        String nationalIdNumber,
        String passportNumber,
        String emergencyContactName,
        String emergencyContactRelationship,
        String emergencyContactPhoneNumber,
        List<PatientInsuranceDto> patientInsurances,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
