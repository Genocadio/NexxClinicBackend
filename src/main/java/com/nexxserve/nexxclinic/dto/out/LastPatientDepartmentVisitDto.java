package com.nexxserve.nexxclinic.dto.out;

public record LastPatientDepartmentVisitDto(
        VisitDto lastVisit,
        LastDepartmentVisitInfoDto lastDepartmentVisit
) {}
