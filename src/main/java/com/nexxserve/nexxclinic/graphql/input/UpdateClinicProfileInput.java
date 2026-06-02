package com.nexxserve.nexxclinic.graphql.input;

public record UpdateClinicProfileInput(
        String name,
        String address,
        String logoUrl,
        String tinNumber,
        Object contacts,
        Object metadata
) {
}
