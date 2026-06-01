package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentDefaultProduct;
import com.nexxserve.nexxclinic.entity.DepartmentInsurancePolicy;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.graphql.input.CreateDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.SearchDepartmentsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateDepartmentInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;
import com.nexxserve.nexxclinic.repository.DepartmentDefaultProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentInsurancePolicyRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
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
    private final DepartmentDefaultProductRepository departmentDefaultProductRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final ProductRepository productRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DepartmentService(
            DepartmentRepository departmentRepository,
            DepartmentInsurancePolicyRepository departmentInsurancePolicyRepository,
            DepartmentDefaultProductRepository departmentDefaultProductRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            ProductRepository productRepository,
            ProductInsuranceCoverageRepository productInsuranceCoverageRepository
    ) {
        this.departmentRepository = departmentRepository;
        this.departmentInsurancePolicyRepository = departmentInsurancePolicyRepository;
        this.departmentDefaultProductRepository = departmentDefaultProductRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.productRepository = productRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
    }

    @Transactional
    public ApiResponse createDepartment(CreateDepartmentInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        String name = requiredTrim(input.name());
        if (name == null) {
            return ApiResponse.error("name is required.", "VALIDATION_ERROR");
        }

        if (departmentRepository.existsByNameIgnoreCase(name)) {
            return ApiResponse.error("Department name already exists.", "DUPLICATE_DEPARTMENT_NAME");
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

        ApiResponse defaultProductsError = syncDefaultProducts(savedDepartment, input.defaultProductIds(), false);
        if (defaultProductsError != null) {
            return defaultProductsError;
        }

        return ApiResponse.success("Department created.", departmentToMap(savedDepartment));
    }

    @Transactional
    public ApiResponse updateDepartment(UUID departmentId, UpdateDepartmentInput input) {
        if (departmentId == null || input == null) {
            return ApiResponse.error("departmentId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        Department department = departmentOptional.get();

        if (input.name() != null) {
            String name = requiredTrim(input.name());
            if (name == null) {
                return ApiResponse.error("name cannot be blank.", "VALIDATION_ERROR");
            }
            if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, department.getId())) {
                return ApiResponse.error("Department name already exists.", "DUPLICATE_DEPARTMENT_NAME");
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

        if (input.defaultProductIds() != null) {
            ApiResponse defaultProductsError = syncDefaultProducts(department, input.defaultProductIds(), true);
            if (defaultProductsError != null) {
                return defaultProductsError;
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
        return ApiResponse.success("Department updated.", departmentToMap(saved));
    }

    @Transactional(readOnly = true)
    public ApiResponse department(UUID departmentId) {
        if (departmentId == null) {
            return ApiResponse.error("departmentId is required.", "VALIDATION_ERROR");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Department fetched.", departmentToMap(departmentOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse departments(SearchDepartmentsInput input) {
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
        List<Map<String, Object>> departments = departmentPage.getContent().stream().map(this::departmentToMap).toList();

        return ApiResponse.success(
                "Departments fetched.",
            departments,
            paginationToMap(departmentPage)
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
                return ApiResponse.error("Do not send insuranceProviderIds when mode is ALL.", "VALIDATION_ERROR");
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
            return ApiResponse.error("Duplicate insuranceProviderIds are not allowed.", "VALIDATION_ERROR");
        }

        Map<UUID, InsuranceProvider> providers = new HashMap<>();
        for (UUID providerId : uniqueIds) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(providerId);
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found: " + providerId, "NOT_FOUND");
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

    private ApiResponse syncDefaultProducts(Department department, List<UUID> productIds, boolean replaceExisting) {
        if (productIds == null) {
            return null;
        }

        List<UUID> ids = normalizeUuidList(productIds);
        Set<UUID> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            return ApiResponse.error("Duplicate defaultProductIds are not allowed.", "VALIDATION_ERROR");
        }

        Map<UUID, Product> products = new HashMap<>();
        for (UUID productId : uniqueIds) {
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found: " + productId, "NOT_FOUND");
            }
            products.put(productId, productOptional.get());
        }

        if (replaceExisting) {
            departmentDefaultProductRepository.deleteByDepartmentId(department.getId());
        }

        List<DepartmentDefaultProduct> defaults = new ArrayList<>();
        for (UUID productId : ids) {
            DepartmentDefaultProduct link = new DepartmentDefaultProduct();
            link.setDepartment(department);
            link.setProduct(products.get(productId));
            defaults.add(link);
        }

        if (!defaults.isEmpty()) {
            departmentDefaultProductRepository.saveAll(defaults);
        }

        return null;
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

    private Map<String, Object> departmentToMap(Department department) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", department.getId());
        data.put("name", department.getName());
        data.put("insurancePolicyMode", department.getInsurancePolicyMode());
        data.put(
                "insurancePolicies",
                departmentInsurancePolicyRepository.findByDepartmentId(department.getId())
                        .stream()
                        .map(policy -> insuranceProviderToMap(policy.getInsuranceProvider()))
                        .toList()
        );
        data.put("defaultProducts", departmentDefaultProductRepository.findByDepartmentId(department.getId()).stream().map(link -> productToMap(link.getProduct())).toList());
        data.put("createdAt", department.getCreatedAt());
        data.put("updatedAt", department.getUpdatedAt());
        data.put("nursing", department.isNursing());
        data.put("supportRequests", department.isSupportRequests());
        data.put("requestsProducts", department.isRequestsProducts());
        return data;
    }

    private Map<String, Object> insuranceProviderToMap(InsuranceProvider insuranceProvider) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", insuranceProvider.getId());
        data.put("insuranceName", insuranceProvider.getInsuranceName());
        data.put("acronym", insuranceProvider.getAcronym());
        data.put("defaultCoveragePercentage", insuranceProvider.getDefaultCoveragePercentage());
        data.put("supportedByClinic", insuranceProvider.isSupportedByClinic());
        data.put("iconUrl", insuranceProvider.getIconUrl());
        data.put("createdAt", insuranceProvider.getCreatedAt());
        data.put("updatedAt", insuranceProvider.getUpdatedAt());
        return data;
    }

    private Map<String, Object> productToMap(Product product) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("genericName", product.getGenericName());
        data.put("code", product.getCode());
        data.put("description", product.getDescription());
        data.put("type", product.getType());
        data.put("unit", product.getUnit());
        data.put("metadata", parseMetadata(product.getMetadata()));
        data.put("privateRhicPrice", product.getPrivateRhicPrice());
        data.put("clinicPrice", product.getClinicPrice());
        data.put("insuranceCoverages", productInsuranceCoverageRepository.findByProductId(product.getId()).stream().map(this::productCoverageToMap).toList());
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
        return data;
    }

    private Map<String, Object> productCoverageToMap(ProductInsuranceCoverage coverage) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", coverage.getId());
        data.put("insuranceProvider", insuranceProviderToMap(coverage.getInsuranceProvider()));
        data.put("cost", coverage.getCost());
        data.put("covered", coverage.isCovered());
        data.put("requireMedicalAdvisor", coverage.isRequireMedicalAdvisor());
        data.put("mustPrescribedBy", coverage.getMustPrescribedBy());
        data.put("drugAdministrationFrequency", coverage.getDrugAdministrationFrequency());
        data.put("authorizationRequestReasons", new ArrayList<>(coverage.getAuthorizationRequestReasons()));
        data.put("createdAt", coverage.getCreatedAt());
        data.put("updatedAt", coverage.getUpdatedAt());
        return data;
    }

    private Object parseMetadata(String metadataJson) {
        String raw = (metadataJson == null || metadataJson.isBlank()) ? "{}" : metadataJson;
        try {
            return objectMapper.readValue(raw, Object.class);
        } catch (JsonProcessingException ex) {
            return Map.of();
        }
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

    private Map<String, Object> paginationToMap(Page<?> page) {
        Map<String, Object> pagination = new HashMap<>();
        pagination.put("total", page.getTotalElements());
        pagination.put("perPage", page.getSize());
        pagination.put("currentPage", page.getNumber());
        pagination.put("totalPages", page.getTotalPages());
        return pagination;
    }
}
