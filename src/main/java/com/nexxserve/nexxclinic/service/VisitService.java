package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitVitalSignsGroup;
import com.nexxserve.nexxclinic.entity.VitalMeasurement;
import com.nexxserve.nexxclinic.entity.VisitPreInstruction;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionMedication;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductStatusInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignItemInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionsInput;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentInput;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionProductRequest;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentMedicationRepository;
import com.nexxserve.nexxclinic.repository.VisitVitalSignsGroupRepository;
import com.nexxserve.nexxclinic.repository.VitalMeasurementRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private final VisitRepository visitRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    private final VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;
    private final VisitVitalSignsGroupRepository visitVitalSignsGroupRepository;
    private final VitalMeasurementRepository vitalMeasurementRepository;
    private final com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final DepartmentFormService departmentFormService;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final ProductInsuranceCoverageRepository productInsuranceCoverageRepository;
    private final WorkerRepository workerRepository;

    public VisitService(
            VisitRepository visitRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository,
            VisitDepartmentMedicationRepository visitDepartmentMedicationRepository,
            VisitVitalSignsGroupRepository visitVitalSignsGroupRepository,
            VitalMeasurementRepository vitalMeasurementRepository,
            VisitBillingRepository visitBillingRepository,
            com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository,
            DepartmentFormService departmentFormService,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            DepartmentRepository departmentRepository,
            ProductRepository productRepository,
            ProductInsuranceCoverageRepository productInsuranceCoverageRepository,
            WorkerRepository workerRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitDepartmentDiagnosisRepository = visitDepartmentDiagnosisRepository;
        this.visitDepartmentMedicationRepository = visitDepartmentMedicationRepository;
        this.visitVitalSignsGroupRepository = visitVitalSignsGroupRepository;
        this.vitalMeasurementRepository = vitalMeasurementRepository;
        this.visitPreInstructionRepository = visitPreInstructionRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.departmentFormService = departmentFormService;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.departmentRepository = departmentRepository;
        this.productRepository = productRepository;
        this.productInsuranceCoverageRepository = productInsuranceCoverageRepository;
        this.workerRepository = workerRepository;
    }


    @Transactional
    public ApiResponse createVisit(CreateVisitInput input, AuthenticatedUser authUser) {
        if (input == null || input.patientId() == null) {
            return ApiResponse.error("patientId is required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        List<PatientInsurance> linkedInsurances = resolveLinkedInsurances(input.patientId(), input.linkedPatientInsuranceIds());
        if (linkedInsurances == null) {
            return ApiResponse.error("Each linked insurance must exist and belong to the selected patient.", "INVALID_VISIT_INSURANCE_LINKS");
        }

        Worker actingUser = resolveWorker(authUser);

        Visit visit = new Visit();
        visit.setPatient(patientOptional.get());
        visit.setVisitDate(input.visitDate() == null ? LocalDateTime.now() : input.visitDate());
        visit.setStatus(VisitStatus.CREATED);

        Visit saved = visitRepository.save(visit);

        if (!linkedInsurances.isEmpty()) {
            List<VisitInsurance> links = new ArrayList<>();
            for (PatientInsurance patientInsurance : linkedInsurances) {
                VisitInsurance link = new VisitInsurance();
                link.setVisit(saved);
                link.setPatientInsurance(patientInsurance);
                links.add(link);
            }
            visitInsuranceRepository.saveAll(links);
        }

        ApiResponse departmentError = addDepartmentsToVisit(saved, input.departments(), actingUser);
        if (departmentError != null) {
            return departmentError;
        }

        Visit latest = visitRepository.findById(saved.getId()).orElse(saved);
        return ApiResponse.success("Visit created.", visitToMap(latest));
    }

    @Transactional
    public ApiResponse changeVisitDate(ChangeVisitDateInput input) {
        if (input == null || input.visitId() == null || input.visitDate() == null) {
            return ApiResponse.error("visitId and visitDate are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        int updatedRows = visitRepository.updateVisitDate(input.visitId(), input.visitDate());
        if (updatedRows == 0) {
            return ApiResponse.error("Visit date could not be updated.", "UPDATE_FAILED");
        }

        visitBillingRepository.updateBillingDateByVisitId(input.visitId(), input.visitDate());

        Visit updatedVisit = visitRepository.findById(input.visitId()).orElse(visitOptional.get());
        return ApiResponse.success("Visit date changed.", visitToMap(updatedVisit));
    }

    @Transactional(readOnly = true)
    public ApiResponse visit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        return visitOptional.map(visit -> ApiResponse.success("Visit fetched.", visitToMap(visit))).orElseGet(() -> ApiResponse.error("Visit not found.", "NOT_FOUND"));

    }

    @Transactional(readOnly = true)
    public ApiResponse visits(SearchVisitsInput input) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Specification<Visit> spec = (root, queryDef, builder) -> builder.conjunction();

        if (input != null && input.visitDate() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("visitDate"), input.visitDate()));
        }

        if (input != null && input.status() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("status"), input.status()));
        }

        String patientName = input == null ? null : blankToNull(input.patientName());
        if (patientName != null) {
            String normalized = patientName.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.like(builder.lower(root.join("patient").get("fullName")), "%" + normalized + "%")
            );
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<Map<String, Object>> visits = visitPage.getContent().stream().map(this::visitToMap).toList();

        return ApiResponse.success("Visits fetched.", visits, paginationToMap(visitPage));
    }

    @Transactional(readOnly = true)
    public ApiResponse getPatientHistory(UUID patientId, SearchPatientHistoryInput input) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.", "VALIDATION_ERROR");
        }

        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.", "NOT_FOUND");
        }

        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        DateWindow dateWindow;
        try {
            dateWindow = resolvePatientHistoryDateWindow(input);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(ex.getMessage(), "VALIDATION_ERROR");
        }

        Set<UUID> departmentIds = normalizeDepartmentIds(input == null ? null : input.departmentIds());

        Specification<Visit> spec = (root, queryDef, builder) -> builder.equal(root.join("patient").get("id"), patientId);

        if (dateWindow.start() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.greaterThanOrEqualTo(root.get("visitDate"), dateWindow.start()));
        }

        if (dateWindow.endExclusive() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.lessThan(root.get("visitDate"), dateWindow.endExclusive()));
        }

        if (!departmentIds.isEmpty()) {
            Set<UUID> matchingVisitIds = new LinkedHashSet<>();
            for (VisitDepartment visitDepartment : visitDepartmentRepository.findByDepartmentIdIn(departmentIds)) {
                matchingVisitIds.add(visitDepartment.getVisit().getId());
            }

            if (matchingVisitIds.isEmpty()) {
                Page<Visit> emptyPage = Page.empty(pageable);
                return ApiResponse.success("Patient history fetched.", List.of(), paginationToMap(emptyPage));
            }

            spec = spec.and((root, queryDef, builder) -> root.get("id").in(matchingVisitIds));
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<Map<String, Object>> visits = visitPage.getContent().stream()
                .map(visit -> visitToMap(visit, departmentIds))
                .toList();

        return ApiResponse.success("Patient history fetched.", visits, paginationToMap(visitPage));
    }

    @Transactional
    public ApiResponse completeVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visit cannot be completed.", "INVALID_VISIT_STATUS_TRANSITION");
        }

        // Check if all products on the visit are billed (not PENDING)
        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        boolean hasUnbilledProducts = visitProducts.stream()
                .anyMatch(product -> product.getStatus() == VisitProductStatus.PENDING);
        
        if (hasUnbilledProducts) {
            return ApiResponse.error("Cannot complete visit with unbilled products. All products must be billed first.", "VISIT_HAS_UNBILLED_PRODUCTS");
        }

        // Mark all departments as COMPLETED
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            if (dept.getStatus() != VisitDepartmentStatus.CANCELLED) {
                dept.setStatus(VisitDepartmentStatus.COMPLETED);
                visitDepartmentRepository.save(dept);
            }
        }

        visit.setStatus(VisitStatus.COMPLETED);
        Visit saved = visitRepository.save(visit);
        return ApiResponse.success("Visit completed.", visitToMap(saved));
    }

    @Transactional
    public ApiResponse completeVisit(ConsultationAnswersInput input, boolean finalAnswer, AuthenticatedUser authUser) {
        if (input == null) {
            return ApiResponse.error("input is required.", "VALIDATION_ERROR");
        }

        if (input.visitId() == null || input.departmentId() == null) {
            return ApiResponse.error("visitId and departmentId are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visit cannot be completed.", "INVALID_VISIT_STATUS_TRANSITION");
        }

        ConsultationAnswersInput effectiveInput = input;
        if (finalAnswer) {
            effectiveInput = new ConsultationAnswersInput(
                    input.consultationId(),
                    input.visitId(),
                    input.patientId(),
                    input.departmentId(),
                    input.formId(),
                    input.formVersion(),
                    AnswerStatus.FINAL,
                    input.answers()
            );
        }

        ApiResponse answerResponse = departmentFormService.upsertConsultationAnswers(effectiveInput, authUser);
        if (answerResponse.status() != ResponseStatus.SUCCESS) {
            return answerResponse;
        }

        // Always set department status to BILLING
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(input.visitId());
        for (VisitDepartment dept : departments) {
            if (dept.getStatus() != VisitDepartmentStatus.CANCELLED) {
                dept.setStatus(VisitDepartmentStatus.BILLING);
                visitDepartmentRepository.save(dept);
            }
        }

        Visit saved = visitRepository.save(visit);
        return ApiResponse.success("Visit submitted for billing.", visitToMap(saved));
    }

    @Transactional
    public ApiResponse saveAnswerAndCompleteVisit(ConsultationAnswersInput input, boolean finalAnswer, AuthenticatedUser authUser) {
        return completeVisit(input, finalAnswer, authUser);
    }

    @Transactional
    public ApiResponse cancelVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Completed visit cannot be cancelled.", "INVALID_VISIT_STATUS_TRANSITION");
        }

        // Check if there are any products on the visit
        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        if (!visitProducts.isEmpty()) {
            return ApiResponse.error("Cannot cancel visit with existing products. Remove all products first.", "VISIT_HAS_PRODUCTS");
        }

        // Mark all departments as CANCELLED
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            dept.setStatus(VisitDepartmentStatus.CANCELLED);
            visitDepartmentRepository.save(dept);
        }

        visit.setStatus(VisitStatus.CANCELLED);
        Visit saved = visitRepository.save(visit);
        return ApiResponse.success("Visit cancelled.", visitToMap(saved));
    }

    @Transactional
    public ApiResponse addVisitDepartment(UUID visitId, UUID departmentId, AuthenticatedUser authUser) {
        if (visitId == null || departmentId == null) {
            return ApiResponse.error("visitId and departmentId are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add departments to a completed visit.", "VISIT_IS_COMPLETED");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add departments to a cancelled visit.", "VISIT_IS_CANCELLED");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        if (visitDepartmentRepository.existsByVisitIdAndDepartmentId(visitId, departmentId)) {
            return ApiResponse.error("Department is already added to this visit.", "DUPLICATE_VISIT_DEPARTMENT");
        }

        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(departmentOptional.get());
        visitDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartmentRepository.save(visitDepartment);

        return ApiResponse.success("Department added to visit.", visitToMap(visit));
    }

    @Transactional
    public ApiResponse addChildVisitDepartment(AddChildVisitDepartmentInput input, AuthenticatedUser authUser) {
        if (input == null || input.parentVisitDepartmentId() == null || input.departmentId() == null || 
            input.products() == null || input.products().isEmpty()) {
            return ApiResponse.error("parentVisitDepartmentId, departmentId and products are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> parentOptional = visitDepartmentRepository.findById(input.parentVisitDepartmentId());
        if (parentOptional.isEmpty()) {
            return ApiResponse.error("Parent visit department not found.", "NOT_FOUND");
        }

        VisitDepartment parent = parentOptional.get();
        if (parent.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add child departments to a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (parent.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add child departments to a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        if (parent.getDepartment() != null && parent.getDepartment().getId() != null && parent.getDepartment().getId().equals(input.departmentId())) {
            return ApiResponse.error("A department cannot be added as a child of itself.", "INVALID_CHILD_DEPARTMENT");
        }

        Optional<Department> departmentOptional = departmentRepository.findById(input.departmentId());
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.", "NOT_FOUND");
        }

        Department childDepartment = departmentOptional.get();
        if (!childDepartment.isSupportRequests()) {
            return ApiResponse.error("Only support request departments can be added as children.", "INVALID_CHILD_DEPARTMENT");
        }

        Visit visit = parent.getVisit();
        if (visit == null) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        if (visitDepartmentRepository.existsByVisitIdAndDepartmentId(visit.getId(), input.departmentId())
            || visitDepartmentRepository.existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(visit.getId(), input.departmentId(), input.parentVisitDepartmentId())) {
            return ApiResponse.error("Child department already exists for this parent.", "DUPLICATE_VISIT_DEPARTMENT");
        }

        // Validate all products before creating the child department
        Worker actingUser = resolveWorker(authUser);
        Set<UUID> seenProducts = new LinkedHashSet<>();
        
        for (var productInput : input.products()) {
            if (productInput == null || productInput.productId() == null) {
                return ApiResponse.error("productId is required for each product.", "VALIDATION_ERROR");
            }
            
            if (productInput.quantity() == null) {
                return ApiResponse.error("quantity is required for each product.", "VALIDATION_ERROR");
            }
            
            if (!seenProducts.add(productInput.productId())) {
                return ApiResponse.error("Duplicate productId found in products.", "DUPLICATE_VISIT_DEPARTMENT_PRODUCT");
            }
            
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.", "NOT_FOUND");
            }
        }

        // Validate provided processorId if present; child departments may be created without processor assigned.
        if (input.processorId() != null) {
            VisitDepartment tempChild = new VisitDepartment();
            tempChild.setVisit(visit);
            tempChild.setDepartment(childDepartment);
            tempChild.setParentVisitDepartment(parent);
            tempChild.setStatus(VisitDepartmentStatus.PENDING);

            List<Worker> processors = resolveAvailableProcessorsForProductAssignment(tempChild);
            Worker requestedProcessor = findProcessorById(processors, input.processorId());
            if (requestedProcessor == null) {
                return ApiResponse.error("processorId must belong to the visit department processors.", "INVALID_PROCESSOR");
            }
        }

        // All validations passed, now create the child department
        VisitDepartment child = new VisitDepartment();
        child.setVisit(visit);
        child.setDepartment(childDepartment);
        child.setParentVisitDepartment(parent);
        child.setStatus(VisitDepartmentStatus.PENDING);

        VisitDepartment savedChild = visitDepartmentRepository.save(child);
        
        // Add all products to the child department
        for (var productInput : input.products()) {
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                // This shouldn't happen as we already validated above, but safe to check
                return ApiResponse.error("Product not found.", "NOT_FOUND");
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

        return ApiResponse.success("Child visit department added.", visitDepartmentToMap(parent));
    }

    @Transactional
    public ApiResponse removeChildVisitDepartment(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment child = departmentOptional.get();
        if (child.getParentVisitDepartment() == null) {
            return ApiResponse.error("Only child visit departments can be removed with this mutation.", "INVALID_CHILD_DEPARTMENT");
        }

        VisitDepartment parent = child.getParentVisitDepartment();
        visitDepartmentRepository.delete(child);
        return ApiResponse.success("Child visit department removed.", visitDepartmentToMap(parent));
    }

    @Transactional
    public ApiResponse linkVisitInsurances(UUID visitId, List<UUID> insuranceIds, AuthenticatedUser authUser) {
        if (visitId == null || insuranceIds == null) {
            return ApiResponse.error("visitId and insuranceIds are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add insurances to a completed visit.", "VISIT_IS_COMPLETED");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add insurances to a cancelled visit.", "VISIT_IS_CANCELLED");
        }

        List<UUID> uniqueIds = normalizeUniqueIds(insuranceIds);
        if (uniqueIds.isEmpty()) {
            return ApiResponse.error("insuranceIds must not be empty.", "VALIDATION_ERROR");
        }

        List<PatientInsurance> patientInsurances = resolveLinkedInsurances(visit.getPatient().getId(), uniqueIds);
        if (patientInsurances == null) {
            return ApiResponse.error("Each insurance must exist and belong to the selected patient.", "INVALID_VISIT_INSURANCE_LINKS");
        }

        for (PatientInsurance patientInsurance : patientInsurances) {
            if (visitInsuranceRepository.existsByVisitIdAndPatientInsuranceId(visitId, patientInsurance.getId())) {
                return ApiResponse.error("One or more insurances are already linked to this visit.", "DUPLICATE_VISIT_INSURANCE");
            }
        }

        List<VisitInsurance> links = new ArrayList<>();
        for (PatientInsurance patientInsurance : patientInsurances) {
            VisitInsurance link = new VisitInsurance();
            link.setVisit(visit);
            link.setPatientInsurance(patientInsurance);
            links.add(link);
        }

        visitInsuranceRepository.saveAll(links);
        Visit refreshedVisit = visitRepository.findById(visitId).orElse(visit);
        return ApiResponse.success("Insurance linked to visit.", visitToMap(refreshedVisit));
    }

    @Transactional
    public ApiResponse unlinkVisitInsurances(UUID visitId, List<UUID> insuranceIds, AuthenticatedUser authUser) {
        if (visitId == null || insuranceIds == null) {
            return ApiResponse.error("visitId and insuranceIds are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot remove insurances from a completed visit.", "VISIT_IS_COMPLETED");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot remove insurances from a cancelled visit.", "VISIT_IS_CANCELLED");
        }

        List<UUID> uniqueIds = normalizeUniqueIds(insuranceIds);
        if (uniqueIds.isEmpty()) {
            return ApiResponse.error("insuranceIds must not be empty.", "VALIDATION_ERROR");
        }

        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visitId);
        List<VisitInsurance> linksToRemove = new ArrayList<>();
        for (VisitInsurance visitInsurance : visitInsurances) {
            if (visitInsurance.getPatientInsurance() != null && uniqueIds.contains(visitInsurance.getPatientInsurance().getId())) {
                linksToRemove.add(visitInsurance);
            }
        }

        if (linksToRemove.size() != uniqueIds.size()) {
            return ApiResponse.error("One or more insurances are not linked to this visit.", "VISIT_INSURANCE_NOT_FOUND");
        }

        visitInsuranceRepository.deleteAll(linksToRemove);
        Visit refreshedVisit = visitRepository.findById(visitId).orElse(visit);
        return ApiResponse.success("Insurance unlinked from visit.", visitToMap(refreshedVisit));
    }

    @Transactional
    public ApiResponse addVisitDepartmentProduct(CreateVisitDepartmentProductInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null || input.departmentId() == null || input.productId() == null) {
            return ApiResponse.error("visitId, departmentId and productId are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add products to a completed visit.", "VISIT_IS_COMPLETED");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled visit.", "VISIT_IS_CANCELLED");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findByVisitIdAndDepartmentId(input.visitId(), input.departmentId());
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Department is not linked to this visit.", "VISIT_DEPARTMENT_NOT_FOUND");
        }

        VisitDepartment visitDepartment = visitDepartmentOptional.get();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add products to a completed department.", "DEPARTMENT_IS_COMPLETED");
        }

        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        Optional<Product> productOptional = productRepository.findById(input.productId());
        if (productOptional.isEmpty()) {
            return ApiResponse.error("Product not found.", "NOT_FOUND");
        }

        Optional<VisitDepartmentProduct> existing = visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(
                visitDepartment.getId(), input.productId()
        );
        if (existing.isPresent()) {
            return ApiResponse.error("Product already exists for this visit department.", "DUPLICATE_VISIT_DEPARTMENT_PRODUCT");
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
        return ApiResponse.success("Product added to visit department.", visitDepartmentToMap(visitDepartment));
    }

    @Transactional
    public ApiResponse addVisitVitalSigns(AddVisitVitalSignsInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null || input.vitalSigns() == null || input.vitalSigns().isEmpty()) {
            return ApiResponse.error("visitId and vitalSigns are required.", "VALIDATION_ERROR");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.", "NOT_FOUND");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add vital signs to a completed visit.", "VISIT_IS_COMPLETED");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add vital signs to a cancelled visit.", "VISIT_IS_CANCELLED");
        }

        Worker actingUser = resolveWorker(authUser);
        VisitVitalSignsGroup group = new VisitVitalSignsGroup();
        group.setVisit(visit);
        group.setAddedBy(actingUser);
        group = visitVitalSignsGroupRepository.save(group);

        List<VitalMeasurement> measurements = new ArrayList<>();
        for (AddVisitVitalSignItemInput vitalSignInput : input.vitalSigns()) {
            if (vitalSignInput == null
                    || vitalSignInput.measurementName() == null
                    || vitalSignInput.measurementName().isBlank()
                    || vitalSignInput.value() == null
                    || vitalSignInput.value().isBlank()
                    || vitalSignInput.unit() == null
                    || vitalSignInput.unit().isBlank()) {
                return ApiResponse.error("measurementName, value and unit are required for each vital sign.", "VALIDATION_ERROR");
            }

            VitalMeasurement item = new VitalMeasurement();
            item.setGroup(group);
            item.setMeasurementName(vitalSignInput.measurementName().trim());
            item.setValue(vitalSignInput.value().trim());
            item.setUnit(vitalSignInput.unit().trim());
            measurements.add(item);
        }

        vitalMeasurementRepository.saveAll(measurements);
        return ApiResponse.success("Vital signs added to visit.", visitToMap(visit));
    }

    @Transactional
    public ApiResponse addVisitPreInstructions(AddVisitPreInstructionsInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentId() == null || input.items() == null || input.items().isEmpty()) {
            return ApiResponse.error("visitDepartmentId and items are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment visitDepartment = visitDepartmentOptional.get();
        Visit visit = visitDepartment.getVisit();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add pre-instructions to a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add pre-instructions to a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        Worker actingUser = resolveWorker(authUser);
        List<VisitPreInstruction> items = new ArrayList<>();
        for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionItemInput itemInput : input.items()) {
            if (itemInput == null || itemInput.type() == null || itemInput.type().isBlank()) {
                return ApiResponse.error("Each item must have a type (NOTE or MEDICATION).", "VALIDATION_ERROR");
            }
            String type = itemInput.type().trim().toUpperCase();
            if (!type.equals("NOTE") && !type.equals("MEDICATION") && !type.equals("PRODUCT")) {
                return ApiResponse.error("Item type must be NOTE, MEDICATION or PRODUCT.", "VALIDATION_ERROR");
            }

            VisitPreInstruction pi = new VisitPreInstruction();
            pi.setVisit(visit);
            pi.setVisitDepartment(visitDepartment);
            pi.setType(type);
            pi.setNote(itemInput.note());
            pi.setAddedBy(actingUser);

            if (type.equals("MEDICATION")) {
                if (itemInput.medications() == null || itemInput.medications().isEmpty()) {
                    return ApiResponse.error("Medication items must include at least one medication entry.", "VALIDATION_ERROR");
                }
                for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionMedicationInput medIn : itemInput.medications()) {
                    if (medIn == null || medIn.medName() == null || medIn.medName().isBlank()) {
                        return ApiResponse.error("medName is required for medication entries.", "VALIDATION_ERROR");
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
                    return ApiResponse.error("Product items must include at least one product entry.", "VALIDATION_ERROR");
                }
                for (com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionProductInput prodIn : itemInput.products()) {
                    if (prodIn == null || prodIn.productId() == null) {
                        return ApiResponse.error("productId is required for product entries.", "VALIDATION_ERROR");
                    }
                    Optional<com.nexxserve.nexxclinic.entity.Product> productOptional = productRepository.findById(prodIn.productId());
                    if (productOptional.isEmpty()) {
                        return ApiResponse.error("Product not found.", "NOT_FOUND");
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
        return ApiResponse.success("Pre-instructions added to visit department.", visitDepartmentToMap(visitDepartment));
    }

    @Transactional
    public ApiResponse updateVisitDepartmentProductStatus(UpdateVisitDepartmentProductStatusInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentProductId() == null || input.status() == null) {
            return ApiResponse.error("visitDepartmentProductId and status are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(input.visitDepartmentProductId());
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.", "NOT_FOUND");
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
        return ApiResponse.success("Visit department product status updated.", visitDepartmentToMap(saved.getVisitDepartment()));
    }

    @Transactional
    public ApiResponse updateVisitDepartmentStatus(com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentId() == null || input.status() == null) {
            return ApiResponse.error("visitDepartmentId and status are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
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
        return ApiResponse.success("Visit department status updated.", visitDepartmentToMap(saved));
    }

    @Transactional
    public ApiResponse addVisitDepartmentProcessor(UUID visitDepartmentId, UUID processorId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null || processorId == null) {
            return ApiResponse.error("visitDepartmentId and processorId are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot modify processors on a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot modify processors on a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        Optional<Worker> processorOptional = workerRepository.findById(processorId);
        if (processorOptional.isEmpty()) {
            return ApiResponse.error("Processor not found.", "NOT_FOUND");
        }

        addProcessorToVisitDepartment(department, processorOptional.get());
        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department processor added.", visitDepartmentToMap(saved));
    }

    @Transactional
    public ApiResponse removeVisitDepartmentProcessor(UUID visitDepartmentId, UUID processorId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null || processorId == null) {
            return ApiResponse.error("visitDepartmentId and processorId are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot modify processors on a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot modify processors on a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        if (department.getProcessors() == null || department.getProcessors().isEmpty()) {
            return ApiResponse.error("Visit department has no processors.", "PROCESSOR_NOT_FOUND");
        }

        boolean removed = department.getProcessors().removeIf(worker -> worker != null && processorId.equals(worker.getId()));
        if (!removed) {
            return ApiResponse.error("Processor not found on this visit department.", "PROCESSOR_NOT_FOUND");
        }

        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department processor removed.", visitDepartmentToMap(saved));
    }

    @Transactional
    public ApiResponse updateVisitDepartmentProductQuantity(com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductQuantityInput input) {
        if (input == null || input.visitDepartmentProductId() == null || input.quantity() == null) {
            return ApiResponse.error("visitDepartmentProductId and quantity are required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(input.visitDepartmentProductId());
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.", "NOT_FOUND");
        }

        VisitDepartmentProduct item = itemOptional.get();
        Visit visit = item.getVisitDepartment().getVisit();

        if (input.quantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            // Delete product from visit department when quantity is 0 or less
            VisitDepartment affectedDepartment = item.getVisitDepartment();
            visitDepartmentProductRepository.delete(item);
            reopenVisitIfCompleted(visit);
            VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartment);
            return ApiResponse.success("Visit department product removed.", visitDepartmentToMap(mappedDepartment == null ? affectedDepartment : mappedDepartment));
        }

        item.setQuantity(input.quantity());

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        deleteChildVisitDepartmentIfEmpty(saved.getVisitDepartment());
        return ApiResponse.success("Visit department product quantity updated.", visitDepartmentToMap(saved.getVisitDepartment()));
    }

    @Transactional
    public ApiResponse removeVisitDepartmentProduct(UUID visitDepartmentProductId) {
        if (visitDepartmentProductId == null) {
            return ApiResponse.error("visitDepartmentProductId is required.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(visitDepartmentProductId);
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.", "NOT_FOUND");
        }

        VisitDepartmentProduct item = itemOptional.get();
        Visit visit = item.getVisitDepartment().getVisit();
        VisitDepartment affectedDepartment = item.getVisitDepartment();

        // Delete the product from visit department
        visitDepartmentProductRepository.delete(item);
        reopenVisitIfCompleted(visit);

        VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartment);
        return ApiResponse.success("Visit department product removed.", visitDepartmentToMap(mappedDepartment == null ? affectedDepartment : mappedDepartment));
    }

    private ApiResponse addDepartmentsToVisit(
            Visit visit,
            List<CreateVisitDepartmentInput> departments,
            Worker actingUser
    ) {
        if (departments == null || departments.isEmpty()) {
            return null;
        }

        Set<UUID> seenDepartments = new LinkedHashSet<>();
        for (CreateVisitDepartmentInput departmentInput : departments) {
            if (departmentInput == null || departmentInput.departmentId() == null) {
                return ApiResponse.error("departmentId is required for each visit department.", "VALIDATION_ERROR");
            }

            if (!seenDepartments.add(departmentInput.departmentId())) {
                return ApiResponse.error("Duplicate departmentId found in visit departments.", "DUPLICATE_VISIT_DEPARTMENT");
            }

            Optional<Department> departmentOptional = departmentRepository.findById(departmentInput.departmentId());
            if (departmentOptional.isEmpty()) {
                return ApiResponse.error("Department not found.", "NOT_FOUND");
            }

            VisitDepartment visitDepartment = new VisitDepartment();
            visitDepartment.setVisit(visit);
            visitDepartment.setDepartment(departmentOptional.get());
            visitDepartment.setStatus(VisitDepartmentStatus.PENDING);
            VisitDepartment savedVisitDepartment = visitDepartmentRepository.save(visitDepartment);

            ApiResponse productsError = addProductsToVisitDepartment(savedVisitDepartment, departmentInput.products(), actingUser);
            if (productsError != null) {
                return productsError;
            }
        }

        return null;
    }

    private ApiResponse addProductsToVisitDepartment(
            VisitDepartment visitDepartment,
            List<CreateVisitDepartmentInput.CreateVisitDepartmentProductItemInput> products,
            Worker actingUser
    ) {
        if (products == null || products.isEmpty()) {
            return null;
        }

        Set<UUID> seenProducts = new LinkedHashSet<>();
        for (CreateVisitDepartmentInput.CreateVisitDepartmentProductItemInput productInput : products) {
            if (productInput == null || productInput.productId() == null) {
                return ApiResponse.error("productId is required for each department product.", "VALIDATION_ERROR");
            }

            if (!seenProducts.add(productInput.productId())) {
                return ApiResponse.error("Duplicate productId found in visit department products.", "DUPLICATE_VISIT_DEPARTMENT_PRODUCT");
            }

            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.", "NOT_FOUND");
            }

            VisitDepartmentProduct item = new VisitDepartmentProduct();
            item.setVisitDepartment(visitDepartment);
            item.setProduct(productOptional.get());
            item.setQuantity(normalizeQuantity(productInput.quantity()));
            item.setPrice(resolveUnitPriceSnapshot(productOptional.get(), productInput.price()));
            item.setStatus(productInput.status() == null ? VisitProductStatus.PENDING : productInput.status());
            ApiResponse processorError = assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, productInput.processorId());
            if (processorError != null) {
                return processorError;
            }
            item.setAddedBy(actingUser);
            if (item.getStatus() != VisitProductStatus.PENDING) {
                item.setBilledBy(actingUser);
            }
            visitDepartmentProductRepository.save(item);
        }

        return null;
    }

    private List<PatientInsurance> resolveLinkedInsurances(UUID patientId, List<UUID> linkedInsuranceIds) {
        if (linkedInsuranceIds == null || linkedInsuranceIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>();
        for (UUID id : linkedInsuranceIds) {
            if (id != null) {
                uniqueIds.add(id);
            }
        }

        List<PatientInsurance> linkedInsurances = new ArrayList<>();
        for (UUID insuranceId : uniqueIds) {
            Optional<PatientInsurance> insuranceOptional = patientInsuranceRepository.findById(insuranceId);
            if (insuranceOptional.isEmpty()) {
                return null;
            }

            PatientInsurance insurance = insuranceOptional.get();
            if (!insurance.getPatient().getId().equals(patientId)) {
                return null;
            }

            linkedInsurances.add(insurance);
        }

        return linkedInsurances;
    }

    private List<UUID> normalizeUniqueIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        Set<UUID> uniqueIds = new LinkedHashSet<>();
        for (UUID id : ids) {
            if (id != null) {
                uniqueIds.add(id);
            }
        }

        return new ArrayList<>(uniqueIds);
    }

    private Map<String, Object> visitToMap(Visit visit) {
        return visitToMap(visit, Set.of());
    }

    private Map<String, Object> visitToMap(Visit visit, Set<UUID> departmentIds) {
        Set<UUID> visitInsuranceProviderIds = resolveVisitInsuranceProviderIds(visit.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("id", visit.getId());
        data.put("patient", patientToMap(visit.getPatient()));
        data.put("status", visit.getStatus());
        data.put("visitDate", visit.getVisitDate());
        data.put(
                "linkedInsurances",
                visitInsuranceRepository.findByVisitId(visit.getId())
                        .stream()
                        .map(link -> patientInsuranceToMap(link.getPatientInsurance()))
                        .toList()
        );
        data.put(
                "departments",
            resolveVisitDepartmentsForResponse(visit.getId(), departmentIds)
                .stream()
                .map(visitDepartment -> visitDepartmentToMap(visitDepartment, visitInsuranceProviderIds))
                .toList()
        );
        data.put(
                "vitalSigns",
            visitVitalSignsGroupRepository.findByVisitIdOrderByCreatedAtAsc(visit.getId())
                        .stream()
                .map(this::visitVitalSignsGroupToMap)
                        .toList()
        );
        return data;
    }

    private Map<String, Object> visitPreInstructionToMap(VisitPreInstruction pre) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", pre.getId());
        data.put("type", pre.getType());
        data.put("note", pre.getNote());
        data.put("addedBy", pre.getAddedBy() == null ? null : workerToMap(pre.getAddedBy()));
        data.put("createdAt", pre.getCreatedAt());
        data.put(
                "medications",
                pre.getMedications().stream().map(m -> {
                    Map<String, Object> med = new HashMap<>();
                    med.put("id", m.getId());
                    med.put("medName", m.getMedName());
                    med.put("dosage", m.getDosage());
                    med.put("route", m.getRoute());
                    med.put("frequency", m.getFrequency());
                    med.put("duration", m.getDuration());
                    med.put("quantity", m.getQuantity());
                    med.put("otherInstructions", m.getOtherInstructions());
                    med.put("createdAt", m.getCreatedAt());
                    return med;
                }).toList()
        );
                data.put(
                    "products",
                    pre.getProducts().stream()
                            .map(item -> visitPreInstructionProductToMap(
                                    item,
                                    resolveVisitInsuranceProviderIds(pre.getVisit().getId())
                            ))
                            .toList()
                );
        return data;
    }

    private Map<String, Object> visitDepartmentToMap(VisitDepartment visitDepartment) {
        return visitDepartmentToMap(
                visitDepartment,
                resolveVisitInsuranceProviderIds(visitDepartment.getVisit().getId()),
                new LinkedHashSet<>()
        );
    }

    private Map<String, Object> visitDepartmentToMap(VisitDepartment visitDepartment, Set<UUID> visitInsuranceProviderIds) {
        return visitDepartmentToMap(visitDepartment, visitInsuranceProviderIds, new LinkedHashSet<>());
    }

    private Map<String, Object> visitDepartmentToMap(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            Set<UUID> visitedDepartmentIds
    ) {
        if (visitDepartment == null) {
            return null;
        }

        if (visitedDepartmentIds.contains(visitDepartment.getId())) {
            Map<String, Object> circular = new HashMap<>();
            circular.put("id", visitDepartment.getId());
            circular.put("department", departmentToMap(visitDepartment.getDepartment()));
            circular.put("status", visitDepartment.getStatus());
            circular.put("completedAt", visitDepartment.getCompletedAt());
            circular.put("processors", visitDepartment.getProcessors() == null || visitDepartment.getProcessors().isEmpty() ? null : visitDepartment.getProcessors().stream().map(this::workerToMap).toList());
            circular.put("products", List.of());
            circular.put("diagnostics", List.of());
            circular.put("medications", List.of());
            circular.put("preInstructions", List.of());
            circular.put("childVisitDepartments", List.of());
            circular.put("createdAt", visitDepartment.getCreatedAt());
            circular.put("updatedAt", visitDepartment.getUpdatedAt());
            return circular;
        }

        visitedDepartmentIds.add(visitDepartment.getId());
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitDepartment.getId());
        data.put("department", departmentToMap(visitDepartment.getDepartment()));
        data.put("status", visitDepartment.getStatus());
        data.put("completedAt", visitDepartment.getCompletedAt());
        data.put(
            "processors",
            visitDepartment.getProcessors() == null || visitDepartment.getProcessors().isEmpty()
                ? null
                : visitDepartment.getProcessors().stream().map(this::workerToMap).toList()
        );
        data.put(
                "products",
                visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream()
                        .map(item -> visitDepartmentProductToMap(item, visitInsuranceProviderIds))
                        .toList()
        );
        data.put(
                "diagnostics",
                visitDepartmentDiagnosisRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream()
                        .map(this::visitDepartmentDiagnosisToMap)
                        .toList()
        );
        data.put(
                "medications",
                visitDepartmentMedicationRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream()
                        .map(this::visitDepartmentMedicationToMap)
                        .toList()
        );
        data.put(
                "preInstructions",
                visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartment.getId())
                        .stream()
                        .map(this::visitPreInstructionToMap)
                        .toList()
        );
        data.put(
            "childVisitDepartments",
            visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(child -> visitDepartmentToMap(child, visitInsuranceProviderIds, new LinkedHashSet<>(visitedDepartmentIds)))
                .toList()
        );
        data.put("createdAt", visitDepartment.getCreatedAt());
        data.put("updatedAt", visitDepartment.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitPreInstructionProductToMap(
            VisitPreInstructionProductRequest item,
            Set<UUID> visitInsuranceProviderIds
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("product", productToMap(item.getProduct(), visitInsuranceProviderIds));
        data.put("quantity", item.getQuantity());
        data.put("requestedBy", workerToMap(item.getRequestedBy()));
        data.put("status", item.getStatus());
        data.put("processedBy", workerToMap(item.getProcessedBy()));
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentDiagnosisToMap(VisitDepartmentDiagnosis item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("diagnosisName", item.getDiagnosisName());
        data.put("icd11Code", item.getIcd11Code());
        data.put("createdAt", item.getCreatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentMedicationToMap(VisitDepartmentMedication item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("medicationName", item.getMedicationName());
        data.put("instructions", item.getInstructions());
        data.put("createdAt", item.getCreatedAt());
        return data;
    }

    private Map<String, Object> visitDepartmentProductToMap(
            VisitDepartmentProduct item,
            Set<UUID> visitInsuranceProviderIds
    ) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("product", productToMap(item.getProduct(), visitInsuranceProviderIds));
        data.put("quantity", item.getQuantity());
        data.put("price", item.getPrice());
        data.put("status", item.getStatus());
        data.put("addedBy", workerToMap(item.getAddedBy()));
        data.put("billedBy", workerToMap(item.getBilledBy()));
        data.put("processor", workerToMap(item.getProcessor()));
        data.put("createdAt", item.getCreatedAt());
        data.put("updatedAt", item.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitVitalSignsGroupToMap(VisitVitalSignsGroup group) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", group.getId());
        data.put("createdAt", group.getCreatedAt());
        data.put("addedBy", workerToMap(group.getAddedBy()));
        data.put(
                "measurements",
                vitalMeasurementRepository.findByGroupIdOrderByCreatedAtAsc(group.getId())
                        .stream()
                        .map(this::vitalMeasurementToMap)
                        .toList()
        );
        return data;
    }

    private Map<String, Object> vitalMeasurementToMap(VitalMeasurement item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("measurementName", item.getMeasurementName());
        data.put("value", item.getValue());
        data.put("unit", item.getUnit());
        data.put("createdAt", item.getCreatedAt());
        return data;
    }

    private Map<String, Object> departmentToMap(Department department) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", department.getId());
        data.put("name", department.getName());
        data.put("insurancePolicyMode", department.getInsurancePolicyMode());
        data.put("createdAt", department.getCreatedAt());
        data.put("updatedAt", department.getUpdatedAt());
        data.put("nursing", department.isNursing());
        data.put("supportRequests", department.isSupportRequests());
        data.put("requestsProducts", department.isRequestsProducts());
        return data;
    }

    private Map<String, Object> productToMap(Product product, Set<UUID> visitInsuranceProviderIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", product.getId());
        data.put("name", product.getName());
        data.put("genericName", product.getGenericName());
        data.put("code", product.getCode());
        data.put("description", product.getDescription());
        data.put("type", product.getType());
        data.put("unit", product.getUnit());
        data.put("metadata", product.getMetadata());
        data.put("privateRhicPrice", product.getPrivateRhicPrice());
        data.put("clinicPrice", product.getClinicPrice());
        data.put("insuranceCoverages", resolveProductInsuranceCoverages(product, visitInsuranceProviderIds));
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
        return data;
    }

    private Set<UUID> resolveVisitInsuranceProviderIds(UUID visitId) {
        return visitInsuranceRepository.findByVisitId(visitId).stream()
                .map(link -> link.getPatientInsurance().getInsuranceProvider().getId())
                .collect(Collectors.toSet());
    }

    private List<Map<String, Object>> resolveProductInsuranceCoverages(Product product, Set<UUID> visitInsuranceProviderIds) {
        if (visitInsuranceProviderIds == null || visitInsuranceProviderIds.isEmpty()) {
            return List.of();
        }

        return productInsuranceCoverageRepository.findByProductId(product.getId()).stream()
                .filter(coverage -> visitInsuranceProviderIds.contains(coverage.getInsuranceProvider().getId()))
                .map(this::productCoverageToMap)
                .toList();
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

    private Map<String, Object> workerToMap(Worker worker) {
        if (worker == null) {
            return null;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", worker.getId());
        data.put("firstName", worker.getFirstName());
        data.put("lastName", worker.getLastName());
        data.put("username", worker.getUsername());
        data.put("email", worker.getEmail());
        return data;
    }

    private Map<String, Object> patientToMap(Patient patient) {
        return patientToMap(patient, true);
    }

    private Map<String, Object> patientSummaryToMap(Patient patient) {
        return patientToMap(patient, false);
    }

    private Map<String, Object> patientToMap(Patient patient, boolean includePatientInsurances) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", patient.getId());
        data.put("firstName", patient.getFirstName());
        data.put("middleName", patient.getMiddleName());
        data.put("lastName", patient.getLastName());
        data.put("fullName", patient.getFullName());
        data.put("dateOfBirth", patient.getDateOfBirth());
        data.put("age", patient.getDateOfBirth() == null ? null : Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears());
        data.put("gender", patient.getGender());
        data.put("primaryPhoneNumber", patient.getPrimaryPhoneNumber());
        data.put("alternativePhone", patient.getAlternativePhone());
        data.put("village", patient.getVillage());
        data.put("city", patient.getCity());
        data.put("district", patient.getDistrict());
        data.put("postalAddress", patient.getPostalAddress());
        data.put("nationalIdNumber", patient.getNationalIdNumber());
        data.put("passportNumber", patient.getPassportNumber());
        data.put("emergencyContactName", patient.getEmergencyContactName());
        data.put("emergencyContactRelationship", patient.getEmergencyContactRelationship());
        data.put("emergencyContactPhoneNumber", patient.getEmergencyContactPhoneNumber());
        data.put(
            "patientInsurances",
            includePatientInsurances
                ? patientInsuranceRepository.findByPatientId(patient.getId()).stream().map(this::patientInsuranceToMap).toList()
                : List.of()
        );
        data.put("createdAt", patient.getCreatedAt());
        data.put("updatedAt", patient.getUpdatedAt());
        return data;
    }

    private Map<String, Object> patientInsuranceToMap(PatientInsurance patientInsurance) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", patientInsurance.getId());
        data.put("patient", patientSummaryToMap(patientInsurance.getPatient()));
        data.put("insuranceProvider", insuranceProviderToMap(patientInsurance.getInsuranceProvider()));
        data.put("insuranceCardNumber", patientInsurance.getInsuranceCardNumber());
        data.put("providingCompanyOrEmployer", patientInsurance.getProvidingCompanyOrEmployer());
        data.put("principalMember", patientInsurance.isPrincipalMember());
        data.put("principalMemberName", patientInsurance.getPrincipalMemberName());
        data.put("principalMemberPhoneNumber", patientInsurance.getPrincipalMemberPhoneNumber());
        data.put("validFrom", patientInsurance.getValidFrom());
        data.put("validUntil", patientInsurance.getValidUntil());
        data.put("createdAt", patientInsurance.getCreatedAt());
        data.put("updatedAt", patientInsurance.getUpdatedAt());
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

    private Set<UUID> normalizeDepartmentIds(List<UUID> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return Set.of();
        }

        Set<UUID> normalized = new LinkedHashSet<>();
        for (UUID departmentId : departmentIds) {
            if (departmentId != null) {
                normalized.add(departmentId);
            }
        }

        return normalized;
    }

    private List<VisitDepartment> resolveVisitDepartmentsForResponse(UUID visitId, Set<UUID> departmentIds) {
        if (departmentIds == null || departmentIds.isEmpty()) {
            return visitDepartmentRepository.findByVisitIdAndParentVisitDepartmentIsNull(visitId);
        }

        List<VisitDepartment> visitDepartments = new ArrayList<>();
        for (UUID departmentId : departmentIds) {
            visitDepartmentRepository.findByVisitIdAndDepartmentId(visitId, departmentId)
                    .ifPresent(visitDepartments::add);
        }
        return visitDepartments;
    }

    private DateWindow resolvePatientHistoryDateWindow(SearchPatientHistoryInput input) {
        if (input == null) {
            return new DateWindow(null, null);
        }

        if (input.startDate() != null || input.endDate() != null) {
            LocalDateTime start = input.startDate() == null ? null : input.startDate().atStartOfDay();
            LocalDateTime endExclusive = input.endDate() == null ? null : input.endDate().plusDays(1).atStartOfDay();
            if (start != null && endExclusive != null && endExclusive.isBefore(start)) {
                throw new IllegalArgumentException("endDate must be on or after startDate.");
            }
            return new DateWindow(start, endExclusive);
        }

        if (input.startMonth() != null || input.endMonth() != null) {
            YearMonth startMonth = input.startMonth();
            YearMonth endMonth = input.endMonth();
            LocalDateTime start = startMonth == null ? null : startMonth.atDay(1).atStartOfDay();
            LocalDateTime endExclusive = endMonth == null ? null : endMonth.plusMonths(1).atDay(1).atStartOfDay();
            if (start != null && endExclusive != null && endExclusive.isBefore(start)) {
                throw new IllegalArgumentException("endMonth must be on or after startMonth.");
            }
            return new DateWindow(start, endExclusive);
        }

        if (input.startYear() != null || input.endYear() != null) {
            Integer startYear = input.startYear();
            Integer endYear = input.endYear();
            LocalDateTime start = startYear == null ? null : LocalDate.of(startYear, 1, 1).atStartOfDay();
            LocalDateTime endExclusive = endYear == null ? null : LocalDate.of(endYear + 1, 1, 1).atStartOfDay();
            if (start != null && endExclusive != null && endExclusive.isBefore(start)) {
                throw new IllegalArgumentException("endYear must be on or after startYear.");
            }
            return new DateWindow(start, endExclusive);
        }

        if (input.day() != null) {
            if (input.year() == null || input.month() == null) {
                throw new IllegalArgumentException("year and month are required when day is provided.");
            }
            LocalDateTime start = LocalDate.of(input.year(), input.month(), input.day()).atStartOfDay();
            return new DateWindow(start, start.plusDays(1));
        }

        if (input.month() != null) {
            if (input.year() == null) {
                throw new IllegalArgumentException("year is required when month is provided.");
            }
            LocalDateTime start = LocalDate.of(input.year(), input.month(), 1).atStartOfDay();
            return new DateWindow(start, start.plusMonths(1));
        }

        if (input.year() != null) {
            LocalDateTime start = LocalDate.of(input.year(), 1, 1).atStartOfDay();
            return new DateWindow(start, start.plusYears(1));
        }

        return new DateWindow(null, null);
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

    private record DateWindow(LocalDateTime start, LocalDateTime endExclusive) {
    }

    private void reopenVisitIfCompleted(Visit visit) {
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            visit.setStatus(VisitStatus.IN_PROGRESS);
            visitRepository.save(visit);
        }
    }

    private ApiResponse assignVisitDepartmentProductProcessor(
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
                return ApiResponse.error("processorId must belong to the visit department processors.", "INVALID_PROCESSOR");
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
                return ApiResponse.error("processorId is required when supportRequests is enabled.", "VALIDATION_ERROR");
            }

            item.setProcessor(null);
            return null;
        }

        return ApiResponse.error("processorId is required when the visit department has multiple processors.", "VALIDATION_ERROR");
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

    private void addProcessorToVisitDepartment(VisitDepartment department, Worker worker) {
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

    private VisitDepartment deleteChildVisitDepartmentIfEmpty(VisitDepartment visitDepartment) {
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

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    private BigDecimal normalizeQuantity(BigDecimal value) {
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

    private BigDecimal resolveUnitPriceSnapshot(Product product, BigDecimal inputPrice) {
        if (inputPrice != null && inputPrice.compareTo(BigDecimal.ZERO) >= 0) {
            return normalizePrice(inputPrice);
        }

        if (product != null && product.getClinicPrice() != null && product.getClinicPrice().compareTo(BigDecimal.ZERO) >= 0) {
            return normalizePrice(product.getClinicPrice());
        }

        return BigDecimal.ZERO;
    }

    @Transactional
    public ApiResponse addDiagnosisToVisitDepartment(AddDiagnosisInput input) {
        if (input == null || input.visitDepartmentId() == null || input.diagnosisName() == null || input.diagnosisName().isBlank()) {
            return ApiResponse.error("visitDepartmentId and diagnosisName are required.", "VALIDATION_ERROR");
        }

        UUID visitDeptId;
        try {
            visitDeptId = UUID.fromString(input.visitDepartmentId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid visitDepartmentId format.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> visitDeptOptional = visitDepartmentRepository.findById(visitDeptId);
        if (visitDeptOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment visitDept = visitDeptOptional.get();
        if (visitDept.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add diagnostics to a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add diagnostics to a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        VisitDepartmentDiagnosis diagnosis = new VisitDepartmentDiagnosis();
        diagnosis.setVisitDepartment(visitDept);
        diagnosis.setDiagnosisName(input.diagnosisName().trim());
        diagnosis.setIcd11Code(input.icd11Code() == null || input.icd11Code().isBlank() ? null : input.icd11Code().trim());

        visitDepartmentDiagnosisRepository.save(diagnosis);
        return ApiResponse.success("Diagnosis added successfully.", visitDepartmentToMap(visitDept));
    }

    @Transactional
    public ApiResponse addMedicationToVisitDepartment(AddMedicationInput input) {
        if (input == null || input.visitDepartmentId() == null || input.medicationName() == null || input.medicationName().isBlank() || input.instructions() == null || input.instructions().isBlank()) {
            return ApiResponse.error("visitDepartmentId, medicationName and instructions are required.", "VALIDATION_ERROR");
        }

        UUID visitDeptId;
        try {
            visitDeptId = UUID.fromString(input.visitDepartmentId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid visitDepartmentId format.", "VALIDATION_ERROR");
        }

        Optional<VisitDepartment> visitDeptOptional = visitDepartmentRepository.findById(visitDeptId);
        if (visitDeptOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.", "NOT_FOUND");
        }

        VisitDepartment visitDept = visitDeptOptional.get();
        if (visitDept.getStatus() == VisitDepartmentStatus.COMPLETED) {
            return ApiResponse.error("Cannot add medication to a completed department.", "DEPARTMENT_IS_COMPLETED");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add medication to a cancelled department.", "DEPARTMENT_IS_CANCELLED");
        }

        VisitDepartmentMedication medication = new VisitDepartmentMedication();
        medication.setVisitDepartment(visitDept);
        medication.setMedicationName(input.medicationName().trim());
        medication.setInstructions(input.instructions().trim());

        visitDepartmentMedicationRepository.save(medication);
        return ApiResponse.success("Medication added successfully.", visitDepartmentToMap(visitDept));
    }
}
