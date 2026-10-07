package com.nexxserve.nexxclinic.dto.out;

import java.util.List;

public record Icd11SearchResult(
        String query,
        String lang,
        int limit,
        int total,
        List<Icd11Suggestion> results,
        String source
) {
}
