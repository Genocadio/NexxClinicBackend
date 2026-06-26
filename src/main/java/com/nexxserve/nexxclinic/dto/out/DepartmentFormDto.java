package com.nexxserve.nexxclinic.dto.out;

public record DepartmentFormDto(
        StandaloneFormDto form,
        Boolean isDefault
) {}