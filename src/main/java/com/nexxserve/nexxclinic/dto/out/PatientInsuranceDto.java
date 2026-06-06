package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientInsuranceDto(
        UUID id,
        UUID patientId,
        UUID insuranceProviderId,
        String insuranceName,
        String insuranceAcronym,
        Double coveragePercentage,
        String insuranceCardNumber,
        String providingCompanyOrEmployer,
        boolean principalMember,
        String principalMemberName,
        String principalMemberPhoneNumber,
        LocalDate validFrom,
        LocalDate validUntil,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}