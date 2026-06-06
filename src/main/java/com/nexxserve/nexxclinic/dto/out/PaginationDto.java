package com.nexxserve.nexxclinic.dto.out;

public record PaginationDto(long total, int perPage, int currentPage, int totalPages) {
}
