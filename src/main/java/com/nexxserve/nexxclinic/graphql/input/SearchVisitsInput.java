package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.VisitStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SearchVisitsInput(
        LocalDate visitDate,

        VisitStatus status,

        @Size(max = 200, message = "patientName must not exceed 200 characters")
        String patientName,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}
