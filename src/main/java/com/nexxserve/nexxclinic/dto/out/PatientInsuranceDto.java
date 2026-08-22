package com.nexxserve.nexxclinic.dto.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PatientInsuranceDto(
        UUID id,
        PatientDto patient,
        InsuranceProviderDto insuranceProvider,
        String insuranceCardNumber,
        String providingCompanyOrEmployer,
        boolean principalMember,
        String principalMemberName,
        String principalMemberPhoneNumber,
        LocalDate validFrom,
        LocalDate validUntil,
        boolean deactivated,
        Integer patientSharePercentage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}