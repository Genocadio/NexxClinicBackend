package com.nexxserve.nexxclinic.dto.out;

public record FileInfoResponse(
        String fileName,
        String originalFileName,
        String url,
        String contentType,
        long size
) {
}