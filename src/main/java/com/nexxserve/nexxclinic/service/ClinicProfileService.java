package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.entity.ClinicContact;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.graphql.input.ClinicContactInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
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
            profile.setContacts(toClinicContacts(input.contacts()));
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
                .map(this::migrateLegacyContactsIfNeeded)
                .orElseGet(() -> {
                    ClinicProfile profile = new ClinicProfile();
                    profile.setMetadata(EMPTY_JSON);
                    return clinicProfileRepository.save(profile);
                });
    }

    private ClinicProfile migrateLegacyContactsIfNeeded(ClinicProfile profile) {
        if (profile.getContacts() != null && !profile.getContacts().isEmpty()) {
            return profile;
        }

        List<ClinicContact> migratedContacts = parseLegacyContacts(profile.getLegacyContactsJson());
        if (migratedContacts.isEmpty()) {
            return profile;
        }

        profile.setContacts(migratedContacts);
        return clinicProfileRepository.save(profile);
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
        data.put("contacts", resolveContacts(profile).stream().map(this::clinicContactToMap).collect(Collectors.toList()));
        data.put("tinNumber", profile.getTinNumber());
        data.put("logoUrl", profile.getLogoUrl());
        data.put("metadata", parseJson(profile.getMetadata()));
        data.put("createdAt", profile.getCreatedAt().toString());
        data.put("updatedAt", profile.getUpdatedAt().toString());
        return data;
    }

    private List<ClinicContact> resolveContacts(ClinicProfile profile) {
        if (profile.getContacts() != null && !profile.getContacts().isEmpty()) {
            return profile.getContacts();
        }

        return parseLegacyContacts(profile.getLegacyContactsJson());
    }

    private Map<String, Object> clinicContactToMap(ClinicContact contact) {
        Map<String, Object> data = new HashMap<>();
        data.put("contactType", contact.getContactType() == null ? null : contact.getContactType().name());
        data.put("value", contact.getValue());
        data.put("description", contact.getDescription());
        return data;
    }

    private List<ClinicContact> toClinicContacts(List<ClinicContactInput> inputs) {
        return inputs.stream().map(this::toClinicContact).collect(Collectors.toList());
    }

    private List<ClinicContact> parseLegacyContacts(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }

        try {
            List<?> rawContacts = objectMapper.readValue(json, List.class);
            List<ClinicContact> contacts = new ArrayList<>();
            for (Object rawContact : rawContacts) {
                if (!(rawContact instanceof Map<?, ?> contactMap)) {
                    continue;
                }

                Object typeValue = contactMap.get("contactType");
                Object valueValue = contactMap.get("value");
                if (typeValue == null || valueValue == null) {
                    continue;
                }

                ClinicContact contact = new ClinicContact();
                contact.setContactType(com.nexxserve.nexxclinic.model.ClinicContactType.valueOf(typeValue.toString()));
                contact.setValue(valueValue.toString());
                Object descriptionValue = contactMap.get("description");
                contact.setDescription(descriptionValue == null ? null : descriptionValue.toString());
                contacts.add(contact);
            }
            return contacts;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private ClinicContact toClinicContact(ClinicContactInput input) {
        ClinicContact contact = new ClinicContact();
        contact.setContactType(input.contactType());
        contact.setValue(blankToNull(input.value()));
        contact.setDescription(blankToNull(input.description()));
        return contact;
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
