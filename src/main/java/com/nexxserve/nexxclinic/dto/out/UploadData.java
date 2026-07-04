package com.nexxserve.nexxclinic.dto.out;

import java.util.UUID;

public record UploadData(
        UUID id,
        String url
) {
}
