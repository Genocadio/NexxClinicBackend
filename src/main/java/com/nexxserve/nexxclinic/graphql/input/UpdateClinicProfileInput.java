package com.nexxserve.nexxclinic.graphql.input;

import jakarta.validation.Valid;
import java.util.List;

public record UpdateClinicProfileInput(
        String name,
        String address,
        String logoUrl,
        String tinNumber,
        @Valid
        List<ClinicContactInput> contacts,
        Object metadata
) {
}
