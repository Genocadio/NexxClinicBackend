package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInput;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.graphql.input.SearchProductsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.DrugAdministrationFrequency;
import com.nexxserve.nexxclinic.model.MustPrescribedBy;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import java.math.BigDecimal;
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
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductInsuranceCoverageRepository coverageRepository;
    private final InsuranceProviderRepository insuranceProviderRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductService(
            ProductRepository productRepository,
            ProductInsuranceCoverageRepository coverageRepository,
            InsuranceProviderRepository insuranceProviderRepository
    ) {
        this.productRepository = productRepository;
        this.coverageRepository = coverageRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
    }

    @Transactional
    public ApiResponse createProduct(CreateProductInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        String name = requiredTrim(input.name());
        String code = requiredTrim(input.code());
        String description = requiredTrim(input.description());

        if (name == null || code == null || description == null || input.type() == null || input.unit() == null) {
            return ApiResponse.error("name, code, description, type and unit are required.", "VALIDATION_ERROR");
        }

        if (productRepository.existsByCodeIgnoreCase(code)) {
            return ApiResponse.error("Product code already exists.", "DUPLICATE_PRODUCT_CODE");
        }

        if (input.id() != null && productRepository.existsById(input.id())) {
            return ApiResponse.error("Product id already exists.", "DUPLICATE_PRODUCT_ID");
        }

        Product product = new Product();
        if (input.id() != null) {
            product.setId(input.id());
        }
        product.setName(name);
        product.setGenericName(blankToNull(input.genericName()));
        product.setCode(code);
        product.setDescription(description);
        product.setType(input.type());
        product.setUnit(input.unit());

        ApiResponse metadataValidation = applyMetadata(product, input.metadata());
        if (metadataValidation != null) {
            return metadataValidation;
        }

        if (input.privateRhicPrice() != null && input.privateRhicPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("privateRhicPrice must be greater than or equal to 0.", "VALIDATION_ERROR");
        }
        if (input.clinicPrice() != null && input.clinicPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("clinicPrice must be greater than or equal to 0.", "VALIDATION_ERROR");
        }
        product.setPrivateRhicPrice(input.privateRhicPrice());
        product.setClinicPrice(input.clinicPrice());

        Product saved = productRepository.save(product);

        if (input.insuranceCoverages() != null) {
            ApiResponse coverageError = replaceCoverages(saved, input.insuranceCoverages(), null);
            if (coverageError != null) {
                return coverageError;
            }
        }

        Product latest = productRepository.findById(saved.getId()).orElse(saved);
        return ApiResponse.success("Product created.", productToMap(latest));
    }

    @Transactional
    public ApiResponse updateProduct(UUID productId, UpdateProductInput input) {
        if (productId == null || input == null) {
            return ApiResponse.error("productId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.", "NOT_FOUND");
        }

        Product product = productOptional.get();

        if (input.name() != null) {
            String name = requiredTrim(input.name());
            if (name == null) {
                return ApiResponse.error("name cannot be blank.", "VALIDATION_ERROR");
            }
            product.setName(name);
        }

        if (input.genericName() != null) {
            product.setGenericName(blankToNull(input.genericName()));
        }

        if (input.code() != null) {
            String code = requiredTrim(input.code());
            if (code == null) {
                return ApiResponse.error("code cannot be blank.", "VALIDATION_ERROR");
            }
            if (productRepository.existsByCodeIgnoreCaseAndIdNot(code, product.getId())) {
                return ApiResponse.error("Product code already exists.", "DUPLICATE_PRODUCT_CODE");
            }
            product.setCode(code);
        }

        if (input.description() != null) {
            String description = requiredTrim(input.description());
            if (description == null) {
                return ApiResponse.error("description cannot be blank.", "VALIDATION_ERROR");
            }
            product.setDescription(description);
        }

        if (input.type() != null) {
            product.setType(input.type());
        }

        if (input.unit() != null) {
            product.setUnit(input.unit());
        }

        if (input.metadata() != null) {
            ApiResponse metadataValidation = applyMetadata(product, input.metadata());
            if (metadataValidation != null) {
                return metadataValidation;
            }
        }

        if (input.privateRhicPrice() != null) {
            if (input.privateRhicPrice().compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponse.error("privateRhicPrice must be greater than or equal to 0.", "VALIDATION_ERROR");
            }
            product.setPrivateRhicPrice(input.privateRhicPrice());
        }

        if (input.clinicPrice() != null) {
            if (input.clinicPrice().compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponse.error("clinicPrice must be greater than or equal to 0.", "VALIDATION_ERROR");
            }
            product.setClinicPrice(input.clinicPrice());
        }

        Product saved = productRepository.save(product);

        if (input.insuranceCoverages() != null) {
            ApiResponse coverageError = replaceCoverages(saved, null, input.insuranceCoverages());
            if (coverageError != null) {
                return coverageError;
            }
        }

        Product latest = productRepository.findById(saved.getId()).orElse(saved);
        return ApiResponse.success("Product updated.", productToMap(latest));
    }

    @Transactional(readOnly = true)
    public ApiResponse product(UUID productId) {
        if (productId == null) {
            return ApiResponse.error("productId is required.", "VALIDATION_ERROR");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Product fetched.", productToMap(productOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse products(SearchProductsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Specification<Product> spec = (root, queryDef, builder) -> builder.conjunction();

        String name = input == null ? null : blankToNull(input.name());
        if (name != null) {
            String normalized = name.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.or(
                            builder.like(builder.lower(root.get("name")), "%" + normalized + "%"),
                            builder.like(builder.lower(root.get("genericName")), "%" + normalized + "%"),
                            builder.like(builder.lower(root.get("description")), "%" + normalized + "%")
                    )
            );
        }

        if (input != null && input.type() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("type"), input.type()));
        }

        Page<Product> productPage = productRepository.findAll(spec, pageable);
        List<Map<String, Object>> products = productPage.getContent().stream().map(this::productToMap).toList();

        return ApiResponse.success(
                "Products fetched.",
            products,
            paginationToMap(productPage)
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse productInsuranceCoverage(UUID productInsuranceCoverageId) {
        if (productInsuranceCoverageId == null) {
            return ApiResponse.error("productInsuranceCoverageId is required.", "VALIDATION_ERROR");
        }

        Optional<ProductInsuranceCoverage> coverageOptional = coverageRepository.findById(productInsuranceCoverageId);
        if (coverageOptional.isEmpty()) {
            return ApiResponse.error("Product insurance coverage not found.", "NOT_FOUND");
        }

        return ApiResponse.success("Product insurance coverage fetched.", coverageToMap(coverageOptional.get()));
    }

    @Transactional
    public ApiResponse createProductInsuranceCoverage(UUID productId, CreateProductInsuranceCoverageInput input) {
        if (productId == null || input == null) {
            return ApiResponse.error("productId and input are required.", "VALIDATION_ERROR");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.", "NOT_FOUND");
        }

        if (input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required.", "VALIDATION_ERROR");
        }

        if (coverageRepository.existsByProductIdAndInsuranceProviderId(productId, input.insuranceProviderId())) {
            return ApiResponse.error("Coverage for this insurance provider already exists on product.", "DUPLICATE_COVERAGE");
        }

        Optional<InsuranceProvider> insuranceProviderOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (insuranceProviderOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        ApiResponse costValidation = validateCost(input.cost());
        if (costValidation != null) {
            return costValidation;
        }

        ProductInsuranceCoverage coverage = buildCoverageEntity(productOptional.get(), insuranceProviderOptional.get(), input);
        ProductInsuranceCoverage saved = coverageRepository.save(coverage);
        return ApiResponse.success("Product insurance coverage created.", coverageToMap(saved));
    }

    @Transactional
    public ApiResponse updateProductInsuranceCoverage(UUID productInsuranceCoverageId, UpdateProductInsuranceCoverageInput input) {
        if (productInsuranceCoverageId == null || input == null) {
            return ApiResponse.error("productInsuranceCoverageId and input are required.", "VALIDATION_ERROR");
        }

        Optional<ProductInsuranceCoverage> coverageOptional = coverageRepository.findById(productInsuranceCoverageId);
        if (coverageOptional.isEmpty()) {
            return ApiResponse.error("Product insurance coverage not found.", "NOT_FOUND");
        }

        ProductInsuranceCoverage coverage = coverageOptional.get();

        if (input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required.", "VALIDATION_ERROR");
        }

        Optional<InsuranceProvider> insuranceProviderOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (insuranceProviderOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.", "NOT_FOUND");
        }

        if (coverageRepository.existsByProductIdAndInsuranceProviderIdAndIdNot(
                coverage.getProduct().getId(),
                input.insuranceProviderId(),
                coverage.getId()
        )) {
            return ApiResponse.error("Coverage for this insurance provider already exists on product.", "DUPLICATE_COVERAGE");
        }

        ApiResponse costValidation = validateCost(input.cost());
        if (costValidation != null) {
            return costValidation;
        }

        coverage.setInsuranceProvider(insuranceProviderOptional.get());

        BigDecimal cost = input.cost() == null ? BigDecimal.ZERO : input.cost();
        coverage.setCost(cost);
        coverage.setCovered(cost.compareTo(BigDecimal.ZERO) > 0);
        coverage.setRequireMedicalAdvisor(input.requireMedicalAdvisor() != null && input.requireMedicalAdvisor());
        coverage.setMustPrescribedBy(input.mustPrescribedBy() == null ? MustPrescribedBy.ALL : input.mustPrescribedBy());
        coverage.setDrugAdministrationFrequency(
                input.drugAdministrationFrequency() == null
                        ? DrugAdministrationFrequency.CUSTOM_HOURS
                        : input.drugAdministrationFrequency()
        );
        coverage.setAuthorizationRequestReasons(
                input.authorizationRequestReasons() == null ? new ArrayList<>() : new ArrayList<>(input.authorizationRequestReasons())
        );

        ProductInsuranceCoverage saved = coverageRepository.save(coverage);
        return ApiResponse.success("Product insurance coverage updated.", coverageToMap(saved));
    }

    @Transactional
    public ApiResponse deleteProductInsuranceCoverage(UUID productInsuranceCoverageId) {
        if (productInsuranceCoverageId == null) {
            return ApiResponse.error("productInsuranceCoverageId is required.", "VALIDATION_ERROR");
        }

        if (!coverageRepository.existsById(productInsuranceCoverageId)) {
            return ApiResponse.error("Product insurance coverage not found.", "NOT_FOUND");
        }

        coverageRepository.deleteById(productInsuranceCoverageId);
        return ApiResponse.success("Product insurance coverage deleted.", true);
    }

    private ApiResponse applyMetadata(Product product, Object metadata) {
        try {
            if (metadata == null) {
                product.setMetadata("{}");
            } else {
                product.setMetadata(objectMapper.writeValueAsString(metadata));
            }
            return null;
        } catch (JsonProcessingException ex) {
            return ApiResponse.error("metadata must be valid JSON.", "VALIDATION_ERROR");
        }
    }

    private ApiResponse validateCost(BigDecimal cost) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("cost must be greater than or equal to 0.", "VALIDATION_ERROR");
        }
        return null;
    }

    private ApiResponse replaceCoverages(
            Product product,
            List<CreateProductInsuranceCoverageInput> createCoverages,
            List<UpdateProductInsuranceCoverageInput> updateCoverages
    ) {
        List<UUID> providerIds = new ArrayList<>();
        if (createCoverages != null) {
            for (CreateProductInsuranceCoverageInput coverage : createCoverages) {
                if (coverage == null || coverage.insuranceProviderId() == null) {
                    return ApiResponse.error("insuranceProviderId is required for each coverage.", "VALIDATION_ERROR");
                }
                providerIds.add(coverage.insuranceProviderId());
            }
        }

        if (updateCoverages != null) {
            for (UpdateProductInsuranceCoverageInput coverage : updateCoverages) {
                if (coverage == null || coverage.insuranceProviderId() == null) {
                    return ApiResponse.error("insuranceProviderId is required for each coverage.", "VALIDATION_ERROR");
                }
                providerIds.add(coverage.insuranceProviderId());
            }
        }

        Set<UUID> uniqueProviderIds = new HashSet<>(providerIds);
        if (uniqueProviderIds.size() != providerIds.size()) {
            return ApiResponse.error("Duplicate insuranceProviderId in insuranceCoverages is not allowed.", "VALIDATION_ERROR");
        }

        Map<UUID, InsuranceProvider> providers = new HashMap<>();
        for (UUID providerId : uniqueProviderIds) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(providerId);
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found: " + providerId, "NOT_FOUND");
            }
            providers.put(providerId, providerOptional.get());
        }

        coverageRepository.deleteByProductId(product.getId());

        List<ProductInsuranceCoverage> coveragesToSave = new ArrayList<>();
        if (createCoverages != null) {
            for (CreateProductInsuranceCoverageInput item : createCoverages) {
                coveragesToSave.add(buildCoverageEntity(product, providers.get(item.insuranceProviderId()), item));
            }
        }
        if (updateCoverages != null) {
            for (UpdateProductInsuranceCoverageInput item : updateCoverages) {
                coveragesToSave.add(buildCoverageEntity(product, providers.get(item.insuranceProviderId()), item));
            }
        }

        if (!coveragesToSave.isEmpty()) {
            coverageRepository.saveAll(coveragesToSave);
        }

        return null;
    }

    private ProductInsuranceCoverage buildCoverageEntity(
            Product product,
            InsuranceProvider insuranceProvider,
            CreateProductInsuranceCoverageInput input
    ) {
        ProductInsuranceCoverage coverage = new ProductInsuranceCoverage();
        coverage.setProduct(product);
        coverage.setInsuranceProvider(insuranceProvider);

        BigDecimal cost = input.cost() == null ? BigDecimal.ZERO : input.cost();
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost must be greater than or equal to 0");
        }

        coverage.setCost(cost);
        coverage.setCovered(cost.compareTo(BigDecimal.ZERO) > 0);
        coverage.setRequireMedicalAdvisor(input.requireMedicalAdvisor() != null && input.requireMedicalAdvisor());
        coverage.setMustPrescribedBy(input.mustPrescribedBy() == null ? MustPrescribedBy.ALL : input.mustPrescribedBy());
        coverage.setDrugAdministrationFrequency(
                input.drugAdministrationFrequency() == null
                        ? DrugAdministrationFrequency.CUSTOM_HOURS
                        : input.drugAdministrationFrequency()
        );
        coverage.setAuthorizationRequestReasons(
                input.authorizationRequestReasons() == null ? new ArrayList<>() : new ArrayList<>(input.authorizationRequestReasons())
        );
        return coverage;
    }

    private ProductInsuranceCoverage buildCoverageEntity(
            Product product,
            InsuranceProvider insuranceProvider,
            UpdateProductInsuranceCoverageInput input
    ) {
        ProductInsuranceCoverage coverage = new ProductInsuranceCoverage();
        coverage.setProduct(product);
        coverage.setInsuranceProvider(insuranceProvider);

        BigDecimal cost = input.cost() == null ? BigDecimal.ZERO : input.cost();
        if (cost.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("cost must be greater than or equal to 0");
        }

        coverage.setCost(cost);
        coverage.setCovered(cost.compareTo(BigDecimal.ZERO) > 0);
        coverage.setRequireMedicalAdvisor(input.requireMedicalAdvisor() != null && input.requireMedicalAdvisor());
        coverage.setMustPrescribedBy(input.mustPrescribedBy() == null ? MustPrescribedBy.ALL : input.mustPrescribedBy());
        coverage.setDrugAdministrationFrequency(
                input.drugAdministrationFrequency() == null
                        ? DrugAdministrationFrequency.CUSTOM_HOURS
                        : input.drugAdministrationFrequency()
        );
        coverage.setAuthorizationRequestReasons(
                input.authorizationRequestReasons() == null ? new ArrayList<>() : new ArrayList<>(input.authorizationRequestReasons())
        );
        return coverage;
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
        data.put("insuranceCoverages", coverageRepository.findByProductId(product.getId()).stream().map(this::coverageToMap).toList());
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
        return data;
    }

    private Map<String, Object> coverageToMap(ProductInsuranceCoverage coverage) {
        Map<String, Object> insuranceProvider = new HashMap<>();
        insuranceProvider.put("id", coverage.getInsuranceProvider().getId());
        insuranceProvider.put("insuranceName", coverage.getInsuranceProvider().getInsuranceName());
        insuranceProvider.put("acronym", coverage.getInsuranceProvider().getAcronym());
        insuranceProvider.put("defaultCoveragePercentage", coverage.getInsuranceProvider().getDefaultCoveragePercentage());
        insuranceProvider.put("supportedByClinic", coverage.getInsuranceProvider().isSupportedByClinic());
        insuranceProvider.put("iconUrl", coverage.getInsuranceProvider().getIconUrl());

        Map<String, Object> data = new HashMap<>();
        data.put("id", coverage.getId());
        data.put("insuranceProvider", insuranceProvider);
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
