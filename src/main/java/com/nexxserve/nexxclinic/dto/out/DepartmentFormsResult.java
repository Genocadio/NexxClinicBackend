package com.nexxserve.nexxclinic.dto.out;

import java.util.List;

public record DepartmentFormsResult(
        List<DepartmentFormDto> forms,
        StandaloneFormDto defaultForm
) {}