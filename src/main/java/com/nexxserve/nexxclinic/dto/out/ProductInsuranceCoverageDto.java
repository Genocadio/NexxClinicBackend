package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.DrugAdministrationFrequency;
import com.nexxserve.nexxclinic.model.MustPrescribedBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProductInsuranceCoverageDto(
        UUID id,
        InsuranceProviderDto insuranceProvider,
        BigDecimal cost,
        boolean covered,
        boolean notPaid,
        boolean requireMedicalAdvisor,
        MustPrescribedBy mustPrescribedBy,
        DrugAdministrationFrequency drugAdministrationFrequency,
        List<String> authorizationRequestReasons,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}