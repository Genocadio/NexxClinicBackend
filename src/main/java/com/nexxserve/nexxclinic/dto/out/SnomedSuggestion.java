package com.nexxserve.nexxclinic.dto.out;

import java.util.List;

public record SnomedSuggestion(
        String id,
        String preferred,
        List<String> synonyms,
        String fsn,
        String type,
        List<String> bodySystem,
        boolean common,
        double score
) {
}
