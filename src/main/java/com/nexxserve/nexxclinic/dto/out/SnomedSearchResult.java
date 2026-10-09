package com.nexxserve.nexxclinic.dto.out;

import java.util.List;

public record SnomedSearchResult(
        String query,
        int limit,
        int total,
        List<SnomedSuggestion> results,
        String source,
        String releaseVersion
) {
}
