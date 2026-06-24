package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
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
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentMedicationRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitDepartmentService {

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    private final VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;
    private final com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;

    private final WorkerMapper workerMapper;
    private final DepartmentMapper departmentMapper;
    private final ProductMapper productMapper;

    // Lazy to break circular dependency with VisitService
    private final VisitDepartmentNoteService visitDepartmentNoteService;

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
            WorkerMapper workerMapper,
            DepartmentMapper departmentMapper,
            ProductMapper productMapper,
            @Lazy VisitDepartmentNoteService visitDepartmentNoteService
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
        this.workerMapper = workerMapper;
        this.departmentMapper = departmentMapper;
        this.productMapper = productMapper;
        this.visitDepartmentNoteService = visitDepartmentNoteService;
    }

    // ─────────────────────────────────────────────────────────────
    //  VISIT DEPARTMENT MANAGEMENT
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDto> addVisitDepartment(UUID visitId, UUID departmentId, com.nexxserve.nexxclinic.model.EncounterType encounterType, AuthenticatedUser authUser) {
        if (visitId == null || departmentId == null) {
            return ApiResponse.error("visitId and departmentId are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
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

        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(departmentOptional.get());
        if (encounterType != null) {
            visitDepartment.setEncounterType(encounterType);
        }
        visitDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartmentRepository.save(visitDepartment);

        // Return refreshed visit DTO via visitRepository – caller can map if needed
        Visit refreshed = visitRepository.findById(visitId).orElse(visit);
        return ApiResponse.success("Department added to visit.", null); // DTO built by caller
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addChildVisitDepartment(AddChildVisitDepartmentInput input, AuthenticatedUser authUser) {
        if (input == null || input.parentVisitDepartmentId() == null || input.departmentId() == null ||
                input.products() == null || input.products().isEmpty()) {
            return ApiResponse.error("parentVisitDepartmentId, departmentId and products are required.");
        }

        Optional<VisitDepartment> parentOptional = visitDepartmentRepository.findById(input.parentVisitDepartmentId());
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

        if (visitDepartmentRepository.existsByVisitIdAndDepartmentId(visit.getId(), input.departmentId())
                || visitDepartmentRepository.existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(visit.getId(), input.departmentId(), input.parentVisitDepartmentId())) {
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

        VisitDepartment savedChild = visitDepartmentRepository.save(child);

        for (var productInput : input.products()) {
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.");
            }

            VisitDepartmentProduct item = new VisitDepartmentProduct();
            item.setVisitDepartment(savedChild);
            item.setProduct(productOptional.get());
            item.setQuantity(normalizeQuantity(BigDecimal.valueOf(productInput.quantity())));
            item.setPrice(resolveUnitPriceSnapshot(productOptional.get(), null));
            item.setStatus(VisitProductStatus.PENDING);

            ApiResponse processorError = assignVisitDepartmentProductProcessor(savedChild, item, actingUser, input.processorId());
            if (processorError != null) {
                return processorError;
            }

            item.setAddedBy(actingUser);
            visitDepartmentProductRepository.save(item);
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

        VisitDepartment visitDepartment = visitDepartmentOptional.get();
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

        VisitDepartmentProduct item = new VisitDepartmentProduct();
        item.setVisitDepartment(visitDepartment);
        item.setProduct(productOptional.get());
        item.setQuantity(normalizeQuantity(input.quantity()));
        item.setPrice(resolveUnitPriceSnapshot(productOptional.get(), input.price()));
        item.setStatus(input.status() == null ? VisitProductStatus.PENDING : input.status());

        Worker actingUser = resolveWorker(authUser);
        ApiResponse processorError = assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, input.processorId());
        if (processorError != null) {
            return processorError;
        }
        item.setAddedBy(actingUser);
        if (item.getStatus() != VisitProductStatus.PENDING) {
            item.setBilledBy(actingUser);
        }

        visitDepartmentProductRepository.save(item);
        reopenVisitIfCompleted(visit);
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
        VisitProductStatus previous = item.getStatus();
        item.setStatus(input.status());

        Worker actingUser = resolveWorker(authUser);
        if (previous == VisitProductStatus.PENDING && input.status() != VisitProductStatus.PENDING) {
            item.setBilledBy(actingUser);
        }

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

        if (input.quantity().compareTo(BigDecimal.ZERO) <= 0) {
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
        department.setStatus(input.status());
        if (input.status() == VisitDepartmentStatus.COMPLETED) {
            department.setCompletedAt(java.time.LocalDateTime.now());
        }

        if (input.status() == VisitDepartmentStatus.ACTIVE) {
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

    private VisitDepartmentProductDto visitDepartmentProductToDto(
            VisitDepartmentProduct item,
            Set<UUID> visitInsuranceProviderIds
    ) {
        return new VisitDepartmentProductDto(
                item.getId(),
                productMapper.toDto(item.getProduct()),
                item.getQuantity(),
                item.getPrice(),
                item.getStatus(),
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

        VisitDepartment parent = visitDepartment.getParentVisitDepartment();
        visitDepartmentRepository.delete(visitDepartment);
        return parent;
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
        // Delegate to shared utility via repository — pulled inline to avoid dep on VisitService
        return visitDepartmentRepository.findByVisitId(visitId).stream()
                .findFirst()
                .map(vd -> vd.getVisit().getId())
                .map(id -> Set.<UUID>of())
                .orElse(Set.of());
        // NOTE: Caller should supply visitInsuranceProviderIds where precision is needed.
    }

    public BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

    private BigDecimal normalizePrice(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        return value;
    }

    public BigDecimal resolveUnitPriceSnapshot(Product product, BigDecimal inputPrice) {
        if (inputPrice != null && inputPrice.compareTo(BigDecimal.ZERO) >= 0) {
            return normalizePrice(inputPrice);
        }

        if (product != null && product.getClinicPrice() != null && product.getClinicPrice().compareTo(BigDecimal.ZERO) >= 0) {
            return normalizePrice(product.getClinicPrice());
        }

        return BigDecimal.ZERO;
    }
}