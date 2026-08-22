package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.dto.out.PaginationDto;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceProviderInput;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceProvidersInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceProviderInput;
import com.nexxserve.nexxclinic.mappers.out.InsuranceProviderMapper;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Service
public class InsuranceProviderService {

    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final InsuranceProviderMapper mapper;

    public InsuranceProviderService(
            InsuranceProviderRepository insuranceProviderRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            InsuranceProviderMapper mapper
    ) {
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.mapper = mapper;
    }

    // =========================
    // CREATE
    // =========================
    @Transactional
    public ApiResponse<InsuranceProviderDto> createInsuranceProvider(CreateInsuranceProviderInput input) {

        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        String insuranceName = requiredTrim(input.insuranceName());
        if (insuranceName == null) {
            return ApiResponse.error("insuranceName is required.");
        }

        if (insuranceProviderRepository.existsByInsuranceNameIgnoreCase(insuranceName)) {
            return ApiResponse.error("Insurance name already exists.");
        }

        String acronym = blankToNull(input.acronym());
        if (acronym != null && insuranceProviderRepository.existsByAcronymIgnoreCase(acronym)) {
            return ApiResponse.error("Acronym already exists.");
        }

        Integer coverage = input.defaultPatientSharePercentage();
        if (coverage == null || coverage < 0 || coverage > 100) {
            return ApiResponse.error("Coverage must be 0-100.");
        }

        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName(insuranceName);
        provider.setAcronym(acronym);
        provider.setDefaultPatientSharePercentage(coverage);
        provider.setSupportedByClinic(input.supportedByClinic() == null || input.supportedByClinic());
        provider.setIconUrl(blankToNull(input.iconUrl()));

        InsuranceProvider saved = insuranceProviderRepository.save(provider);

        return ApiResponse.success("Insurance provider created.", mapper.toDto(saved));
    }

    // =========================
    // UPDATE
    // =========================
    @Transactional
    public ApiResponse<InsuranceProviderDto> updateInsuranceProvider(
            UUID id,
            UpdateInsuranceProviderInput input
    ) {

        if (id == null || input == null) {
            return ApiResponse.error("id and input are required.");
        }

        // S8 fix: an unknown id must be a clean error, not a RuntimeException -> 500.
        Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(id);
        if (providerOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found");
        }
        InsuranceProvider provider = providerOptional.get();

        if (input.insuranceName() != null) {
            String name = requiredTrim(input.insuranceName());
            provider.setInsuranceName(name);
        }

        if (input.acronym() != null) {
            provider.setAcronym(blankToNull(input.acronym()));
        }

        if (input.defaultPatientSharePercentage() != null) {
            provider.setDefaultPatientSharePercentage(input.defaultPatientSharePercentage());
        }

        if (input.supportedByClinic() != null) {
            provider.setSupportedByClinic(input.supportedByClinic());
        }

        if (input.iconUrl() != null) {
            provider.setIconUrl(blankToNull(input.iconUrl()));
        }

        InsuranceProvider saved = insuranceProviderRepository.save(provider);

        return ApiResponse.success("Insurance provider updated.", mapper.toDto(saved));
    }

    // =========================
    // GET ONE
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<InsuranceProviderDto> insuranceProvider(UUID id) {

        return insuranceProviderRepository.findById(id)
                .map(p -> ApiResponse.success("Insurance provider fetched.", mapper.toDto(p)))
                .orElseGet(() -> ApiResponse.error("Not found."));
    }

    // =========================
    // LIST
    // =========================
    @Transactional(readOnly = true)
    public ApiResponse<List<InsuranceProviderDto>> insuranceProviders(SearchInsuranceProvidersInput input) {

        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<InsuranceProvider> spec = (root, query, cb) -> cb.conjunction();

        if (input != null && input.query() != null) {
            String q = input.query().toLowerCase();
            spec = spec.and((root, query1, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("insuranceName")), "%" + q + "%"),
                            cb.like(cb.lower(root.get("acronym")), "%" + q + "%")
                    )
            );
        }

        if (input != null && input.supportedByClinic() != null) {
            spec = spec.and((root, query1, cb) ->
                    cb.equal(root.get("supportedByClinic"), input.supportedByClinic())
            );
        }

        Page<InsuranceProvider> pageResult = insuranceProviderRepository.findAll(spec, pageable);
        List<InsuranceProviderDto> dtoList = pageResult.getContent().stream().map(mapper::toDto).toList();

        return ApiResponse.success(
                "Insurance providers fetched.",
                dtoList,
                new PaginationDto(
                        pageResult.getTotalElements(),
                        pageResult.getSize(),
                        pageResult.getNumber(),
                        pageResult.getTotalPages()
                )
        );
    }

    // =========================
    // DELETE
    // =========================
    @Transactional
    public ApiResponse<Boolean> deleteInsuranceProvider(UUID id) {

        if (!insuranceProviderRepository.existsById(id)) {
            return ApiResponse.error("Not found.");
        }

        if (patientInsuranceRepository.existsByInsuranceProviderId(id)) {
            return ApiResponse.error("Provider in use.");
        }

        insuranceProviderRepository.deleteById(id);

        return ApiResponse.success("Deleted.", true);
    }

    // =========================
    // HELPERS
    // =========================
    private String requiredTrim(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private int normalizePage(Integer page) {
        return (page == null || page < 0) ? 0 : page;
    }

    private int normalizeSize(Integer size) {
        return (size == null || size <= 0) ? 20 : Math.min(size, 100);
    }
}