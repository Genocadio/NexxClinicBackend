package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceProviderInput;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceProvidersInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceProviderInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceProviderService {

    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;

    public InsuranceProviderService(
            InsuranceProviderRepository insuranceProviderRepository,
            PatientInsuranceRepository patientInsuranceRepository
    ) {
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
    }

    @Transactional
    public ApiResponse createInsuranceProvider(CreateInsuranceProviderInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        String insuranceName = requiredTrim(input.insuranceName());
        if (insuranceName == null) {
            return ApiResponse.error("insuranceName is required.", "VALIDATION_ERROR");
        }

        if (insuranceProviderRepository.existsByInsuranceNameIgnoreCase(insuranceName)) {
            return ApiResponse.error("Insurance name already exists.", "DUPLICATE_INSURANCE_NAME");
        }

        String acronym = blankToNull(input.acronym());
        if (acronym != null && insuranceProviderRepository.existsByAcronymIgnoreCase(acronym)) {
            return ApiResponse.error("Acronym already exists.", "DUPLICATE_ACRONYM");
        }

        Integer coverage = input.defaultCoveragePercentage();
        if (coverage == null || coverage < 0 || coverage > 100) {
            return ApiResponse.error("defaultCoveragePercentage must be between 0 and 100.", "VALIDATION_ERROR");
        }

        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName(insuranceName);
        provider.setAcronym(acronym);
        provider.setDefaultCoveragePercentage(coverage);
        provider.setSupportedByClinic(input.supportedByClinic() == null || input.supportedByClinic());
        provider.setIconUrl(blankToNull(input.iconUrl()));

        InsuranceProvider saved = insuranceProviderRepository.save(provider);
        return ApiResponse.success("Insurance provider created.", insuranceProviderToMap(saved));
    }

    @Transactional
    public ApiResponse updateInsuranceProvider(UUID insuranceProviderId, UpdateInsuranceProviderInput input) {
        if (insuranceProviderId == null || input == null) {
            return ApiResponse.error("insuranceProviderId and input are required.", "VALIDATION_ERROR");
        }

        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(insuranceProviderId);
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        InsuranceProvider provider = providerOptional.get();

        if (input.insuranceName() != null) {
            String insuranceName = requiredTrim(input.insuranceName());
            if (insuranceName == null) {
                return ApiResponse.error("insuranceName cannot be blank.", "VALIDATION_ERROR");
            }
            if (insuranceProviderRepository.existsByInsuranceNameIgnoreCaseAndIdNot(insuranceName, provider.getId())) {
                return ApiResponse.error("Insurance name already exists.", "DUPLICATE_INSURANCE_NAME");
            }
            provider.setInsuranceName(insuranceName);
        }

        if (input.acronym() != null) {
            String acronym = blankToNull(input.acronym());
            if (acronym != null && insuranceProviderRepository.existsByAcronymIgnoreCaseAndIdNot(acronym, provider.getId())) {
                return ApiResponse.error("Acronym already exists.", "DUPLICATE_ACRONYM");
            }
            provider.setAcronym(acronym);
        }

        if (input.defaultCoveragePercentage() != null) {
            if (input.defaultCoveragePercentage() < 0 || input.defaultCoveragePercentage() > 100) {
                return ApiResponse.error("defaultCoveragePercentage must be between 0 and 100.", "VALIDATION_ERROR");
            }
            provider.setDefaultCoveragePercentage(input.defaultCoveragePercentage());
        }

        if (input.supportedByClinic() != null) {
            provider.setSupportedByClinic(input.supportedByClinic());
        }

        if (input.iconUrl() != null) {
            provider.setIconUrl(blankToNull(input.iconUrl()));
        }

        InsuranceProvider saved = insuranceProviderRepository.save(provider);
        return ApiResponse.success("Insurance provider updated.", insuranceProviderToMap(saved));
    }

    @Transactional
    public ApiResponse deleteInsuranceProvider(UUID insuranceProviderId) {
        if (insuranceProviderId == null) {
            return ApiResponse.error("insuranceProviderId is required.", "VALIDATION_ERROR");
        }

        if (!insuranceProviderRepository.existsById(insuranceProviderId)) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        if (patientInsuranceRepository.existsByInsuranceProviderId(insuranceProviderId)) {
            return ApiResponse.error(
                    "Insurance provider is in use by patient insurance records and cannot be deleted.",
                    "INSURANCE_IN_USE"
            );
        }

        insuranceProviderRepository.deleteById(insuranceProviderId);
        return ApiResponse.success("Insurance provider deleted.", true);
    }

    @Transactional(readOnly = true)
    public ApiResponse insuranceProvider(UUID insuranceProviderId) {
        if (insuranceProviderId == null) {
            return ApiResponse.error("insuranceProviderId is required.", "VALIDATION_ERROR");
        }

        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(insuranceProviderId);
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Insurance provider fetched.", insuranceProviderToMap(providerOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse insuranceProviders(SearchInsuranceProvidersInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<InsuranceProvider> spec = (root, queryDef, builder) -> builder.conjunction();

        String query = input == null ? null : blankToNull(input.query());
        if (query != null) {
            String normalizedQuery = query.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.or(
                            builder.like(builder.lower(root.get("insuranceName")), "%" + normalizedQuery + "%"),
                            builder.like(builder.lower(root.get("acronym")), "%" + normalizedQuery + "%")
                    )
            );
        }

        if (input != null && input.supportedByClinic() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("supportedByClinic"), input.supportedByClinic()));
        }

        Page<InsuranceProvider> providerPage = insuranceProviderRepository.findAll(spec, pageable);
        List<Map<String, Object>> providers = providerPage.getContent().stream().map(this::insuranceProviderToMap).toList();

        return ApiResponse.success(
                "Insurance providers fetched.",
            providers,
            paginationToMap(providerPage)
        );
    }

    private Map<String, Object> insuranceProviderToMap(InsuranceProvider provider) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", provider.getId());
        data.put("insuranceName", provider.getInsuranceName());
        data.put("acronym", provider.getAcronym());
        data.put("defaultCoveragePercentage", provider.getDefaultCoveragePercentage());
        data.put("supportedByClinic", provider.isSupportedByClinic());
        data.put("iconUrl", provider.getIconUrl());
        data.put("createdAt", provider.getCreatedAt());
        data.put("updatedAt", provider.getUpdatedAt());
        return data;
    }

    private String requiredTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int normalizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    private int normalizeSize(Integer size) {
        if (size == null || size <= 0) {
            return 20;
        }
        return Math.min(size, 100);
    }

    private Map<String, Object> paginationToMap(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", page.getTotalElements());
        pagination.put("perPage", page.getSize());
        pagination.put("currentPage", page.getNumber());
        pagination.put("totalPages", page.getTotalPages());
        return pagination;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
