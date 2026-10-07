package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.NexxIcdProperties;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.Icd11SearchResult;
import java.net.URI;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class Icd11SearchService {

    private static final Logger log = LoggerFactory.getLogger(Icd11SearchService.class);
    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    private final NexxIcdProperties properties;
    private final RestClient restClient;

    @Autowired
    public Icd11SearchService(NexxIcdProperties properties) {
        this(properties, RestClient.create());
    }

    Icd11SearchService(NexxIcdProperties properties, RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public ApiResponse<Icd11SearchResult> search(
            String query,
            String language,
            Integer requestedLimit,
            String chapter
    ) {
        if (!StringUtils.hasText(properties.getUrl())) {
            return ApiResponse.error("ICD-11 search is not configured. Set NEXXICD_URL.");
        }
        if (!StringUtils.hasText(query)) {
            return ApiResponse.error("Search query must not be blank.");
        }

        String lang = language == null || language.isBlank()
            ? "en"
            : language.trim().toLowerCase(Locale.ROOT);
        if (!lang.equals("en") && !lang.equals("fr")) {
            return ApiResponse.error("Language must be en or fr.");
        }

        int limit = requestedLimit == null ? DEFAULT_LIMIT : requestedLimit;
        if (limit < 1 || limit > MAX_LIMIT) {
            return ApiResponse.error("Limit must be between 1 and " + MAX_LIMIT + ".");
        }

        String normalizedChapter = chapter == null || chapter.isBlank()
            ? null
            : chapter.trim().toUpperCase(Locale.ROOT);
        if (normalizedChapter != null
                && !normalizedChapter.matches("^[0-9]{2}$|^[A-Z]{1,2}$")) {
            return ApiResponse.error("Chapter must be a 2-digit code or a letter.");
        }

        try {
            Icd11SearchResult result = restClient.get()
                .uri(searchUri(query.trim(), lang, limit, normalizedChapter))
                .retrieve()
                .body(Icd11SearchResult.class);
            if (result == null) {
                return ApiResponse.error("ICD-11 search returned an empty response.");
            }
            return ApiResponse.success("ICD-11 suggestions fetched.", result);
        } catch (RestClientException e) {
            log.warn("ICD-11 search request failed: {}", e.getMessage());
            return ApiResponse.error("ICD-11 search service is unavailable.");
        }
    }

    private URI searchUri(
            String query,
            String language,
            int limit,
            String chapter
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(properties.getUrl().trim())
            .path("/api/v1/search")
            .queryParam("q", query)
            .queryParam("lang", language)
            .queryParam("limit", limit);
        if (chapter != null) {
            builder.queryParam("chapter", chapter);
        }
        return builder.build().encode().toUri();
    }
}
