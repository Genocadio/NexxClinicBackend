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
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.DepartmentDto;
import com.nexxserve.nexxclinic.dto.out.InsuranceProviderDto;
import com.nexxserve.nexxclinic.dto.out.PaginationDto;
import com.nexxserve.nexxclinic.dto.out.ProductDto;
import com.nexxserve.nexxclinic.mappers.out.DepartmentMapper;
import com.nexxserve.nexxclinic.mappers.out.InsuranceProviderMapper;
import com.nexxserve.nexxclinic.mappers.out.ProductMapper;
import com.nexxserve.nexxclinic.model.DepartmentInsurancePolicyMode;
import com.nexxserve.nexxclinic.repository.DepartmentDefaultProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentInsurancePolicyRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
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
    private final DepartmentDefaultProductRepository departmentDefaultProductRepository;
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
            DepartmentDefaultProductRepository departmentDefaultProductRepository,
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
        this.departmentDefaultProductRepository = departmentDefaultProductRepository;
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

        ApiResponse defaultProductsError = syncDefaultProducts(savedDepartment, input.defaultProductIds(), false);
        if (defaultProductsError != null) {
            return defaultProductsError;
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
        return ApiResponse.success("Department updated.", departmentToDto(saved));
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

    private ApiResponse syncDefaultProducts(Department department, List<UUID> productIds, boolean replaceExisting) {
        if (productIds == null) {
            return null;
        }

        List<UUID> ids = normalizeUuidList(productIds);
        Set<UUID> uniqueIds = new HashSet<>(ids);
        if (uniqueIds.size() != ids.size()) {
            return ApiResponse.error("Duplicate defaultProductIds are not allowed.");
        }

        Map<UUID, Product> products = new HashMap<>();
        for (UUID productId : uniqueIds) {
            Optional<Product> productOptional = productRepository.findById(productId);
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found: " + productId);
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

    private DepartmentDto departmentToDto(Department department) {
        List<InsuranceProviderDto> policies = departmentInsurancePolicyRepository.findByDepartmentId(department.getId())
                .stream()
                .map(policy -> insuranceProviderMapper.toDto(policy.getInsuranceProvider()))
                .toList();

        List<ProductDto> products = departmentDefaultProductRepository.findByDepartmentId(department.getId())
                .stream()
                .map(link -> productMapper.toDto(link.getProduct()))
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

        return departmentMapper.toDtoWithDetails(department, policies, products, standaloneForms, defaultForm);
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
