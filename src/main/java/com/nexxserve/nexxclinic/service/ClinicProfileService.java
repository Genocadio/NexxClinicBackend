package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicProfileService {

    private static final String EMPTY_JSON = "{}";
    private final ClinicProfileRepository clinicProfileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClinicProfileService(ClinicProfileRepository clinicProfileRepository) {
        this.clinicProfileRepository = clinicProfileRepository;
    }

    @Transactional(readOnly = true)
    public ApiResponse clinicProfile() {
        return clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                .map(profile -> ApiResponse.success("Clinic profile fetched.", clinicProfileToMap(profile)))
                .orElseGet(() -> ApiResponse.success("Clinic profile not configured.", null));
    }

    @Transactional
    public ApiResponse updateClinicProfile(UpdateClinicProfileInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        ClinicProfile profile = resolveClinicProfile();

        if (input.name() != null) {
            profile.setName(blankToNull(input.name()));
        }
        if (input.address() != null) {
            profile.setAddress(blankToNull(input.address()));
        }
        if (input.logoUrl() != null) {
            profile.setLogoUrl(blankToNull(input.logoUrl()));
        }
        if (input.tinNumber() != null) {
            profile.setTinNumber(blankToNull(input.tinNumber()));
        }
        if (input.contacts() != null) {
            ApiResponse validation = applyJsonField(profile::setContacts, input.contacts(), "contacts");
            if (validation != null) {
                return validation;
            }
        }
        if (input.metadata() != null) {
            ApiResponse validation = applyJsonField(profile::setMetadata, input.metadata(), "metadata");
            if (validation != null) {
                return validation;
            }
        }

        ClinicProfile saved = clinicProfileRepository.save(profile);
        return ApiResponse.success("Clinic profile updated.", clinicProfileToMap(saved));
    }

    @Transactional
    public ApiResponse deleteClinicProfile() {
        Optional<ClinicProfile> profileOptional = clinicProfileRepository.findFirstByOrderByCreatedAtAsc();
        profileOptional.ifPresent(clinicProfileRepository::delete);
        return ApiResponse.success("Clinic profile deleted.", true);
    }

    private ClinicProfile resolveClinicProfile() {
        return clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    ClinicProfile profile = new ClinicProfile();
                    profile.setContacts(EMPTY_JSON);
                    profile.setMetadata(EMPTY_JSON);
                    return clinicProfileRepository.save(profile);
                });
    }

    private ApiResponse applyJsonField(Consumer<String> setter, Object value, String fieldName) {
        try {
            setter.accept(objectMapper.writeValueAsString(value));
            return null;
        } catch (JsonProcessingException ex) {
            return ApiResponse.error(fieldName + " must be valid JSON.", "VALIDATION_ERROR");
        }
    }

    private Map<String, Object> clinicProfileToMap(ClinicProfile profile) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", profile.getId());
        data.put("name", profile.getName());
        data.put("address", profile.getAddress());
        data.put("contacts", parseJson(profile.getContacts()));
        data.put("tinNumber", profile.getTinNumber());
        data.put("logoUrl", profile.getLogoUrl());
        data.put("metadata", parseJson(profile.getMetadata()));
        data.put("createdAt", profile.getCreatedAt().toString());
        data.put("updatedAt", profile.getUpdatedAt().toString());
        return data;
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<String, Object>();
        }

        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException ex) {
            return new HashMap<String, Object>();
        }
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
