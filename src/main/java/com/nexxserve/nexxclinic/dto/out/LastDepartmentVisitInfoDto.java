package com.nexxserve.nexxclinic.dto.out;

import java.util.UUID;

public record LastDepartmentVisitInfoDto(
        UUID visitId,
        VisitDepartmentDto visitDepartment
) {}
