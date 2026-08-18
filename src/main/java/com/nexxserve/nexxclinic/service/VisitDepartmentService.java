package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentProfile;
import com.nexxserve.nexxclinic.entity.DepartmentProfileProduct;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitPreInstruction;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionMedication;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionProductRequest;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionsInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductStatusInput;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.mappers.out.*;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentProfileProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentMedicationRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitDepartmentService {

    private static final Logger log = LoggerFactory.getLogger(VisitDepartmentService.class);

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    private final VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;
    private final com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final DepartmentProfileProductRepository departmentProfileProductRepository;

    private final WorkerMapper workerMapper;
    private final DepartmentMapper departmentMapper;
    private final ProductMapper productMapper;

    // Lazy to break circular dependency with VisitService
    private final VisitDepartmentNoteService visitDepartmentNoteService;
    private final VisitService visitService;

    public VisitDepartmentService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository,
            VisitDepartmentMedicationRepository visitDepartmentMedicationRepository,
            com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository,
            DepartmentRepository departmentRepository,
            ProductRepository productRepository,
            WorkerRepository workerRepository,
            VisitBillingItemRepository visitBillingItemRepository,
            com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository visitDepartmentBillingRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            DepartmentProfileRepository departmentProfileRepository,
            DepartmentProfileProductRepository departmentProfileProductRepository,
            WorkerMapper workerMapper,
            DepartmentMapper departmentMapper,
            ProductMapper productMapper,
            @Lazy VisitDepartmentNoteService visitDepartmentNoteService,
            @Lazy VisitService visitService
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitDepartmentDiagnosisRepository = visitDepartmentDiagnosisRepository;
        this.visitDepartmentMedicationRepository = visitDepartmentMedicationRepository;
        this.visitPreInstructionRepository = visitPreInstructionRepository;
        this.departmentRepository = departmentRepository;
        this.productRepository = productRepository;
        this.workerRepository = workerRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.departmentProfileRepository = departmentProfileRepository;
        this.departmentProfileProductRepository = departmentProfileProductRepository;
        this.workerMapper = workerMapper;
        this.departmentMapper = departmentMapper;
        this.productMapper = productMapper;
        this.visitDepartmentNoteService = visitDepartmentNoteService;
        this.visitService = visitService;
    }

    // ─────────────────────────────────────────────────────────────
    //  VISIT DEPARTMENT MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDto> addVisitDepartment(UUID visitId, UUID departmentId, UUID profileId, com.nexxserve.nexxclinic.model.EncounterType encounterType, UUID processorId, AuthenticatedUser authUser) {
        if (visitId == null || departmentId == null) {
            return ApiResponse.error("visitId and departmentId are required.");
        }

        // Lock the visit row (PESSIMISTIC_WRITE) before the duplicate check: two
        // concurrent adds of the same (visit, department) would otherwise both pass
        // existsByVisitIdAndDepartmentId and one would 500 on uk_visit_department.
        // Serializing on the visit row makes the second request see the first's
        // committed department and return the friendly error below.
        Optional<Visit> visitOptional = visitRepository.findByIdForUpdate(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add departments to a completed visit.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add departments to a cancelled visit.");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.");
        }

        if (visitDepartmentRepository.existsByVisitIdAndDepartmentId(visitId, departmentId)) {
            return ApiResponse.error("Department is already added to this visit.");
        }

        Department department = departmentOptional.get();

        // Profiles are NEVER auto-applied anymore: a department is added without a
        // profile unless an explicit profileId is provided, and only departments
        // that do not support requests can have a profile set.
        DepartmentProfile profile = null;
        if (profileId != null) {
            if (department.isSupportRequests()) {
                return ApiResponse.error("Profiles can only be set on departments that do not support requests.");
            }
            profile = resolveProfileForDepartment(departmentId, profileId);
            if (profile == null) {
                return ApiResponse.error("Profile not found or does not belong to this department.");
            }
        }

        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);
        visitDepartment.setProfile(profile);
        if (encounterType != null) {
            visitDepartment.setEncounterType(encounterType);
        }
        visitDepartment.setStatus(VisitDepartmentStatus.PENDING);

        if (processorId != null) {
            Optional<Worker> processorOptional = workerRepository.findById(processorId);
            if (processorOptional.isEmpty()) {
                return ApiResponse.error("Processor not found.");
            }
            addProcessorToVisitDepartment(visitDepartment, processorOptional.get());
        }

        VisitDepartment saved;
        try {
            // saveAndFlush (not save): uk_visit_department is only checked at flush
            // time, so a plain save() would defer the violation past this catch.
            saved = visitDepartmentRepository.saveAndFlush(visitDepartment);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // uk_visit_department (visit_id, department_id): a concurrent request
            // added the same department between the check above and this save.
            log.debug("Concurrent visit department addition detected for visit {} and department {}: {}",
                visitId, departmentId, ex.getMessage());
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error("Department is already added to this visit.");
        }

        // Auto-add the profile's products (source=PROFILE) when a profile is applied.
        if (profile != null) {
            Worker actingUser = resolveWorker(authUser);
            ApiResponse applyError = applyProfileProducts(saved, profile, actingUser);
            if (applyError != null) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return applyError;
            }
        }

        // Return refreshed visit DTO via visitRepository – caller can map if needed
        Visit refreshed = visitRepository.findById(visitId).orElse(visit);
        return ApiResponse.success("Department added to visit.", visitService.visitToDto(refreshed, null, authUser));
    }

    /**
     * Changes the profile used by a visit department, swapping its PROFILE-sourced
     * products for the new profile's products. USER-sourced products (added manually)
     * are untouched. Profile products cannot be removed individually — this is the
     * only way to replace them.
     */
    @Transactional
    public ApiResponse<VisitDepartmentDto> changeVisitDepartmentProfile(UUID visitDepartmentId, UUID profileId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDepartment = departmentOptional.get();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot change the profile on a completed department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot change the profile on a cancelled department.");
        }
        // D2-style guard: a department that has been billed is frozen for finance.
        if (visitDepartment.getStatus() == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(visitDepartment.getId())) {
            return ApiResponse.error("Cannot change the profile of a billed department. Use editBillVisit to correct the billing.");
        }

        UUID departmentId = visitDepartment.getDepartment() == null ? null : visitDepartment.getDepartment().getId();
        if (departmentId == null) {
            return ApiResponse.error("Visit department has no linked department.");
        }

        // Profiles can only be set on departments that do not support requests.
        if (profileId != null && visitDepartment.getDepartment().isSupportRequests()) {
            return ApiResponse.error("Profiles can only be set on departments that do not support requests.");
        }

        DepartmentProfile profile = resolveProfileForDepartment(departmentId, profileId);
        if (profileId != null && profile == null) {
            return ApiResponse.error("Profile not found or does not belong to this department.");
        }

        // Remove existing PROFILE-sourced products (they are managed by the profile).
        List<VisitDepartmentProduct> profileProducts = visitDepartmentProductRepository
            .findByVisitDepartmentId(visitDepartment.getId())
            .stream()
            .filter(p -> p.getSource() == VisitDepartmentProductSource.PROFILE)
            .toList();
        if (!profileProducts.isEmpty()) {
            // Never hard-delete a profile product that has billing history.
            for (VisitDepartmentProduct p : profileProducts) {
                if (!visitBillingItemRepository.findByVisitDepartmentProductId(p.getId()).isEmpty()) {
                    return ApiResponse.error(
                        "Cannot change the profile: profile products have billing history. Use editBillVisit to correct the billing instead."
                    );
                }
            }
            visitDepartmentProductRepository.deleteAll(profileProducts);
        }

        visitDepartment.setProfile(profile);
        VisitDepartment saved = visitDepartmentRepository.save(visitDepartment);

        if (profile != null) {
            Worker actingUser = resolveWorker(authUser);
            ApiResponse applyError = applyProfileProducts(saved, profile, actingUser);
            if (applyError != null) {
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return applyError;
            }
        }

        // A child department whose PROFILE products were just removed may now be
        // empty — children can never exist with zero products (same rule as the
        // product-removal paths). Map to the parent so the response stays valid.
        VisitDepartment mapped = deleteChildVisitDepartmentIfEmpty(saved);
        return ApiResponse.success(
            "Visit department profile changed.",
            visitDepartmentToDto(mapped == null ? saved : mapped)
        );
    }

    private DepartmentProfile resolveProfileForDepartment(UUID departmentId, UUID profileId) {
        if (profileId == null) {
            return null; // profiles are only ever set explicitly — never auto-applied
        }
        return departmentProfileRepository.findById(profileId)
            .filter(p -> p.getDepartment() != null && departmentId.equals(p.getDepartment().getId()))
            .orElse(null);
    }

    /**
     * Applies an explicitly provided profile to a visit department, adding its
     * products as source=PROFILE. Profiles are never auto-applied: a null
     * {@code profileId} leaves the department without a profile. Only departments
     * that do not support requests can have a profile set.
     */
    public ApiResponse applyProfileToVisitDepartment(VisitDepartment visitDepartment, UUID profileId, Worker actingUser) {
        if (visitDepartment == null || visitDepartment.getDepartment() == null) {
            return ApiResponse.error("Visit department is required.");
        }
        if (profileId == null) {
            visitDepartment.setProfile(null);
            return null;
        }
        if (visitDepartment.getDepartment().isSupportRequests()) {
            return ApiResponse.error("Profiles can only be set on departments that do not support requests.");
        }
        DepartmentProfile profile = resolveProfileForDepartment(visitDepartment.getDepartment().getId(), profileId);
        if (profile == null) {
            return ApiResponse.error("Profile not found or does not belong to this department.");
        }
        visitDepartment.setProfile(profile);
        return applyProfileProducts(visitDepartment, profile, actingUser);
    }

    /**
     * Adds the products of {@code profile} to the visit department as source=PROFILE.
     * Products already present (by product id, non-deleted) are skipped.
     */
    private ApiResponse applyProfileProducts(VisitDepartment visitDepartment, DepartmentProfile profile, Worker actingUser) {
        if (visitDepartment == null || profile == null) {
            return null;
        }
        List<DepartmentProfileProduct> links = departmentProfileProductRepository.findByProfileId(profile.getId());
        if (links == null || links.isEmpty()) {
            return null; // a profile may have zero products
        }

        for (DepartmentProfileProduct link : links) {
            if (link == null || link.getProduct() == null) {
                continue;
            }
            Optional<VisitDepartmentProduct> existing = visitDepartmentProductRepository
                .findByVisitDepartmentIdAndProductId(visitDepartment.getId(), link.getProduct().getId());
            if (existing.isPresent()) {
                continue;
            }

            VisitDepartmentProduct item = new VisitDepartmentProduct();
            item.setVisitDepartment(visitDepartment);
            item.setProduct(link.getProduct());
            item.setQuantity(BigDecimal.ONE);
            item.setStatus(VisitProductStatus.PENDING);
            item.setSource(VisitDepartmentProductSource.PROFILE);
            item.setAddedBy(actingUser);

            ApiResponse processorError = assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, null);
            if (processorError != null) {
                return processorError;
            }
            try {
                // saveAndFlush: the partial unique index is only checked at flush time;
                // a plain save() would defer the violation past this catch.
                visitDepartmentProductRepository.saveAndFlush(item);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Partial unique index (visit_department_id, product_id): concurrent
                // profile application or explicit add raced this insert.
                log.debug("Concurrent profile product addition detected for visitDepartment {} and product {}: {}",
                    visitDepartment.getId(), link.getProduct().getId(), ex.getMessage());
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return ApiResponse.error(
                    "Product already exists in this visit department."
                );
            }
        }
        return null;
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addChildVisitDepartment(AddChildVisitDepartmentInput input, AuthenticatedUser authUser) {
        if (input == null || input.parentVisitDepartmentId() == null || input.departmentId() == null) {
            return ApiResponse.error("parentVisitDepartmentId and departmentId are required.");
        }
        boolean hasExplicitProducts = input.products() != null && !input.products().isEmpty();
        // Child departments always support requests, and profiles can only be set on
        // departments that do NOT support requests — so children can never use a
        // profile and must come with at least one explicit product.
        if (input.profileId() != null) {
            return ApiResponse.error("Profiles cannot be used on child departments. Add products instead.");
        }
        if (!hasExplicitProducts) {
            // Children can never exist with zero products.
            return ApiResponse.error("At least one product is required for a child department.");
        }

        Optional<VisitDepartment> parentOptional = visitDepartmentRepository.findByIdForUpdate(input.parentVisitDepartmentId());
        if (parentOptional.isEmpty()) {
            return ApiResponse.error("Parent visit department not found.");
        }

        VisitDepartment parent = parentOptional.get();
        if (parent.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add child departments to a completed department.");
        }
        if (parent.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add child departments to a cancelled department.");
        }
        // S5: a department in BILLING status is frozen for finance; new products must
        // go through editBillVisit so a new billing version captures them.
        if (parent.getStatus() == VisitDepartmentStatus.BILLING) {
            return ApiResponse.error("Cannot add child departments to a department in BILLING status. Use editBillVisit to correct the billing.");
        }

        if (parent.getDepartment() != null && parent.getDepartment().getId() != null && parent.getDepartment().getId().equals(input.departmentId())) {
            return ApiResponse.error("A department cannot be added as a child of itself.");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(input.departmentId());
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.");
        }

        Department childDepartment = departmentOptional.get();
        if (!childDepartment.isSupportRequests()) {
            return ApiResponse.error("Only support request departments can be added as children.");
        }

        Visit visit = parent.getVisit();
        if (visit == null) {
            return ApiResponse.error("Visit not found.");
        }

        // Lock the visit row too: uk_visit_department is unique on (visit_id,
        // department_id) — NOT parent-scoped — so two concurrent child adds of the
        // same department under DIFFERENT parents both pass the parent-scoped
        // existsBy check below and one would 500. The visit lock serializes them;
        // the try/catch on the save below is the belt-and-braces backstop.
        Optional<Visit> lockedVisit = visitRepository.findByIdForUpdate(visit.getId());
        if (lockedVisit.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }
        visit = lockedVisit.get();

        if (visitDepartmentRepository.existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(visit.getId(), input.departmentId(), input.parentVisitDepartmentId())) {
            return ApiResponse.error("Child department already exists for this parent.");
        }

        Worker actingUser = resolveWorker(authUser);
        Set<UUID> seenProducts = new LinkedHashSet<>();

        // Validate all products before creating
        for (var productInput : input.products()) {
            if (productInput == null || productInput.productId() == null) {
                return ApiResponse.error("productId is required for each product.");
            }
            if (productInput.quantity() == null) {
                return ApiResponse.error("quantity is required for each product.");
            }
            if (!seenProducts.add(productInput.productId())) {
                return ApiResponse.error("Duplicate productId found in products.");
            }
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.");
            }
        }

        if (input.processorId() != null) {
            VisitDepartment tempChild = new VisitDepartment();
            tempChild.setVisit(visit);
            tempChild.setDepartment(childDepartment);
            tempChild.setParentVisitDepartment(parent);
            tempChild.setStatus(VisitDepartmentStatus.PENDING);

            List<Worker> processors = resolveAvailableProcessorsForProductAssignment(tempChild);
            Worker requestedProcessor = findProcessorById(processors, input.processorId());
            if (requestedProcessor == null) {
                return ApiResponse.error("processorId must belong to the visit department processors.");
            }
        }

        VisitDepartment child = new VisitDepartment();
        child.setVisit(visit);
        child.setDepartment(childDepartment);
        child.setParentVisitDepartment(parent);
        if (input.encounterType() != null) {
            child.setEncounterType(input.encounterType());
        }
        child.setStatus(VisitDepartmentStatus.PENDING);

        VisitDepartment savedChild;
        try {
            // saveAndFlush (not save): uk_visit_department is only checked at flush
            // time, so a plain save() would defer the violation past this catch.
            savedChild = visitDepartmentRepository.saveAndFlush(child);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // uk_visit_department (visit_id, department_id): a concurrent add of the
            // same department (same or different parent) raced us.
            log.debug("Concurrent visit department addition detected for visit {} and department {}: {}",
                visit.getId(), input.departmentId(), ex.getMessage());
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error("Department already exists in this visit.");
        }

        // hasExplicitProducts is guaranteed true here (guards above reject the
        // no-product case), so the loop always runs.
        for (var productInput : input.products()) {
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.");
            }

            // Skip products already present (same dedupe rule as
            // addVisitDepartmentProduct / addProductsToVisitDepartment).
            if (visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(
                    savedChild.getId(), productInput.productId()).isPresent()) {
                continue;
            }

            VisitDepartmentProduct item = new VisitDepartmentProduct();
            item.setVisitDepartment(savedChild);
            item.setProduct(productOptional.get());
            item.setQuantity(normalizeQuantity(BigDecimal.valueOf(productInput.quantity())));
            item.setStatus(VisitProductStatus.PENDING);

            ApiResponse processorError = assignVisitDepartmentProductProcessor(savedChild, item, actingUser, input.processorId());
            if (processorError != null) {
                // C2 fix: the child department was already saved above. Returning an error
                // ApiResponse would COMMIT it (Spring only rolls back on exceptions),
                // leaving an empty child department behind. Mark rollback-only.
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return processorError;
            }

            item.setAddedBy(actingUser);
            try {
                // saveAndFlush: unique index only checked at flush time.
                visitDepartmentProductRepository.saveAndFlush(item);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Partial unique index: concurrent add of the same product raced us.
                log.debug("Concurrent product addition detected for visitDepartment {} and product {}: {}",
                    savedChild.getId(), productInput.productId(), ex.getMessage());
                org.springframework.transaction.interceptor.TransactionAspectSupport
                    .currentTransactionStatus()
                    .setRollbackOnly();
                return ApiResponse.error(
                    "Product already exists in this child visit department."
                );
            }
        }

        // A child department can never exist with zero products. Roll back if nothing
        // was actually added.
        List<VisitDepartmentProduct> childProducts = visitDepartmentProductRepository.findByVisitDepartmentId(savedChild.getId());
        if (childProducts.isEmpty()) {
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error("Child department cannot be created with zero products.");
        }

        return ApiResponse.success("Child visit department added.", visitDepartmentToDto(parent));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeChildVisitDepartment(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment child = departmentOptional.get();
        if (child.getParentVisitDepartment() == null) {
            return ApiResponse.error("Only child visit departments can be removed with this mutation.");
        }

        // N1 fix: never hard-delete a child department that still has products (active
        // or soft-deleted) or billing history. Deleting it would leave a dangling
        // FK on visit_department_products (nullable=false) and orphan the billing rows.
        List<VisitDepartmentProduct> childProducts = visitDepartmentProductRepository
            .findByVisitDepartmentIdIncludingDeleted(child.getId());
        if (!childProducts.isEmpty()) {
            return ApiResponse.error(
                "Cannot remove a child department that still has products. " +
                "Remove or re-bill them first (use editBillVisit for billed products)."
            );
        }

        // FK guard: a child with clinical/financial dependents (notes, diagnoses,
        // medications, pre-instructions, billing) cannot be hard-deleted — deleting it
        // would throw DataIntegrityViolationException -> 500.
        if (!visitDepartmentDiagnosisRepository.findByVisitDepartmentId(child.getId()).isEmpty()
                || !visitDepartmentMedicationRepository.findByVisitDepartmentId(child.getId()).isEmpty()
                || !visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(child.getId()).isEmpty()
                || visitDepartmentNoteServiceHasNotes(child.getId())
                || visitDepartmentBillingRepository.existsByVisitDepartmentId(child.getId())) {
            return ApiResponse.error(
                "Cannot remove a child department that has notes, diagnoses, medications, pre-instructions or billing history. " +
                "Use editBillVisit to correct the billing instead."
            );
        }

        VisitDepartment parent = child.getParentVisitDepartment();
        visitDepartmentRepository.delete(child);
        return ApiResponse.success("Child visit department removed.", visitDepartmentToDto(parent));
    }

    // ─────────────────────────────────────────────────────────────
    //  PRODUCTS
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDepartmentDto> addVisitDepartmentProduct(CreateVisitDepartmentProductInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null || input.departmentId() == null || input.productId() == null) {
            return ApiResponse.error("visitId, departmentId and productId are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add products to a completed visit.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled visit.");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findByVisitIdAndDepartmentId(input.visitId(), input.departmentId());
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Department is not linked to this visit.");
        }

        // Lock for update to prevent TOCTOU race with consultation completion
        VisitDepartment visitDepartment = visitDepartmentRepository.findByIdForUpdate(visitDepartmentOptional.get().getId())
                .orElseThrow(() -> new RuntimeException("Visit department disappeared while locking"));

        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add products to a completed department.");
        }

        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled department.");
        }

        Optional<Product> productOptional = productRepository.findById(input.productId());
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.");
        }

        Optional<VisitDepartmentProduct> existing = visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(
                visitDepartment.getId(), input.productId()
        );
        if (existing.isPresent()) {
            return ApiResponse.error("Product already exists for this visit department.");
        }

        // S4: BILLED/EXEMPTED/CORRECTION_PENDING are managed exclusively by the billing
        // service; clients may only create products as PENDING (default) or UNPAID.
        VisitProductStatus requestedStatus = input.status() == null
                ? VisitProductStatus.PENDING
                : input.status();
        if (requestedStatus == VisitProductStatus.BILLED
                || requestedStatus == VisitProductStatus.EXEMPTED
                || requestedStatus == VisitProductStatus.CORRECTION_PENDING) {
            return ApiResponse.error("Status " + requestedStatus + " cannot be set manually. Only PENDING or UNPAID can be set when adding a product.");
        }

        // SD1: if the product exists but was soft-deleted, restore it instead of
        // creating a new row.
        VisitDepartmentProduct item = visitDepartmentProductRepository
            .findByVisitDepartmentIdAndProductIdIncludingDeleted(
                visitDepartment.getId(),
                input.productId()
            )
            .orElse(null);

        if (item != null && !item.isDeleted()) {
            return ApiResponse.error("Product already exists for this visit department.");
        }

        if (item == null) {
            item = new VisitDepartmentProduct();
            item.setVisitDepartment(visitDepartment);
            item.setProduct(productOptional.get());
        }

        item.setDeleted(false);
        item.setQuantity(normalizeQuantity(input.quantity()));
        item.setStatus(requestedStatus);

        Worker actingUser = resolveWorker(authUser);
        ApiResponse processorError = assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, input.processorId());
        if (processorError != null) {
            return processorError;
        }
        item.setAddedBy(actingUser);
        if (item.getStatus() != VisitProductStatus.PENDING) {
            item.setBilledBy(actingUser);
        }

        // N6 fix: this method already rejects COMPLETED visits at the top, so the
        // previous reopenVisitIfCompleted(visit) call here was unreachable dead code.
        try {
            // saveAndFlush: unique index only checked at flush time.
            visitDepartmentProductRepository.saveAndFlush(item);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // Partial unique index (visit_department_id, product_id): the check above
            // raced with a concurrent add of the same product.
            log.debug("Concurrent product addition detected for visitDepartment {} and product {}: {}",
                visitDepartment.getId(), input.productId(), ex.getMessage());
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return ApiResponse.error(
                "Product already exists for this visit department."
            );
        }
        return ApiResponse.success("Product added to visit department.", visitDepartmentToDto(visitDepartment));
    }

    @Transactional
    public ApiResponse updateVisitDepartmentProductStatus(UpdateVisitDepartmentProductStatusInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentProductId() == null || input.status() == null) {
            return ApiResponse.error("visitDepartmentProductId and status are required.");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(input.visitDepartmentProductId());
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOptional.get();
        // D2 fix: products in a department that has been BILLED are frozen for finance.
        // Direct edits here would desync the clinical view from the bill. Use editBillVisit.
        // (A BILLING-status department that was never actually billed is still editable —
        //  it can be reopened via updateVisitDepartmentStatus, matching the D1 rule.)
        if (item.getVisitDepartment().getStatus() == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(item.getVisitDepartment().getId())) {
            return ApiResponse.error("Cannot change the status of a product in a billed department. Use editBillVisit to correct the billing.");
        }
        // S4: BILLED/EXEMPTED/CORRECTION_PENDING are managed exclusively by the billing
        // service. Externally, product status is effectively read-only for those values
        // (only PENDING and UNPAID are externally settable).
        VisitProductStatus requestedStatus = input.status();
        if (requestedStatus == VisitProductStatus.BILLED
                || requestedStatus == VisitProductStatus.EXEMPTED
                || requestedStatus == VisitProductStatus.CORRECTION_PENDING) {
            return ApiResponse.error("Status " + requestedStatus + " can only be set by the billing service. Allowed external statuses: PENDING, UNPAID.");
        }

        // F5 fix: billedBy is only meaningful for BILLED/EXEMPTED, which are set
        // exclusively by the billing service. Flipping PENDING -> UNPAID here previously
        // stamped a bogus billedBy on an unbilled product.
        item.setStatus(requestedStatus);

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        deleteChildVisitDepartmentIfEmpty(saved.getVisitDepartment());
        return ApiResponse.success("Visit department product status updated.", visitDepartmentToDto(saved.getVisitDepartment()));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateVisitDepartmentProductQuantity(com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductQuantityInput input) {
        if (input == null || input.visitDepartmentProductId() == null || input.quantity() == null) {
            return ApiResponse.error("visitDepartmentProductId and quantity are required.");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(input.visitDepartmentProductId());
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOptional.get();
        Visit visit = item.getVisitDepartment().getVisit();

        // D2 fix: products in a department that has been BILLED are frozen for finance.
        if (item.getVisitDepartment().getStatus() == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(item.getVisitDepartment().getId())) {
            return ApiResponse.error("Cannot change the quantity of a product in a billed department. Use editBillVisit to correct the billing.");
        }

        // E4: a BILLED/EXEMPTED product's live quantity must not diverge from its
        // billing snapshot. Quantity corrections go through editBillVisit.
        if (item.getStatus() == VisitProductStatus.BILLED
                || item.getStatus() == VisitProductStatus.EXEMPTED) {
            return ApiResponse.error("Cannot change the quantity of a billed or exempted product. Use editBillVisit to correct the billing.");
        }

        if (input.quantity().compareTo(BigDecimal.ZERO) <= 0) {
            // Profile products are managed by the profile — they cannot be removed
            // individually (quantity-to-zero is a removal). Change the profile instead.
            if (item.getSource() == VisitDepartmentProductSource.PROFILE) {
                return ApiResponse.error("Profile products cannot be removed individually. Change the visit department's profile instead.");
            }
            // B5: never hard-delete a product that has billing history.
            List<VisitBillingItem> billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(item.getId());
            if (!billingItems.isEmpty()) {
                return ApiResponse.error("Cannot remove a product that has billing history. Use editBillVisit to correct the billing instead.");
            }
            VisitDepartment affectedDepartment = item.getVisitDepartment();
            visitDepartmentProductRepository.delete(item);
            reopenVisitIfCompleted(visit);
            VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartment);
            return ApiResponse.success("Visit department product removed.", visitDepartmentToDto(mappedDepartment == null ? affectedDepartment : mappedDepartment));
        }

        item.setQuantity(input.quantity());

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        deleteChildVisitDepartmentIfEmpty(saved.getVisitDepartment());
        return ApiResponse.success("Visit department product quantity updated.", visitDepartmentToDto(saved.getVisitDepartment()));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeVisitDepartmentProduct(UUID visitDepartmentProductId) {
        if (visitDepartmentProductId == null) {
            return ApiResponse.error("visitDepartmentProductId is required.");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(visitDepartmentProductId);
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOptional.get();
        Visit visit = item.getVisitDepartment().getVisit();
        VisitDepartment affectedDepartment = item.getVisitDepartment();

        // Profile products are managed by the profile — they cannot be removed
        // individually. Only changeVisitDepartmentProfile can replace them.
        if (item.getSource() == VisitDepartmentProductSource.PROFILE) {
            return ApiResponse.error("Profile products cannot be removed individually. Change the visit department's profile instead.");
        }

        // D2 fix: products in a department that has been BILLED are frozen for finance.
        if (affectedDepartment.getStatus() == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(affectedDepartment.getId())) {
            return ApiResponse.error("Cannot remove a product from a billed department. Use editBillVisit to correct the billing.");
        }

        // B5: never hard-delete a product that has billing history. If a billing item
        // references this row, a hard delete would leave a dangling FK in
        // visit_billing_items and corrupt invoice generation. Use editBillVisit.
        List<VisitBillingItem> billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(item.getId());
        if (!billingItems.isEmpty()) {
            return ApiResponse.error("Cannot remove a product that has billing history. Use editBillVisit to correct the billing instead.");
        }

        visitDepartmentProductRepository.delete(item);
        reopenVisitIfCompleted(visit);

        VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartment);
        return ApiResponse.success("Visit department product removed.", visitDepartmentToDto(mappedDepartment == null ? affectedDepartment : mappedDepartment));
    }

    // ─────────────────────────────────────────────────────────────
    //  STATUS / PROCESSOR / ENCOUNTER TYPE
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse updateVisitDepartmentStatus(com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentId() == null || input.status() == null) {
            return ApiResponse.error("visitDepartmentId and status are required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment department = departmentOptional.get();
        // D1 fix: enforce a transition guard. Terminal states (COMPLETED/CANCELLED) and
        // the finance-frozen state (BILLING) must not be changed through this generic
        // mutation — otherwise a client could re-open a BILLING department (re-enabling
        // product/clinical edits after finance started, undoing the S5 guards) or fake a
        // COMPLETED department while products are still unbilled.
        VisitDepartmentStatus currentStatus = department.getStatus();
        VisitDepartmentStatus requestedStatus = input.status();
        if (requestedStatus == null) {
            return ApiResponse.error("status is required.");
        }
        if (currentStatus == VisitDepartmentStatus.COMPLETED
                || currentStatus == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("A " + currentStatus + " department is terminal and cannot be changed.");
        }
        if (currentStatus == VisitDepartmentStatus.BILLING
                && requestedStatus != VisitDepartmentStatus.COMPLETED
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(department.getId())) {
            return ApiResponse.error("A department that has been billed is frozen. Use editBillVisit to correct the billing.");
        }
        if (requestedStatus == VisitDepartmentStatus.COMPLETED) {
            List<VisitDepartmentProduct> products = visitDepartmentProductRepository
                .findByVisitDepartmentId(department.getId());
            boolean hasUnbilled = products.stream().anyMatch(p ->
                p.getStatus() == VisitProductStatus.PENDING
                    || p.getStatus() == VisitProductStatus.UNPAID
                    || p.getStatus() == VisitProductStatus.CORRECTION_PENDING);
            if (hasUnbilled) {
                return ApiResponse.error("Cannot complete a department with unbilled products. All products must be billed first.");
            }
        }
        department.setStatus(requestedStatus);
        if (requestedStatus == VisitDepartmentStatus.COMPLETED) {
            department.setCompletedAt(java.time.LocalDateTime.now());
        }

        if (requestedStatus == VisitDepartmentStatus.ACTIVE) {
            Worker actingUser = resolveWorker(authUser);
            if (actingUser != null) {
                addProcessorToVisitDepartment(department, actingUser);
            }
        }

        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department status updated.", visitDepartmentToDto(saved));
    }

    @Transactional
    public ApiResponse addVisitDepartmentProcessor(UUID visitDepartmentId, UUID processorId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null || processorId == null) {
            return ApiResponse.error("visitDepartmentId and processorId are required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot modify processors on a completed department.");
        }
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot modify processors on a cancelled department.");
        }

        Optional<Worker> processorOptional = workerRepository.findById(processorId);
        if (processorOptional.isEmpty()) {
            return ApiResponse.error("Processor not found.");
        }

        addProcessorToVisitDepartment(department, processorOptional.get());
        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department processor added.", visitDepartmentToDto(saved));
    }

    @Transactional
    public ApiResponse removeVisitDepartmentProcessor(UUID visitDepartmentId, UUID processorId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null || processorId == null) {
            return ApiResponse.error("visitDepartmentId and processorId are required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot modify processors on a completed department.");
        }
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot modify processors on a cancelled department.");
        }

        if (department.getProcessors() == null || department.getProcessors().isEmpty()) {
            return ApiResponse.error("Visit department has no processors.");
        }

        boolean removed = department.getProcessors().removeIf(worker -> worker != null && processorId.equals(worker.getId()));
        if (!removed) {
            return ApiResponse.error("Processor not found on this visit department.");
        }

        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department processor removed.", visitDepartmentToDto(saved));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateVisitDepartmentEncounterType(UUID visitDepartmentId, com.nexxserve.nexxclinic.model.EncounterType encounterType, AuthenticatedUser authUser) {
        if (visitDepartmentId == null || encounterType == null) {
            return ApiResponse.error("visitDepartmentId and encounterType are required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot modify encounter type on a completed department.");
        }
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot modify encounter type on a cancelled department.");
        }

        department.setEncounterType(encounterType);
        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department encounter type updated.", visitDepartmentToDto(saved));
    }

    // ─────────────────────────────────────────────────────────────
    //  DIAGNOSIS & MEDICATION
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDepartmentDto> addDiagnosisToVisitDepartment(AddDiagnosisInput input) {
        if (input == null || input.visitDepartmentId() == null || input.diagnosisName() == null || input.diagnosisName().isBlank()) {
            return ApiResponse.error("visitDepartmentId and diagnosisName are required.");
        }

        UUID visitDeptId;
        try {
            visitDeptId = UUID.fromString(input.visitDepartmentId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid visitDepartmentId format.");
        }

        Optional<VisitDepartment> visitDeptOptional = visitDepartmentRepository.findById(visitDeptId);
        if (visitDeptOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDept = visitDeptOptional.get();
        if (visitDept.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add diagnostics to a completed department.");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add diagnostics to a cancelled department.");
        }
        // C1: once a department is handed to finance (BILLING), clinical data is frozen.
        if (visitDept.getStatus() == VisitDepartmentStatus.BILLING) {
            return ApiResponse.error("Cannot add diagnostics to a department in BILLING status.");
        }

        VisitDepartmentDiagnosis diagnosis = new VisitDepartmentDiagnosis();
        diagnosis.setVisitDepartment(visitDept);
        diagnosis.setDiagnosisName(input.diagnosisName().trim());
        diagnosis.setIcd11Code(input.icd11Code() == null || input.icd11Code().isBlank() ? null : input.icd11Code().trim());

        visitDepartmentDiagnosisRepository.save(diagnosis);
        return ApiResponse.success("Diagnosis added successfully.", visitDepartmentToDto(visitDept));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addMedicationToVisitDepartment(AddMedicationInput input) {
        if (input == null || input.visitDepartmentId() == null || input.medicationName() == null || input.medicationName().isBlank() || input.instructions() == null || input.instructions().isBlank()) {
            return ApiResponse.error("visitDepartmentId, medicationName and instructions are required.");
        }

        UUID visitDeptId;
        try {
            visitDeptId = UUID.fromString(input.visitDepartmentId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid visitDepartmentId format.");
        }

        Optional<VisitDepartment> visitDeptOptional = visitDepartmentRepository.findById(visitDeptId);
        if (visitDeptOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDept = visitDeptOptional.get();
        if (visitDept.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add medication to a completed department.");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add medication to a cancelled department.");
        }
        // C1: once a department is handed to finance (BILLING), clinical data is frozen.
        if (visitDept.getStatus() == VisitDepartmentStatus.BILLING) {
            return ApiResponse.error("Cannot add medication to a department in BILLING status.");
        }

        VisitDepartmentMedication medication = new VisitDepartmentMedication();
        medication.setVisitDepartment(visitDept);
        medication.setMedicationName(input.medicationName().trim());
        medication.setInstructions(input.instructions().trim());

        visitDepartmentMedicationRepository.save(medication);
        return ApiResponse.success("Medication added successfully.", visitDepartmentToDto(visitDept));
    }

    // ─────────────────────────────────────────────────────────────
    //  PRE-INSTRUCTIONS
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDepartmentDto> addVisitPreInstructions(AddVisitPreInstructionsInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentId() == null || input.items() == null || input.items().isEmpty()) {
            return ApiResponse.error("visitDepartmentId and items are required.");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDepartment = visitDepartmentOptional.get();
        Visit visit = visitDepartment.getVisit();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add pre-instructions to a completed department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add pre-instructions to a cancelled department.");
        }
        // C1: once a department is handed to finance (BILLING), clinical data is frozen.
        if (visitDepartment.getStatus() == VisitDepartmentStatus.BILLING) {
            return ApiResponse.error("Cannot add pre-instructions to a department in BILLING status.");
        }

        Worker actingUser = resolveWorker(authUser);
        List<VisitPreInstruction> items = new ArrayList<>();
        for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionItemInput itemInput : input.items()) {
            if (itemInput == null || itemInput.type() == null || itemInput.type().isBlank()) {
                return ApiResponse.error("Each item must have a type (NOTE or MEDICATION).");
            }
            String type = itemInput.type().trim().toUpperCase();
            if (!type.equals("NOTE") && !type.equals("MEDICATION") && !type.equals("PRODUCT")) {
                return ApiResponse.error("Item type must be NOTE, MEDICATION or PRODUCT.");
            }

            VisitPreInstruction pi = new VisitPreInstruction();
            pi.setVisit(visit);
            pi.setVisitDepartment(visitDepartment);
            pi.setType(type);
            pi.setNote(itemInput.note());
            pi.setAddedBy(actingUser);

            if (type.equals("MEDICATION")) {
                if (itemInput.medications() == null || itemInput.medications().isEmpty()) {
                    return ApiResponse.error("Medication items must include at least one medication entry.");
                }
                for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionMedicationInput medIn : itemInput.medications()) {
                    if (medIn == null || medIn.medName() == null || medIn.medName().isBlank()) {
                        return ApiResponse.error("medName is required for medication entries.");
                    }
                    VisitPreInstructionMedication med = new VisitPreInstructionMedication();
                    med.setPreInstruction(pi);
                    med.setMedName(medIn.medName().trim());
                    med.setDosage(medIn.dosage());
                    med.setRoute(medIn.route());
                    med.setFrequency(medIn.frequency());
                    med.setDuration(medIn.duration());
                    med.setQuantity(medIn.quantity());
                    med.setOtherInstructions(medIn.otherInstructions());
                    pi.getMedications().add(med);
                }
            }
            if (type.equals("PRODUCT")) {
                if (itemInput.products() == null || itemInput.products().isEmpty()) {
                    return ApiResponse.error("Product items must include at least one product entry.");
                }
                for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionProductInput prodIn : itemInput.products()) {
                    if (prodIn == null || prodIn.productId() == null) {
                        return ApiResponse.error("productId is required for product entries.");
                    }
                    Optional<Product> productOptional = productRepository.findById(prodIn.productId());
                    if (productOptional.isEmpty()) {
                        return ApiResponse.error("Product not found.");
                    }
                    VisitPreInstructionProductRequest pr = new VisitPreInstructionProductRequest();
                    pr.setPreInstruction(pi);
                    pr.setProduct(productOptional.get());
                    pr.setQuantity(prodIn.quantity());
                    pr.setRequestedBy(actingUser);
                    pr.setStatus(com.nexxserve.nexxclinic.model.VisitPreInstructionProductStatus.PENDING);
                    pi.getProducts().add(pr);
                }
            }

            items.add(pi);
        }

        visitPreInstructionRepository.saveAll(items);
        return ApiResponse.success("Pre-instructions added to visit department.", visitDepartmentToDto(visitDepartment));
    }

    // ─────────────────────────────────────────────────────────────
    //  DTO MAPPING
    // ─────────────────────────────────────────────────────────────

    public VisitDepartmentDto visitDepartmentToDto(VisitDepartment visitDepartment) {
        return visitDepartmentToDto(
                visitDepartment,
                resolveVisitInsuranceProviderIds(visitDepartment.getVisit().getId()),
                new LinkedHashSet<>(),
                null
        );
    }

    public VisitDepartmentDto visitDepartmentToDto(VisitDepartment visitDepartment, Set<UUID> visitInsuranceProviderIds) {
        return visitDepartmentToDto(visitDepartment, visitInsuranceProviderIds, new LinkedHashSet<>(), null);
    }

    public VisitDepartmentDto visitDepartmentToDto(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            AuthenticatedUser authUser
    ) {
        return visitDepartmentToDto(visitDepartment, visitInsuranceProviderIds, new LinkedHashSet<>(), authUser);
    }

    public VisitDepartmentDto visitDepartmentToDto(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            Set<UUID> visitedDepartmentIds,
            AuthenticatedUser authUser
    ) {
        if (visitDepartment == null) {
            return null;
        }

        if (visitedDepartmentIds.contains(visitDepartment.getId())) {
            return new VisitDepartmentDto(
                    visitDepartment.getId(),
                    departmentMapper.toDto(visitDepartment.getDepartment()),
                    visitDepartment.getStatus(),
                    visitDepartment.getEncounterType(),
                    departmentProfileToDto(visitDepartment.getProfile()),
                    visitDepartment.getCompletedAt(),
                    visitDepartment.getProcessors() == null || visitDepartment.getProcessors().isEmpty() ? null : visitDepartment.getProcessors().stream().map(workerMapper::toDto).toList(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    visitDepartmentNoteService.buildNotesSummary(visitDepartment.getId(), authUser),
                    visitDepartment.getAnswerId(),
                    visitDepartment.getCreatedAt(),
                    visitDepartment.getUpdatedAt()
            );
        }

        visitedDepartmentIds.add(visitDepartment.getId());

        List<WorkerDto> processors = visitDepartment.getProcessors() == null || visitDepartment.getProcessors().isEmpty()
                ? null
                : visitDepartment.getProcessors().stream().map(workerMapper::toDto).toList();

        List<VisitDepartmentProductDto> products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(item -> visitDepartmentProductToDto(item, visitInsuranceProviderIds))
                .toList();

        List<VisitDepartmentDiagnosisDto> diagnostics = visitDepartmentDiagnosisRepository.findByVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(this::visitDepartmentDiagnosisToDto)
                .toList();

        List<VisitDepartmentMedicationDto> medications = visitDepartmentMedicationRepository.findByVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(this::visitDepartmentMedicationToDto)
                .toList();

        List<VisitPreInstructionDto> preInstructions = visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartment.getId())
                .stream()
                .map(this::visitPreInstructionToDto)
                .toList();

        List<VisitDepartmentDto> childVisitDepartments = visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(child -> visitDepartmentToDto(child, visitInsuranceProviderIds, new LinkedHashSet<>(visitedDepartmentIds), authUser))
                .toList();

        return new VisitDepartmentDto(
                visitDepartment.getId(),
                departmentMapper.toDto(visitDepartment.getDepartment()),
                visitDepartment.getStatus(),
                visitDepartment.getEncounterType(),
                departmentProfileToDto(visitDepartment.getProfile()),
                visitDepartment.getCompletedAt(),
                processors,
                products,
                diagnostics,
                medications,
                preInstructions,
                childVisitDepartments,
                visitDepartmentNoteService.buildNotesSummary(visitDepartment.getId(), authUser),
                visitDepartment.getAnswerId(),
                visitDepartment.getCreatedAt(),
                visitDepartment.getUpdatedAt()
        );
    }

    private DepartmentProfileDto departmentProfileToDto(DepartmentProfile profile) {
        if (profile == null) {
            return null;
        }
        return new DepartmentProfileDto(
                profile.getId(),
                profile.getName(),
                profile.isDefault(),
                departmentProfileProductRepository.findByProfileId(profile.getId())
                        .stream()
                        .map(link -> productMapper.toDto(link.getProduct()))
                        .toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    private VisitDepartmentProductDto visitDepartmentProductToDto(
            VisitDepartmentProduct item,
            Set<UUID> visitInsuranceProviderIds
    ) {
        return new VisitDepartmentProductDto(
                item.getId(),
                productMapper.toDto(item.getProduct()),
                item.getQuantity(),
                item.getStatus(),
                item.getSource(),
                workerMapper.toDto(item.getAddedBy()),
                workerMapper.toDto(item.getBilledBy()),
                workerMapper.toDto(item.getProcessor()),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private VisitDepartmentDiagnosisDto visitDepartmentDiagnosisToDto(VisitDepartmentDiagnosis item) {
        return new VisitDepartmentDiagnosisDto(
                item.getId(),
                item.getDiagnosisName(),
                item.getIcd11Code(),
                item.getCreatedAt()
        );
    }

    private VisitDepartmentMedicationDto visitDepartmentMedicationToDto(VisitDepartmentMedication item) {
        return new VisitDepartmentMedicationDto(
                item.getId(),
                item.getMedicationName(),
                item.getInstructions(),
                item.getCreatedAt()
        );
    }

    private VisitPreInstructionDto visitPreInstructionToDto(VisitPreInstruction pre) {
        List<VisitPreInstructionMedicationRequestDto> medications = pre.getMedications().stream()
                .map(m -> new VisitPreInstructionMedicationRequestDto(
                        m.getId(),
                        m.getMedName(),
                        m.getDosage(),
                        m.getRoute(),
                        m.getFrequency(),
                        m.getDuration(),
                        m.getQuantity(),
                        m.getOtherInstructions(),
                        m.getCreatedAt()
                )).toList();

        List<VisitPreInstructionProductRequestDto> products = pre.getProducts().stream()
                .map(item -> new VisitPreInstructionProductRequestDto(
                        item.getId(),
                        productMapper.toDto(item.getProduct()),
                        item.getQuantity(),
                        workerMapper.toDto(item.getRequestedBy()),
                        item.getStatus(),
                        workerMapper.toDto(item.getProcessedBy()),
                        item.getCreatedAt(),
                        item.getUpdatedAt()
                )).toList();

        return new VisitPreInstructionDto(
                pre.getId(),
                pre.getType(),
                pre.getNote(),
                workerMapper.toDto(pre.getAddedBy()),
                pre.getCreatedAt(),
                medications,
                products
        );
    }

    // ─────────────────────────────────────────────────────────────
    //  PROCESSOR ASSIGNMENT HELPERS
    // ─────────────────────────────────────────────────────────────

    public ApiResponse assignVisitDepartmentProductProcessor(
            VisitDepartment visitDepartment,
            VisitDepartmentProduct item,
            Worker actingUser,
            UUID requestedProcessorId
    ) {
        List<Worker> processors = resolveAvailableProcessorsForProductAssignment(visitDepartment);
        boolean supportRequests = visitDepartment.getDepartment() != null && visitDepartment.getDepartment().isSupportRequests();

        if (requestedProcessorId != null) {
            Worker requestedProcessor = findProcessorById(processors, requestedProcessorId);
            if (requestedProcessor == null) {
                return ApiResponse.error("processorId must belong to the visit department processors.");
            }
            item.setProcessor(requestedProcessor);
            return null;
        }

        if (visitDepartment.getParentVisitDepartment() != null) {
            if (actingUser != null && isProcessor(processors, actingUser.getId())) {
                item.setProcessor(actingUser);
                return null;
            }
            item.setProcessor(null);
            return null;
        }

        if (actingUser != null && isProcessor(processors, actingUser.getId())) {
            item.setProcessor(actingUser);
            return null;
        }

        if (processors.size() == 1) {
            item.setProcessor(processors.get(0));
            return null;
        }

        if (processors.isEmpty()) {
            if (supportRequests) {
                return ApiResponse.error("processorId is required when supportRequests is enabled.");
            }

            item.setProcessor(null);
            return null;
        }

        return ApiResponse.error("processorId is required when the visit department has multiple processors.");
    }

    private List<Worker> resolveAvailableProcessorsForProductAssignment(VisitDepartment visitDepartment) {
        if (visitDepartment == null) {
            return List.of();
        }

        List<Worker> parentProcessors = visitDepartment.getParentVisitDepartment() == null
                ? List.of()
                : normalizeVisitDepartmentProcessors(visitDepartment.getParentVisitDepartment());
        if (!parentProcessors.isEmpty()) {
            return parentProcessors;
        }

        return normalizeVisitDepartmentProcessors(visitDepartment);
    }

    private List<Worker> normalizeVisitDepartmentProcessors(VisitDepartment visitDepartment) {
        if (visitDepartment == null || visitDepartment.getProcessors() == null || visitDepartment.getProcessors().isEmpty()) {
            return List.of();
        }
        return visitDepartment.getProcessors().stream().filter(worker -> worker != null && worker.getId() != null).toList();
    }

    private Worker findProcessorById(List<Worker> processors, UUID workerId) {
        if (processors == null || processors.isEmpty() || workerId == null) {
            return null;
        }
        for (Worker worker : processors) {
            if (workerId.equals(worker.getId())) {
                return worker;
            }
        }
        return null;
    }

    private boolean isProcessor(List<Worker> processors, UUID workerId) {
        return findProcessorById(processors, workerId) != null;
    }

    public void addProcessorToVisitDepartment(VisitDepartment department, Worker worker) {
        if (department == null || worker == null || worker.getId() == null) {
            return;
        }

        if (department.getProcessors() == null) {
            department.setProcessors(new ArrayList<>());
        }

        boolean alreadyAssigned = department.getProcessors().stream()
                .anyMatch(existing -> existing != null && worker.getId().equals(existing.getId()));
        if (!alreadyAssigned) {
            department.getProcessors().add(worker);
        }
    }

    public VisitDepartment deleteChildVisitDepartmentIfEmpty(VisitDepartment visitDepartment) {
        if (visitDepartment == null) {
            return null;
        }

        if (visitDepartment.getParentVisitDepartment() == null) {
            return visitDepartment;
        }

        List<VisitDepartmentProduct> remainingProducts = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        if (!remainingProducts.isEmpty()) {
            return visitDepartment;
        }

        // FK guard: a child with clinical/financial dependents cannot be hard-deleted —
        // deleting it would throw DataIntegrityViolationException -> 500. Keep it and
        // let the caller decide (it will surface in the DTO as a still-present child).
        if (!visitDepartmentDiagnosisRepository.findByVisitDepartmentId(visitDepartment.getId()).isEmpty()
                || !visitDepartmentMedicationRepository.findByVisitDepartmentId(visitDepartment.getId()).isEmpty()
                || !visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartment.getId()).isEmpty()
                || !visitDepartmentNoteServiceHasNotes(visitDepartment.getId())
                || visitDepartmentBillingRepository.existsByVisitDepartmentId(visitDepartment.getId())) {
            return visitDepartment;
        }

        VisitDepartment parent = visitDepartment.getParentVisitDepartment();
        visitDepartmentRepository.delete(visitDepartment);
        return parent;
    }

    private boolean visitDepartmentNoteServiceHasNotes(UUID visitDepartmentId) {
        try {
            return visitDepartmentNoteService.hasNotes(visitDepartmentId);
        } catch (Exception e) {
            return true; // if we cannot verify, do not hard-delete
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  SHARED UTILITIES
    // ─────────────────────────────────────────────────────────────

    public Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    private void reopenVisitIfCompleted(Visit visit) {
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            visit.setStatus(VisitStatus.IN_PROGRESS);
            visitRepository.save(visit);
        }
    }

    private Set<UUID> resolveVisitInsuranceProviderIds(UUID visitId) {
        // S8 fix: previously always returned an empty set. Resolve the insurance
        // provider ids linked to this visit so product DTOs can show the coverage
        // indicator without the caller having to pass them explicitly.
        if (visitId == null) {
            return Set.of();
        }
        return visitInsuranceRepository.findByVisitId(visitId).stream()
                .map(VisitInsurance::getPatientInsurance)
                .filter(pi -> pi != null && pi.getInsuranceProvider() != null && pi.getInsuranceProvider().getId() != null)
                .map(pi -> pi.getInsuranceProvider().getId())
                .collect(Collectors.toSet());
    }

    public BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

}
