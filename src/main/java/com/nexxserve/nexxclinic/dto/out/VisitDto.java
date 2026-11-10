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
        Boolean quickBillEligible,
        String currentLocations,
        Integer activeEpisodeCount,
        Boolean isDischarged,
        Boolean hasReturnedFromBilling,
        String lastReturnReason,
        Boolean hasPendingResults,
        LocalDateTime lastFlowActivityAt
) {
    public VisitDto(
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
    ) {
        this(
                id, patient, status, visitDate, linkedInsurances, departments, vitalSigns,
                estimatedTotal, estimatedInsurancePay, estimatedPatientPay, quickBillEligible,
                null, 0, false, false, null, false, null
        );
    }
}
