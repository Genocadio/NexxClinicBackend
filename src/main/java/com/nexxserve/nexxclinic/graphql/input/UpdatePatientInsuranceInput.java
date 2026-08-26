package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.validation.ValidPhoneNumber;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdatePatientInsuranceInput(
        UUID patientId,
        UUID insuranceProviderId,

        @Size(max = 64, message = "insuranceCardNumber must not exceed 64 characters")
        String insuranceCardNumber,

        @Size(max = 150, message = "providingCompanyOrEmployer must not exceed 150 characters")
        String providingCompanyOrEmployer,

        Boolean principalMember,

        @Size(max = 120, message = "principalMemberName must not exceed 120 characters")
        String principalMemberName,

        @Size(max = 30, message = "principalMemberPhoneNumber must not exceed 30 characters")
        @ValidPhoneNumber
        String principalMemberPhoneNumber,
        LocalDate validFrom,        LocalDate validUntil,

        Integer patientSharePercentage,

        /**
         * Reference to an InsuranceCoverage record whose patientSharePercentage
         * defines the patient's share. When provided, this takes precedence over
         * the legacy patientSharePercentage integer.
         */
        UUID patientShareCoverageId
) {}
