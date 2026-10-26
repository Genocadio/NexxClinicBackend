package com.nexxserve.nexxclinic.graphql.input;

import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.VisitStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record SearchVisitsInput(
        String visitDate,

        String fromDate,

        String toDate,

        Integer recentDays,

        Boolean activeOnly,

        VisitStatus status,

        @Size(max = 200, message = "patientName must not exceed 200 characters")
        String patientName,

        String answersFilter,

        Gender gender,

        String dob,

        Integer minAge,

        Integer maxAge,

        String insuranceId,

        Boolean isPrivateOnly,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
    public SearchVisitsInput(
            String visitDate,
            String fromDate,
            String toDate,
            Integer recentDays,
            Boolean activeOnly,
            VisitStatus status,
            String patientName,
            Integer page,
            Integer size
    ) {
        this(visitDate, fromDate, toDate, recentDays, activeOnly, status, patientName, null, null, null, null, null, null, null, page, size);
    }

    public SearchVisitsInput(
            String visitDate,
            String fromDate,
            String toDate,
            Integer recentDays,
            Boolean activeOnly,
            VisitStatus status,
            String patientName,
            String answersFilter,
            Gender gender,
            String dob,
            Integer minAge,
            Integer maxAge,
            Integer page,
            Integer size
    ) {
        this(visitDate, fromDate, toDate, recentDays, activeOnly, status, patientName, answersFilter, gender, dob, minAge, maxAge, null, null, page, size);
    }
}
