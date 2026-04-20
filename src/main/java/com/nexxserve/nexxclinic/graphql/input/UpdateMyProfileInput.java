package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.DocumentType;
import com.nexxserve.nexxclinic.model.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateMyProfileInput(
        @Size(max = 120, message = "firstName must not exceed 120 characters")
        String firstName,

        @Size(max = 120, message = "lastName must not exceed 120 characters")
        String lastName,
        Gender gender,

        @PastOrPresent(message = "dateOfBirth must be in the past or present")
        LocalDate dateOfBirth,

        @Size(max = 500, message = "profilePhotoUrl must not exceed 500 characters")
        String profilePhotoUrl,

        @Email(message = "email format is invalid")
        @Size(max = 150, message = "email must not exceed 150 characters")
        String email,

        @Size(max = 30, message = "phoneNumber must not exceed 30 characters")
        String phoneNumber,

        @Size(max = 80, message = "username must not exceed 80 characters")
        String username,

        UUID departmentId,

        @Valid
        WorkerDocumentInput workerDocProfile
) {
    public record WorkerDocumentInput(
            DocumentType type,

            @Size(max = 150, message = "title must not exceed 150 characters")
            String title,
            Boolean isRequired,

            @Size(max = 500, message = "documentUrl must not exceed 500 characters")
            String documentUrl,

            @Size(max = 100, message = "documentNumber must not exceed 100 characters")
            String documentNumber,
            Boolean hasExpiration,
            LocalDate expirationDate,

            @Size(max = 120, message = "issuedBy must not exceed 120 characters")
            String issuedBy,
            LocalDate issuedDate,

            @Size(max = 1000, message = "notes must not exceed 1000 characters")
            String notes
    ) {
    }
}
