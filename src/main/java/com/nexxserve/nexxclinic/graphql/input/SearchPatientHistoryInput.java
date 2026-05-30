package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.time.YearMonth;

public record SearchPatientHistoryInput(
        Integer year,

        @Min(value = 1, message = "month must be between 1 and 12")
        @Max(value = 12, message = "month must be between 1 and 12")
        Integer month,

        @Min(value = 1, message = "day must be between 1 and 31")
        @Max(value = 31, message = "day must be between 1 and 31")
        Integer day,

        LocalDate startDate,

        LocalDate endDate,

        YearMonth startMonth,

        YearMonth endMonth,

        Integer startYear,

        Integer endYear,

        @Min(value = 0, message = "page must be 0 or greater")
        Integer page,

        @Min(value = 1, message = "size must be at least 1")
        Integer size
) {
}