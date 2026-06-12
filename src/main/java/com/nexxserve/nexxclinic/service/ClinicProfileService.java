package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.ClinicContactDto;
import com.nexxserve.nexxclinic.dto.out.ClinicProfileDto;
import com.nexxserve.nexxclinic.entity.ClinicContact;
import com.nexxserve.nexxclinic.entity.ClinicMetadata;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.graphql.input.ClinicContactInput;
import com.nexxserve.nexxclinic.graphql.input.ClinicMetadataDto;
import com.nexxserve.nexxclinic.graphql.input.UpdateClinicProfileInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.repository.ClinicProfileRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClinicProfileService {

    private final ClinicProfileRepository clinicProfileRepository;

    public ClinicProfileService(ClinicProfileRepository clinicProfileRepository) {
        this.clinicProfileRepository = clinicProfileRepository;
    }

    // =========================
    // GET PROFILE
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<ClinicProfileDto> clinicProfile() {

        return clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                .map(profile ->
                        ApiResponse.success(
                                "Clinic profile fetched.",
                                toDto(profile)
                        )
                )
                .orElseGet(() ->
                        ApiResponse.success(
                                "Clinic profile not configured.",
                                null
                        )
                );
    }

    // =========================
    // UPDATE PROFILE
    // =========================
    @Transactional
    public ApiResponse<ClinicProfileDto> updateClinicProfile(UpdateClinicProfileInput input) {

        if (input == null) {
            return ApiResponse.error("Input is required.");
        }

        ClinicProfile profile = resolveClinicProfile();

        if (input.name() != null) profile.setName(blankToNull(input.name()));
        if (input.username() != null) profile.setUsername(blankToNull(input.username()));
        if (input.address() != null) profile.setAddress(blankToNull(input.address()));
        if (input.logoUrl() != null) profile.setLogoUrl(blankToNull(input.logoUrl()));
        if (input.tinNumber() != null) profile.setTinNumber(blankToNull(input.tinNumber()));

        if (input.contacts() != null) {
            profile.setContacts(toClinicContacts(input.contacts()));
        }

        if (input.metadata() != null) {
            updateMetadata(profile, input.metadata());
        }

        ClinicProfile saved = clinicProfileRepository.save(profile);

        return ApiResponse.success(
                "Clinic profile updated.",
                toDto(saved)
        );
    }

    // =========================
    // DELETE PROFILE
    // =========================
    @Transactional
    public ApiResponse<Boolean> deleteClinicProfile() {

        clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                .ifPresent(clinicProfileRepository::delete);

        return ApiResponse.success("Clinic profile deleted.", true);
    }

    // =========================
    // DTO MAPPER
    // =========================
    private ClinicProfileDto toDto(ClinicProfile profile) {

        List<ClinicContactDto> contacts = resolveContacts(profile)
                .stream()
                .map(this::toContactDto)
                .toList();

        List<ClinicMetadataDto> metadata = profile.getMetadata() == null
                ? List.of()
                : profile.getMetadata()
                .stream()
                .map(this::toMetadataDto)
                .toList();

        return new ClinicProfileDto(
                profile.getId(),
                profile.getName(),
                profile.getUsername(),
                profile.getAddress(),
                contacts,
                profile.getTinNumber(),
                profile.getLogoUrl(),
                metadata,
                profile.getCreatedAt() == null ? null : profile.getCreatedAt().toString(),
                profile.getUpdatedAt() == null ? null : profile.getUpdatedAt().toString()
        );
    }

    private ClinicContactDto toContactDto(ClinicContact contact) {
        return new ClinicContactDto(
                contact.getContactType(),
                contact.getValue(),
                contact.getDescription()
        );
    }

    private ClinicMetadataDto toMetadataDto(ClinicMetadata metadata) {
        return new ClinicMetadataDto(
                metadata.getKey(),
                metadata.getValue()
        );
    }

    // =========================
    // HELPERS
    // =========================
    private List<ClinicContact> toClinicContacts(List<ClinicContactInput> inputs) {
        return inputs.stream()
                .map(input -> {
                    ClinicContact contact = new ClinicContact();
                    contact.setContactType(input.contactType());
                    contact.setValue(blankToNull(input.value()));
                    contact.setDescription(blankToNull(input.description()));
                    return contact;
                })
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private void updateMetadata(ClinicProfile profile, List<ClinicMetadataDto> inputs) {

        Map<String, ClinicMetadata> current = profile.getMetadata() == null
                ? new HashMap<>()
                : profile.getMetadata()
                .stream()
                .collect(Collectors.toMap(ClinicMetadata::getKey, m -> m));

        for (ClinicMetadataDto input : inputs) {

            String key = blankToNull(input.key());
            String value = blankToNull(input.value());

            if (key == null) continue;

            if (value == null) {
                current.remove(key);
            } else {
                ClinicMetadata meta = current.getOrDefault(key, new ClinicMetadata());
                meta.setKey(key);
                meta.setValue(value);
                current.put(key, meta);
            }
        }

        profile.setMetadata(new ArrayList<>(current.values()));
    }

    private List<ClinicContact> resolveContacts(ClinicProfile profile) {
        return profile.getContacts() == null
                ? new ArrayList<>()
                : profile.getContacts();
    }

    private ClinicProfile resolveClinicProfile() {

        return clinicProfileRepository.findFirstByOrderByCreatedAtAsc()
                .orElseGet(() -> {
                    ClinicProfile p = new ClinicProfile();
                    return clinicProfileRepository.save(p);
                });
    }

    private String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}