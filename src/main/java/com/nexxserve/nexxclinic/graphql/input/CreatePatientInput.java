package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreatePatientInput(
        @NotBlank(message = "firstName is required")
        @Size(max = 120, message = "firstName must not exceed 120 characters")
        String firstName,

        @Size(max = 120, message = "middleName must not exceed 120 characters")
        String middleName,

        @Size(max = 120, message = "lastName must not exceed 120 characters")
        String lastName,

        @NotNull(message = "dateOfBirth is required")
        @PastOrPresent(message = "dateOfBirth must be in the past or present")
        LocalDate dateOfBirth,

        @NotNull(message = "gender is required")
        Gender gender,

        @Size(max = 30, message = "primaryPhoneNumber must not exceed 30 characters")
        String primaryPhoneNumber,

        @Size(max = 30, message = "alternativePhone must not exceed 30 characters")
        String alternativePhone,

        @Size(max = 120, message = "village must not exceed 120 characters")
        String village,

        @Size(max = 120, message = "city must not exceed 120 characters")
        String city,

        @Size(max = 120, message = "district must not exceed 120 characters")
        String district,

        @Size(max = 250, message = "postalAddress must not exceed 250 characters")
        String postalAddress,

        @Size(max = 64, message = "nationalIdNumber must not exceed 64 characters")
        String nationalIdNumber,

        @Size(max = 64, message = "passportNumber must not exceed 64 characters")
        String passportNumber,

        @Size(max = 120, message = "emergencyContactName must not exceed 120 characters")
        String emergencyContactName,

        @Size(max = 120, message = "emergencyContactRelationship must not exceed 120 characters")
        String emergencyContactRelationship,

        @Size(max = 30, message = "emergencyContactPhoneNumber must not exceed 30 characters")
        String emergencyContactPhoneNumber
) {
}
