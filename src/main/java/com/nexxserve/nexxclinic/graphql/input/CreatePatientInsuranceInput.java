package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePatientInsuranceInput(
        @NotNull(message = "patientId is required")
        UUID patientId,

        @NotNull(message = "insuranceProviderId is required")
        UUID insuranceProviderId,

        @NotBlank(message = "insuranceCardNumber is required")
        @Size(max = 64, message = "insuranceCardNumber must not exceed 64 characters")
        String insuranceCardNumber,

        @Size(max = 150, message = "providingCompanyOrEmployer must not exceed 150 characters")
        String providingCompanyOrEmployer,

        @NotNull(message = "principalMember is required")
        Boolean principalMember,

        @Size(max = 120, message = "principalMemberName must not exceed 120 characters")
        String principalMemberName,

        @Size(max = 30, message = "principalMemberPhoneNumber must not exceed 30 characters")
        String principalMemberPhoneNumber,

        @NotNull(message = "validFrom is required")
        LocalDate validFrom,

        @NotNull(message = "validUntil is required")
        LocalDate validUntil
) {
}
