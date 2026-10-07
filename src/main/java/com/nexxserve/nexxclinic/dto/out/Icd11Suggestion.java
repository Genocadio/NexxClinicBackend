package com.nexxserve.nexxclinic.dto.out;

public record Icd11Suggestion(
        String id,
        String code,
        String title,
        String chapter,
        double score
) {
}
