package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.ProductDto;
import com.nexxserve.nexxclinic.dto.out.PaginationDto;
import com.nexxserve.nexxclinic.dto.out.ProductInsuranceCoverageDto;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInput;
import com.nexxserve.nexxclinic.graphql.input.CreateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.graphql.input.SearchProductsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateProductInsuranceCoverageInput;
import com.nexxserve.nexxclinic.mappers.out.ProductMapper;
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
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ProductService(
            ProductRepository productRepository,
            ProductInsuranceCoverageRepository coverageRepository,
            InsuranceProviderRepository insuranceProviderRepository,
            ProductMapper productMapper
    ) {
        this.productRepository = productRepository;
        this.coverageRepository = coverageRepository;
        this.insuranceProviderRepository = insuranceProviderRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ApiResponse<ProductDto> createProduct(CreateProductInput input) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        String name = requiredTrim(input.name());
        String code = requiredTrim(input.code());
        String description = requiredTrim(input.description());

        if (name == null || code == null || description == null || input.type() == null || input.unit() == null) {
            return ApiResponse.error("name, code, description, type and unit are required.");
        }

        if (productRepository.existsByCodeIgnoreCase(code)) {
            return ApiResponse.error("Product code already exists.");
        }

        if (input.id() != null && productRepository.existsById(input.id())) {
            return ApiResponse.error("Product id already exists.");
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

        ApiResponse<ProductDto> metadataValidation = applyMetadata(product, input.metadata());
        if (metadataValidation != null) {
            return metadataValidation;
        }

        if (input.privateRhicPrice() != null && input.privateRhicPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("privateRhicPrice must be greater than or equal to 0.");
        }
        if (input.clinicPrice() != null && input.clinicPrice().compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("clinicPrice must be greater than or equal to 0.");
        }
        product.setPrivateRhicPrice(input.privateRhicPrice());
        product.setClinicPrice(input.clinicPrice());

        Product saved = productRepository.save(product);

        if (input.insuranceCoverages() != null) {
            ApiResponse<ProductDto> coverageError = replaceCoverages(saved, input.insuranceCoverages(), null);
            if (coverageError != null) {
                return coverageError;
            }
        }

        Product latest = productRepository.findById(saved.getId()).orElse(saved);
        return ApiResponse.success("Product created.", productMapper.toDto(latest));
    }

    @Transactional
    public ApiResponse<ProductDto> updateProduct(UUID productId, UpdateProductInput input) {
        if (productId == null || input == null) {
            return ApiResponse.error("productId and input are required.");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.");
        }

        Product product = productOptional.get();

        if (input.name() != null) {
            String name = requiredTrim(input.name());
            if (name == null) {
                return ApiResponse.error("name cannot be blank.");
            }
            product.setName(name);
        }

        if (input.genericName() != null) {
            product.setGenericName(blankToNull(input.genericName()));
        }

        if (input.code() != null) {
            String code = requiredTrim(input.code());
            if (code == null) {
                return ApiResponse.error("code cannot be blank.");
            }
            if (productRepository.existsByCodeIgnoreCaseAndIdNot(code, product.getId())) {
                return ApiResponse.error("Product code already exists.");
            }
            product.setCode(code);
        }

        if (input.description() != null) {
            String description = requiredTrim(input.description());
            if (description == null) {
                return ApiResponse.error("description cannot be blank.");
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
            ApiResponse<ProductDto> metadataValidation = applyMetadata(product, input.metadata());
            if (metadataValidation != null) {
                return metadataValidation;
            }
        }

        if (input.privateRhicPrice() != null) {
            if (input.privateRhicPrice().compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponse.error("privateRhicPrice must be greater than or equal to 0.");
            }
            product.setPrivateRhicPrice(input.privateRhicPrice());
        }

        if (input.clinicPrice() != null) {
            if (input.clinicPrice().compareTo(BigDecimal.ZERO) < 0) {
                return ApiResponse.error("clinicPrice must be greater than or equal to 0.");
            }
            product.setClinicPrice(input.clinicPrice());
        }

        Product saved = productRepository.save(product);

        if (input.insuranceCoverages() != null) {
            ApiResponse<ProductDto> coverageError = replaceCoverages(saved, null, input.insuranceCoverages());
            if (coverageError != null) {
                return coverageError;
            }
        }

        Product latest = productRepository.findById(saved.getId()).orElse(saved);
        return ApiResponse.success("Product updated.", productMapper.toDto(latest));
    }

    @Transactional(readOnly = true)
    public ApiResponse<ProductDto> product(UUID productId) {
        if (productId == null) {
            return ApiResponse.error("productId is required.");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.");
        }

        return ApiResponse.success("Product fetched.", productMapper.toDto(productOptional.get()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<ProductDto>> products(SearchProductsInput input) {
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
        List<ProductDto> products = productMapper.toDtoList(productPage.getContent());

        return ApiResponse.success(
                "Products fetched.",
                products,
                new PaginationDto(
                        productPage.getTotalElements(),
                        productPage.getSize(),
                        productPage.getNumber(),
                        productPage.getTotalPages()
                )
        );
    }

    @Transactional(readOnly = true)
    public ApiResponse<ProductInsuranceCoverageDto> productInsuranceCoverage(UUID productInsuranceCoverageId) {
        if (productInsuranceCoverageId == null) {
            return ApiResponse.error("productInsuranceCoverageId is required.");
        }

        Optional<ProductInsuranceCoverage> coverageOptional = coverageRepository.findById(productInsuranceCoverageId);
        if (coverageOptional.isEmpty()) {
            return ApiResponse.error("Product insurance coverage not found.");
        }

        return ApiResponse.success("Product insurance coverage fetched.", productMapper.toDto(coverageOptional.get()));
    }

    @Transactional
    public ApiResponse<ProductInsuranceCoverageDto> createProductInsuranceCoverage(UUID productId, CreateProductInsuranceCoverageInput input) {
        if (productId == null || input == null) {
            return ApiResponse.error("productId and input are required.");
        }

        Optional<Product> productOptional = productRepository.findById(productId);
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.");
        }

        if (input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required.");
        }

        if (coverageRepository.existsByProductIdAndInsuranceProviderId(productId, input.insuranceProviderId())) {
            return ApiResponse.error("Coverage for this insurance provider already exists on product.");
        }

        Optional<InsuranceProvider> insuranceProviderOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (insuranceProviderOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.");
        }

        ApiResponse<ProductInsuranceCoverageDto> costValidation = validateCost(input.cost());
        if (costValidation != null) {
            return costValidation;
        }

        ProductInsuranceCoverage coverage = buildCoverageEntity(productOptional.get(), insuranceProviderOptional.get(), input);
        ProductInsuranceCoverage saved = coverageRepository.save(coverage);
        return ApiResponse.success("Product insurance coverage created.", productMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<ProductInsuranceCoverageDto> updateProductInsuranceCoverage(UUID productInsuranceCoverageId, UpdateProductInsuranceCoverageInput input) {
        if (productInsuranceCoverageId == null || input == null) {
            return ApiResponse.error("productInsuranceCoverageId and input are required.");
        }

        Optional<ProductInsuranceCoverage> coverageOptional = coverageRepository.findById(productInsuranceCoverageId);
        if (coverageOptional.isEmpty()) {
            return ApiResponse.error("Product insurance coverage not found.");
        }

        ProductInsuranceCoverage coverage = coverageOptional.get();

        if (input.insuranceProviderId() == null) {
            return ApiResponse.error("insuranceProviderId is required.");
        }

        Optional<InsuranceProvider> insuranceProviderOptional = insuranceProviderRepository.findById(input.insuranceProviderId());
        if (insuranceProviderOptional.isEmpty()) {
            return ApiResponse.error("Insurance provider not found.");
        }

        if (coverageRepository.existsByProductIdAndInsuranceProviderIdAndIdNot(
                coverage.getProduct().getId(),
                input.insuranceProviderId(),
                coverage.getId()
        )) {
            return ApiResponse.error("Coverage for this insurance provider already exists on product.");
        }

        ApiResponse<ProductInsuranceCoverageDto> costValidation = validateCost(input.cost());
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
        return ApiResponse.success("Product insurance coverage updated.", productMapper.toDto(saved));
    }

    @Transactional
    public ApiResponse<Boolean> deleteProductInsuranceCoverage(UUID productInsuranceCoverageId) {
        if (productInsuranceCoverageId == null) {
            return ApiResponse.error("productInsuranceCoverageId is required.");
        }

        if (!coverageRepository.existsById(productInsuranceCoverageId)) {
            return ApiResponse.error("Product insurance coverage not found.");
        }

        coverageRepository.deleteById(productInsuranceCoverageId);
        return ApiResponse.success("Product insurance coverage deleted.", true);
    }

    private <Y> ApiResponse<Y> applyMetadata(Product product, Object metadata) {
        try {
            if (metadata == null) {
                product.setMetadata("{}");
            } else {
                product.setMetadata(objectMapper.writeValueAsString(metadata));
            }
            return null;
        } catch (JsonProcessingException ex) {
            return ApiResponse.error("metadata must be valid JSON.");
        }
    }

    private <Y> ApiResponse<Y> validateCost(BigDecimal cost) {
        if (cost != null && cost.compareTo(BigDecimal.ZERO) < 0) {
            return ApiResponse.error("cost must be greater than or equal to 0.");
        }
        return null;
    }

    private <Y> ApiResponse<Y> replaceCoverages(
            Product product,
            List<CreateProductInsuranceCoverageInput> createCoverages,
            List<UpdateProductInsuranceCoverageInput> updateCoverages
    ) {
        List<UUID> providerIds = new ArrayList<>();
        if (createCoverages != null) {
            for (CreateProductInsuranceCoverageInput coverage : createCoverages) {
                if (coverage == null || coverage.insuranceProviderId() == null) {
                    return ApiResponse.error("insuranceProviderId is required for each coverage.");
                }
                providerIds.add(coverage.insuranceProviderId());
            }
        }

        if (updateCoverages != null) {
            for (UpdateProductInsuranceCoverageInput coverage : updateCoverages) {
                if (coverage == null || coverage.insuranceProviderId() == null) {
                    return ApiResponse.error("insuranceProviderId is required for each coverage.");
                }
                providerIds.add(coverage.insuranceProviderId());
            }
        }

        Set<UUID> uniqueProviderIds = new HashSet<>(providerIds);
        if (uniqueProviderIds.size() != providerIds.size()) {
            return ApiResponse.error("Duplicate insuranceProviderId in insuranceCoverages is not allowed.");
        }

        Map<UUID, InsuranceProvider> providers = new HashMap<>();
        for (UUID providerId : uniqueProviderIds) {
            Optional<InsuranceProvider> providerOptional = insuranceProviderRepository.findById(providerId);
            if (providerOptional.isEmpty()) {
                return ApiResponse.error("Insurance provider not found: " + providerId);
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