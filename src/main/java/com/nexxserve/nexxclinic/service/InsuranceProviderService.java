package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.dto.out.PaginationDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.graphql.input.InsuranceCoverageInput;
import com.nexxserve.nexxclinic.graphql.input.CreateInsuranceProviderInput;
import com.nexxserve.nexxclinic.graphql.input.SearchInsuranceProvidersInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceProviderInput;
import com.nexxserve.nexxclinic.mappers.out.InsuranceProviderMapper;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsuranceProviderService {

    private final InsuranceProviderRepository insuranceProviderRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final DepartmentRepository departmentRepository;
    private final InsuranceProviderMapper mapper;

    public InsuranceProviderService(
            InsuranceProviderRepository insuranceProviderRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            DepartmentRepository departmentRepository,
            InsuranceProviderMapper mapper
    ) {
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.departmentRepository = departmentRepository;
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

        // Validate coverages
        List<InsuranceCoverageInput> coverageInputs = input.coverages();
        if (coverageInputs == null || coverageInputs.isEmpty()) {
            return ApiResponse.error("At least one coverage is required.");
        }

        // Check at least one base coverage (no conditions)
        boolean hasBase = coverageInputs.stream()
            .anyMatch(c -> c.departmentId() == null && c.encounterType() == null);
        if (!hasBase) {
            return ApiResponse.error("At least one base coverage (no department or encounter type) is required.");
        }

        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName(insuranceName);
        provider.setAcronym(acronym);
        provider.setSupportedByClinic(input.supportedByClinic() == null || input.supportedByClinic());
        provider.setIconUrl(blankToNull(input.iconUrl()));

        // Create coverages
        List<InsuranceCoverage> coverages = new ArrayList<>();
        for (InsuranceCoverageInput covInput : coverageInputs) {
            InsuranceCoverage cov = buildCoverage(covInput, provider);
            if (cov == null) {
                return ApiResponse.error("Invalid coverage: department not found.");
            }
            coverages.add(cov);
        }
        provider.setCoverages(coverages);

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

        if (input.supportedByClinic() != null) {
            provider.setSupportedByClinic(input.supportedByClinic());
        }

        if (input.iconUrl() != null) {
            provider.setIconUrl(blankToNull(input.iconUrl()));
        }

        // Replace coverages if provided
        if (input.coverages() != null) {
            List<InsuranceCoverageInput> coverageInputs = input.coverages();
            if (coverageInputs.isEmpty()) {
                return ApiResponse.error("At least one coverage is required.");
            }

            boolean hasBase = coverageInputs.stream()
                .anyMatch(c -> c.departmentId() == null && c.encounterType() == null);
            if (!hasBase) {
                return ApiResponse.error("At least one base coverage (no department or encounter type) is required.");
            }

            // Clear existing coverages (orphanRemoval handles deletion)
            provider.getCoverages().clear();

            // Add new coverages
            for (InsuranceCoverageInput covInput : coverageInputs) {
                InsuranceCoverage cov = buildCoverage(covInput, provider);
                if (cov == null) {
                    return ApiResponse.error("Invalid coverage: department not found.");
                }
                provider.addCoverage(cov);
            }
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

    private InsuranceCoverage buildCoverage(InsuranceCoverageInput input, InsuranceProvider provider) {
        if (input.patientSharePercentage() == null || input.patientSharePercentage() < 0 || input.patientSharePercentage() > 100) {
            return null;
        }

        Department department = null;
        if (input.departmentId() != null) {
            Optional<Department> deptOpt = departmentRepository.findById(input.departmentId());
            if (deptOpt.isEmpty()) {
                return null;
            }
            department = deptOpt.get();
        }

        InsuranceCoverage coverage = new InsuranceCoverage();
        coverage.setInsuranceProvider(provider);
        coverage.setDepartment(department);
        coverage.setEncounterType(input.encounterType());
        coverage.setPatientSharePercentage(input.patientSharePercentage());
        return coverage;
    }

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
