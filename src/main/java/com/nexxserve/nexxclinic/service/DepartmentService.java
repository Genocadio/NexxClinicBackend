package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsurancePolicy;
import com.nexxserve.nexxclinic.entity.DepartmentProfile;
import com.nexxserve.nexxclinic.entity.DepartmentProfileProduct;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.graphql.input.CreateDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.DepartmentProfileInput;
import com.nexxserve.nexxclinic.graphql.input.SearchDepartmentsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateDepartmentInput;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.DepartmentDto;
import com.nexxserve.nexxclinic.dto.out.DepartmentProfileDto;
import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.dto.out.PaginationDto;
import com.nexxserve.nexxclinic.dto.out.ProductDto;
import com.nexxserve.nexxclinic.mappers.out.DepartmentMapper;
import com.nexxserve.nexxclinic.mappers.out.InsuranceProviderMapper;
import com.nexxserve.nexxclinic.mappers.out.ProductMapper;
import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;
import com.nexxserve.nexxclinic.repository.DepartmentInsurancePolicyRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormDto;
import com.nexxserve.nexxclinic.dto.out.StandaloneFormVersionDto;
import com.nexxserve.nexxclinic.entity.DepartmentStandaloneForm;
import com.nexxserve.nexxclinic.entity.StandaloneForm;
import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import com.nexxserve.nexxclinic.mappers.out.StandaloneFormMapper;
import com.nexxserve.nexxclinic.repository.DepartmentStandaloneFormRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormVersionRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final DepartmentInsurancePolicyRepository departmentInsurancePolicyRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final DepartmentProfileProductRepository departmentProfileProductRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final ProductRepository productRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final DepartmentStandaloneFormRepository departmentStandaloneFormRepository;
    private final StandaloneFormVersionRepository standaloneFormVersionRepository;
    private final DepartmentMapper departmentMapper;
    private final InsuranceProviderMapper insuranceProviderMapper;
    private final ProductMapper productMapper;
    private final StandaloneFormMapper standaloneFormMapper;

    public DepartmentService(
            DepartmentRepository departmentRepository,
            DepartmentInsurancePolicyRepository departmentInsurancePolicyRepository,
            DepartmentProfileRepository departmentProfileRepository,
            DepartmentProfileProductRepository departmentProfileProductRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            ProductRepository productRepository,
            ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
            DepartmentStandaloneFormRepository departmentStandaloneFormRepository,
            StandaloneFormVersionRepository standaloneFormVersionRepository,
            DepartmentMapper departmentMapper,
            InsuranceProviderMapper insuranceProviderMapper,
            ProductMapper productMapper,
            StandaloneFormMapper standaloneFormMapper
    ) {
        this.departmentRepository = departmentRepository;
        this.departmentInsurancePolicyRepository = departmentInsurancePolicyRepository;
        this.departmentProfileRepository = departmentProfileRepository;
        this.departmentProfileProductRepository = departmentProfileProductRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.productRepository = productRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.departmentStandaloneFormRepository = departmentStandaloneFormRepository;
        this.standaloneFormVersionRepository = standaloneFormVersionRepository;
        this.departmentMapper = departmentMapper;
        this.insuranceProviderMapper = insuranceProviderMapper;
        this.productMapper = productMapper;
        this.standaloneFormMapper = standaloneFormMapper;
    }

    @Transactional
    public ApiResponse<DepartmentDto> createDepartment(CreateDepartmentInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        String name = requiredTrim(input.name());
        if (name == null) {
            return ApiResponse.error("name is required.");
        }

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            return ApiResponse.error("Department name already exists.");
        }

        Department department = new Department();
        department.setName(name);
        department.setInsurancePolicyMode(resolveMode(input.insurancePolicyMode(), input.insuranceProviderIds()));
        if (input.nursing() != null) {
            department.setNursing(input.nursing());
        }
        if (input.supportRequests() != null) {
            department.setSupportRequests(input.supportRequests());
        }
        if (input.requestsProducts() != null) {
            department.setRequestsProducts(input.requestsProducts());
        }

        Department savedDepartment = departmentRepository.save(department);

        ApiResponse validationError = syncDepartmentInsurancePolicies(savedDepartment, input.insurancePolicyMode(), input.insuranceProviderIds(), false);
        if (validationError != null) {
            return validationError;
        }

        ApiResponse profilesError = upsertProfiles(savedDepartment, input.profiles());
        if (profilesError != null) {
            return profilesError;
        }

        return ApiResponse.success("Department created.", departmentToDto(savedDepartment));
    }

    @Transactional
    public ApiResponse<DepartmentDto> updateDepartment(UUID departmentId, UpdateDepartmentInput input) {
        if (departmentId == null || input == null) {
            return ApiResponse.error("departmentId and input are required.");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.");
        }

        Department department = departmentOptional.get();

        if (input.name() != null) {
            String name = requiredTrim(input.name());
            if (name == null) {
                return ApiResponse.error("name cannot be blank.");
            }
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, department.getId())) {
                return ApiResponse.error("Department name already exists.");
            }
            department.setName(name);
        }

        boolean updateInsurance = input.insurancePolicyMode() != null || input.insuranceProviderIds() != null;
        if (updateInsurance) {
            DepartmentInsurancePolicyMode mode = resolveMode(input.insurancePolicyMode(), input.insuranceProviderIds());
            ApiResponse validationError = syncDepartmentInsurancePolicies(department, mode, input.insuranceProviderIds(), true);
            if (validationError != null) {
                return validationError;
            }
            department.setInsurancePolicyMode(mode);
        }

        if (input.profiles() != null) {
            // Profiles are extra objects: each submitted profile is created (no id) or
            // updated (id) in place. Profiles NOT in this payload are left untouched —
            // they are never deleted here. Use removeDepartmentProfile to delete one.
            ApiResponse profilesError = upsertProfiles(department, input.profiles());
            if (profilesError != null) {
                return profilesError;
            }
        }

        if (input.nursing() != null) {
            department.setNursing(input.nursing());
        }

        if (input.supportRequests() != null) {
            department.setSupportRequests(input.supportRequests());
        }

        if (input.requestsProducts() != null) {
            department.setRequestsProducts(input.requestsProducts());
        }

        Department saved = departmentRepository.save(department);
        return ApiResponse.success("Department updated.", departmentToDto(saved));
    }

    /**
     * Deletes a department profile (profiles are extra objects, so removal is a
     * standalone operation). Refuses to delete a profile that is still referenced by
     * a visit department — those must be switched to another profile first via
     * changeVisitDepartmentProfile.
     */
    @Transactional
    public ApiResponse<DepartmentDto> removeDepartmentProfile(UUID profileId) {
        if (profileId == null) {
            return ApiResponse.error("profileId is required.");
        }

        Optional<DepartmentProfile> profileOptional = departmentProfileRepository.findById(profileId);
        if (profileOptional.isEmpty()) {
            return ApiResponse.error("Profile not found.");
        }

        DepartmentProfile profile = profileOptional.get();
        if (visitDepartmentRepository.existsByProfileId(profile.getId())) {
            return ApiResponse.error(
                    "Profile '" + profile.getName() + "' is in use by a visit and cannot be removed. " +
                    "Create a new profile instead or update the visit departments first."
            );
        }

        departmentProfileProductRepository.deleteByProfileId(profile.getId());
        departmentProfileRepository.delete(profile);
        return ApiResponse.success("Department profile removed.", departmentToDto(profile.getDepartment()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<DepartmentDto> department(UUID departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("departmentId is required.");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.");
        }

        return ApiResponse.success("Department fetched.", departmentToDto(departmentOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<DepartmentDto>> departments(SearchDepartmentsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Department> spec = (root, queryDef, builder) -> builder.conjunction();

        String name = input == null ? null : blankToNull(input.name());
        if (name != null) {
            String normalized = name.toLowerCase();
            spec = spec.and((root, queryDef, builder) -> builder.like(builder.lower(root.get("name")), "%" + normalized + "%"));
        }

        Boolean supportRequests = input == null ? null : input.supportRequests();
        if (supportRequests != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("supportRequests"), supportRequests));
        }

        Boolean requestsProducts = input == null ? null : input.requestsProducts();
        if (requestsProducts != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("requestsProducts"), requestsProducts));
        }

        Page<Department> departmentPage = departmentRepository.findAll(spec, pageable);
        List<DepartmentDto> departments = departmentPage.getContent().stream().map(this::departmentToDto).toList();

        return ApiResponse.success(
                "Departments fetched.",
                departments,
                new PaginationDto(
                        departmentPage.getTotalElements(),
                        departmentPage.getSize(),
                        departmentPage.getNumber(),
                        departmentPage.getTotalPages()
                )
        );
    }

    private ApiResponse syncDepartmentInsurancePolicies(
            Department department,
            DepartmentInsurancePolicyMode requestedMode,
            List<UUID> insuranceProviderIds,
            boolean replaceExisting
    ) {
        List<UUID> ids = normalizeUuidList(insuranceProviderIds);
        DepartmentInsurancePolicyMode mode = resolveMode(requestedMode, insuranceProviderIds);

        if (mode == DepartmentInsurancePolicyMode.ALL) {
            if (!ids.isEmpty()) {
                return ApiResponse.error("Do not send insuranceProviderIds when mode is ALL.");
            }
            if (replaceExisting) {
                departmentInsurancePolicyRepository.deleteByDepartmentId(department.getId());
                departmentInsurancePolicyRepository.flush();
            }
            department.setInsurancePolicyMode(DepartmentInsurancePolicyMode.ALL);
            return null;
        }

        Set<UUID> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            return ApiResponse.error("Duplicate insuranceProviderIds are not allowed.");
        }

        Map<UUID, InsuranceProvider> providers = new HashMap<>();
        for (UUID providerId : uniqueIds) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(providerId);
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found: " + providerId);
            }
            providers.put(providerId, providerOptional.get());
        }

        if (replaceExisting) {
            departmentInsurancePolicyRepository.deleteByDepartmentId(department.getId());
            departmentInsurancePolicyRepository.flush();
        }

        List<DepartmentInsurancePolicy> policies = new ArrayList<>();
        for (UUID providerId : ids) {
            DepartmentInsurancePolicy policy = new DepartmentInsurancePolicy();
            policy.setDepartment(department);
            policy.setInsuranceProvider(providers.get(providerId));
            policies.add(policy);
        }

        if (!policies.isEmpty()) {
            departmentInsurancePolicyRepository.saveAll(policies);
        }
        department.setInsurancePolicyMode(mode);
        return null;
    }

    /**
     * Upserts the department's profiles (profiles are "extra objects" — they are
     * created or updated in place and are never deleted by this method).
     *
     * <p>Each submitted profile is either created (no {@code id}) or updated
     * ({@code id} of a profile that belongs to this department). Profiles NOT in
     * {@code inputs} are left untouched; use {@link #removeDepartmentProfile(UUID)}
     * to delete one. The profile's product set is fully replaced on every write.
     *
     * <p>Rules enforced:
     * <ul>
     *   <li>profile names are unique per department (case-insensitive);</li>
     *   <li>at most ONE default profile per department;</li>
     *   <li>a profile may contain zero products.</li>
     * </ul>
     */
    private ApiResponse upsertProfiles(Department department, List<DepartmentProfileInput> inputs) {
        if (inputs == null) {
            return null;
        }

        List<DepartmentProfileInput> normalized = inputs.stream()
                .filter(java.util.Objects::nonNull)
                .toList();

        // ---- validate names + single default across the submitted set ----
        Set<String> seenNames = new HashSet<>();
        int defaultCount = 0;
        for (DepartmentProfileInput input : normalized) {
            String name = requiredTrim(input.name());
            if (name == null) {
                return profileError("Profile name is required.");
            }
            if (!seenNames.add(name.toLowerCase())) {
                return profileError("Duplicate profile name: " + name);
            }
            if (Boolean.TRUE.equals(input.isDefault())) {
                defaultCount++;
            }
        }
        if (defaultCount > 1) {
            return profileError("A department cannot have more than one default profile.");
        }

        // ---- upsert each submitted profile ----
        for (DepartmentProfileInput input : normalized) {
            String name = requiredTrim(input.name());
            boolean isDefault = Boolean.TRUE.equals(input.isDefault());

            DepartmentProfile profile;
            if (input.id() != null) {
                Optional<DepartmentProfile> existingOptional = departmentProfileRepository.findById(input.id());
                if (existingOptional.isEmpty()
                        || !department.getId().equals(existingOptional.get().getDepartment().getId())) {
                    return profileError("Profile not found: " + input.id());
                }
                profile = existingOptional.get();
            } else {
                profile = new DepartmentProfile();
                profile.setDepartment(department);
            }

            // Case-insensitive name uniqueness against the department's OTHER profiles
            // (this one is excluded when updating). Existing profiles outside the
            // payload are kept under merge semantics, so the check must cover them.
            for (DepartmentProfile other : departmentProfileRepository.findByDepartmentId(department.getId())) {
                if (other.getId().equals(profile.getId())) {
                    continue;
                }
                if (other.getName() != null && other.getName().equalsIgnoreCase(name)) {
                    return profileError("Profile name already exists in this department: " + name);
                }
            }

            profile.setName(name);
            if (input.encounterType() != null) {
                profile.setEncounterType(input.encounterType());
            } else if (profile.getEncounterType() == null) {
                // New profile must have an encounter type
                return profileError("Encounter type is required for profile '" + name + "'.");
            }

            // Single-default enforcement. Clear any other default FIRST and flush, so
            // the partial unique index uk_department_profile_single_default
            // (WHERE is_default = true) can never be violated by this profile's
            // insert/update regardless of Hibernate's flush ordering.
            if (isDefault) {
                List<DepartmentProfile> sameDepartment = departmentProfileRepository.findByDepartmentId(department.getId());
                boolean clearedOtherDefault = false;
                for (DepartmentProfile other : sameDepartment) {
                    if (!other.getId().equals(profile.getId()) && other.isDefault()) {
                        other.setDefault(false);
                        departmentProfileRepository.save(other);
                        clearedOtherDefault = true;
                    }
                }
                if (clearedOtherDefault) {
                    departmentProfileRepository.flush();
                }
            }
            profile.setDefault(isDefault);
            departmentProfileRepository.save(profile);

            // ---- sync profile products ----
            List<UUID> productIds = normalizeUuidList(input.productIds());
            Set<UUID> uniqueProductIds = new HashSet<>(productIds);
            if (uniqueProductIds.size() != productIds.size()) {
                return profileError("Duplicate productIds are not allowed within a profile.");
            }

            Map<UUID, Product> products = new HashMap<>();
            for (UUID productId : uniqueProductIds) {
                Optional<Product> productOptional = productRepository.findById(productId);
                if (productOptional.isEmpty()) {
                    return profileError("Product not found: " + productId);
                }
                products.put(productId, productOptional.get());
            }

            departmentProfileProductRepository.deleteByProfileId(profile.getId());

            List<DepartmentProfileProduct> links = new ArrayList<>();
            for (UUID productId : productIds) {
                DepartmentProfileProduct link = new DepartmentProfileProduct();
                link.setProfile(profile);
                link.setProduct(products.get(productId));
                links.add(link);
            }

            if (!links.isEmpty()) {
                departmentProfileProductRepository.saveAll(links);
            }
        }

        return null;
    }

    /**
     * Builds a profile validation error and marks the surrounding transaction for
     * rollback, so a rejected profile payload never commits partially-applied
     * department changes (the department row on create, or name/insurance edits
     * already applied on update).
     */
    private ApiResponse profileError(String message) {
        org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
        return ApiResponse.error(message);
    }

    private DepartmentInsurancePolicyMode resolveMode(DepartmentInsurancePolicyMode requestedMode, List<UUID> insuranceProviderIds) {
        if (requestedMode != null) {
            return requestedMode;
        }
        List<UUID> ids = normalizeUuidList(insuranceProviderIds);
        if (ids.isEmpty()) {
            return DepartmentInsurancePolicyMode.ALL;
        }
        return DepartmentInsurancePolicyMode.ONLY;
    }

    private DepartmentDto departmentToDto(Department department) {
        List<InsuranceProviderDto> policies = departmentInsurancePolicyRepository.findByDepartmentId(department.getId())
                .stream()
                .map(policy -> insuranceProviderMapper.toDto(policy.getInsuranceProvider()))
                .toList();

        List<DepartmentProfileDto> profiles = departmentProfileRepository.findByDepartmentId(department.getId())
                .stream()
                .map(profile -> new DepartmentProfileDto(
                        profile.getId(),
                        profile.getName(),
                        profile.getEncounterType(),
                        profile.isDefault(),
                        departmentProfileProductRepository.findByProfileId(profile.getId())
                                .stream()
                                .map(link -> productMapper.toDto(link.getProduct()))
                                .toList(),
                        profile.getCreatedAt(),
                        profile.getUpdatedAt()
                ))
                .toList();

        List<DepartmentStandaloneForm> formLinks = departmentStandaloneFormRepository.findByDepartmentId(department.getId());

        List<StandaloneFormDto> standaloneForms = formLinks.stream()
                .map(link -> {
                    StandaloneForm form = link.getStandaloneForm();
                    StandaloneFormVersion latest = standaloneFormVersionRepository
                            .findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(form.getId())
                            .orElse(null);
                    return mapToStandaloneFormDto(form, latest);
                })
                .toList();

        StandaloneFormDto defaultForm = formLinks.stream()
                .filter(DepartmentStandaloneForm::isDefault)
                .findFirst()
                .map(link -> {
                    StandaloneForm form = link.getStandaloneForm();
                    StandaloneFormVersion latest = standaloneFormVersionRepository
                            .findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(form.getId())
                            .orElse(null);
                    return mapToStandaloneFormDto(form, latest);
                })
                .orElse(null);

        return departmentMapper.toDtoWithDetails(department, policies, profiles, standaloneForms, defaultForm);
    }

    private StandaloneFormDto mapToStandaloneFormDto(StandaloneForm form, StandaloneFormVersion version) {
        StandaloneFormDto baseDto = standaloneFormMapper.toDto(form);
        StandaloneFormVersionDto versionDto = version != null ? standaloneFormMapper.toDto(version) : null;
        return new StandaloneFormDto(
                baseDto.id(),
                baseDto.name(),
                baseDto.description(),
                baseDto.type(),
                baseDto.category(),
                baseDto.isTemplate(),
                baseDto.createdBy(),
                versionDto,
                baseDto.createdAt(),
                baseDto.updatedAt()
        );
    }

    private List<UUID> normalizeUuidList(List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(java.util.Objects::nonNull).toList();
    }

    private String requiredTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String blankToNull(String value) {
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
}
