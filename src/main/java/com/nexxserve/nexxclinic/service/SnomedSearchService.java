package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.NexxIcdProperties;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.SnomedSearchResult;
import java.net.URI;
import java.util.Locale;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Proxies SNOMED CT sign/symptom autosuggestion to the NexxICD wrapper's
 * {@code GET /api/v1/snomed/search} endpoint.
 */
@Service
public class SnomedSearchService {

    private static final Logger log = LoggerFactory.getLogger(SnomedSearchService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final Set<String> SUPPORTED_TYPES = Set.of("reported", "physical", "finding");

    private final NexxIcdProperties properties;
    private final RestClient restClient;

    @Autowired
    public SnomedSearchService(NexxIcdProperties properties) {
        this(properties, RestClient.create());
    }

    SnomedSearchService(NexxIcdProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public ApiResponse<SnomedSearchResult> search(
            String query,
            String type,
            Integer requestedLimit
    ) {
        if (!StringUtils.hasText(properties.getUrl())) {
            return ApiResponse.error("Symptom search is not configured. Set NEXXICD_URL.");
        }
        if (!StringUtils.hasText(query)) {
            return ApiResponse.error("Search query must not be blank.");
        }

        String normalizedType = type == null || type.isBlank()
            ? null
            : type.trim().toLowerCase(Locale.ROOT);
        if (normalizedType != null && !SUPPORTED_TYPES.contains(normalizedType)) {
            return ApiResponse.error("Type must be one of reported, physical or finding.");
        }

        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            return ApiResponse.error("Limit must be between 1 and " + MAX_LIMIT + ".");
        }

        try {
            SnomedSearchResult result = restClient.get()
                .uri(searchUri(query.trim(), normalizedType, limit))
                .retrieve()
                .body(SnomedSearchResult.class);
            if (result == null) {
                return ApiResponse.error("Symptom search returned an empty response.");
            }
            return ApiResponse.success("Symptom suggestions fetched.", result);
        } catch (RestClientException e) {
            log.warn("Symptom search request failed: {}", e.getMessage());
            return ApiResponse.error("Symptom search service is unavailable.");
        }
    }

    private URI searchUri(String query, String type, int limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getUrl().trim())
            .path("/api/v1/snomed/search")
            .queryParam("q", query)
            .queryParam("limit", limit);
        if (type != null) {
            builder.queryParam("type", type);
        }
        return builder.build().encode().toUri();
    }
}
