package com.nexxserve.nexxclinic.dto.out;

import com.nexxserve.nexxclinic.model.VisitStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record VisitDto(
        UUID id,
        PatientDto patient,
        VisitStatus status,
        LocalDateTime visitDate,
        List<PatientInsuranceDto> linkedInsurances,
        List<VisitDepartmentDto> departments,
        List<VisitVitalSignsGroupDto> vitalSigns,
        BigDecimal estimatedTotal,
        BigDecimal estimatedInsurancePay,
        BigDecimal estimatedPatientPay,
        Boolean quickBillEligible
) {}
