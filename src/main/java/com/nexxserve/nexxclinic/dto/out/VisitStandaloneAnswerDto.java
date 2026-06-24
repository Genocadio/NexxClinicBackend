package com.nexxserve.nexxclinic.dto.out;

public record VisitStandaloneAnswerDto(
    StandaloneFormAnswerDto answer,
    VisitDepartmentDto visitDepartment
) {}
