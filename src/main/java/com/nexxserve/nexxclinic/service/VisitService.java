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
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.mappers.out.*;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
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

    private final WorkerMapper workerMapper;
    private final PatientMapper patientMapper;
    private final PatientInsuranceMapper patientInsuranceMapper;
    private final DepartmentMapper departmentMapper;
    private final ProductMapper productMapper;
    private final InsuranceProviderMapper insuranceProviderMapper;

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
            WorkerRepository workerRepository,
            WorkerMapper workerMapper,
            PatientMapper patientMapper,
            PatientInsuranceMapper patientInsuranceMapper,
            DepartmentMapper departmentMapper,
            ProductMapper productMapper,
            InsuranceProviderMapper insuranceProviderMapper
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
        this.workerMapper = workerMapper;
        this.patientMapper = patientMapper;
        this.patientInsuranceMapper = patientInsuranceMapper;
        this.departmentMapper = departmentMapper;
        this.productMapper = productMapper;
        this.insuranceProviderMapper = insuranceProviderMapper;
    }


    @Transactional
    public ApiResponse<VisitDto> createVisit(CreateVisitInput input, AuthenticatedUser authUser) {
        if (input == null || input.patientId() == null) {
            return ApiResponse.error("patientId is required.");
        }

        Optional<Patient> patientOptional = patientRepository.findById(input.patientId());
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.");
        }

        List<PatientInsurance> linkedInsurances = resolveLinkedInsurances(input.patientId(), input.linkedPatientInsuranceIds());
        if (linkedInsurances == null) {
            return ApiResponse.error("Each linked insurance must exist and belong to the selected patient.");
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
        return ApiResponse.success("Visit created.", visitToDto(latest));
    }

    @Transactional
    public ApiResponse<VisitDto> changeVisitDate(ChangeVisitDateInput input) {
        if (input == null || input.visitId() == null || input.visitDate() == null) {
            return ApiResponse.error("visitId and visitDate are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        int updatedRows = visitRepository.updateVisitDate(input.visitId(), input.visitDate());
        if (updatedRows == 0) {
            return ApiResponse.error("Visit date could not be updated.");
        }

        Visit updatedVisit = visitRepository.findById(input.visitId()).orElse(visitOptional.get());
        return ApiResponse.success("Visit date changed.", visitToDto(updatedVisit));
    }

    @Transactional(readOnly = true)
    public ApiResponse<VisitDto> visit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        return visitOptional.map(visit -> ApiResponse.success("Visit fetched.", visitToDto(visit))).orElseGet(() -> ApiResponse.error("Visit not found."));

    }

    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDto>> visits(SearchVisitsInput input) {
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
        List<VisitDto> visits = visitPage.getContent().stream().map(this::visitToDto).toList();

        return ApiResponse.success("Visits fetched.", visits, new PaginationDto(visitPage.getTotalElements(), visitPage.getSize(), visitPage.getNumber(), visitPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDto>> getPatientHistory(UUID patientId, SearchPatientHistoryInput input) {
        if (patientId == null) {
            return ApiResponse.error("patientId is required.");
        }

        Optional<Patient> patientOptional = patientRepository.findById(patientId);
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.");
        }

        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        DateWindow dateWindow;
        try {
            dateWindow = resolvePatientHistoryDateWindow(input);
        } catch (IllegalArgumentException ex) {
            return ApiResponse.error(ex.getMessage());
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
                return ApiResponse.success("Patient history fetched.", List.of(), new PaginationDto(emptyPage.getTotalElements(), emptyPage.getSize(), emptyPage.getNumber(), emptyPage.getTotalPages()));
            }

            spec = spec.and((root, queryDef, builder) -> root.get("id").in(matchingVisitIds));
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<VisitDto> visits = visitPage.getContent().stream()
                .map(visit -> visitToDto(visit, departmentIds))
                .toList();

        return ApiResponse.success("Patient history fetched.", visits, new PaginationDto(visitPage.getTotalElements(), visitPage.getSize(), visitPage.getNumber(), visitPage.getTotalPages()));
    }

    @Transactional
    public ApiResponse<VisitDto> completeVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visit cannot be completed.");
        }

        // Check if all products on the visit are billed (not PENDING)
        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        boolean hasUnbilledProducts = visitProducts.stream()
                .anyMatch(product -> product.getStatus() == VisitProductStatus.PENDING);
        
        if (hasUnbilledProducts) {
            return ApiResponse.error("Cannot complete visit with unbilled products. All products must be billed first.");
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
        return ApiResponse.success("Visit completed.", visitToDto(saved));
    }

    @Transactional
    public ApiResponse<VisitDto> completeVisit(ConsultationAnswersInput input, boolean finalAnswer, AuthenticatedUser authUser) {
        if (input == null) {
            return ApiResponse.error("input is required.");
        }

        if (input.visitId() == null || input.departmentId() == null) {
            return ApiResponse.error("visitId and departmentId are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visit cannot be completed.");
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
        return ApiResponse.success("Visit submitted for billing.", visitToDto(saved));
    }

    @Transactional
    public ApiResponse<VisitDto> saveAnswerAndCompleteVisit(ConsultationAnswersInput input, boolean finalAnswer, AuthenticatedUser authUser) {
        return completeVisit(input, finalAnswer, authUser);
    }

    @Transactional
    public ApiResponse<VisitDto> cancelVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Completed visit cannot be cancelled.");
        }

        // Check if there are any products on the visit
        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        if (!visitProducts.isEmpty()) {
            return ApiResponse.error("Cannot cancel visit with existing products. Remove all products first.");
        }

        // Mark all departments as CANCELLED
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            dept.setStatus(VisitDepartmentStatus.CANCELLED);
            visitDepartmentRepository.save(dept);
        }

        visit.setStatus(VisitStatus.CANCELLED);
        Visit saved = visitRepository.save(visit);
        return ApiResponse.success("Visit cancelled.", visitToDto(saved));
    }

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

        return ApiResponse.success("Department added to visit.", visitToDto(visit));
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

        // Validate all products before creating the child department
        Worker actingUser = resolveWorker(authUser);
        Set<UUID> seenProducts = new LinkedHashSet<>();
        
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
                return ApiResponse.error("processorId must belong to the visit department processors.");
            }
        }

        // All validations passed, now create the child department
        VisitDepartment child = new VisitDepartment();
        child.setVisit(visit);
        child.setDepartment(childDepartment);
        child.setParentVisitDepartment(parent);
        if (input.encounterType() != null) {
            child.setEncounterType(input.encounterType());
        }
        child.setStatus(VisitDepartmentStatus.PENDING);

        VisitDepartment savedChild = visitDepartmentRepository.save(child);
        
        // Add all products to the child department
        for (var productInput : input.products()) {
            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                // This shouldn't happen as we already validated above, but safe to check
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

    @Transactional
    public ApiResponse<VisitDto> linkVisitInsurances(UUID visitId, List<UUID> insuranceIds, AuthenticatedUser authUser) {
        if (visitId == null || insuranceIds == null) {
            return ApiResponse.error("visitId and insuranceIds are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add insurances to a completed visit.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add insurances to a cancelled visit.");
        }

        List<UUID> uniqueIds = normalizeUniqueIds(insuranceIds);
        if (uniqueIds.isEmpty()) {
            return ApiResponse.error("insuranceIds must not be empty.");
        }

        List<PatientInsurance> patientInsurances = resolveLinkedInsurances(visit.getPatient().getId(), uniqueIds);
        if (patientInsurances == null) {
            return ApiResponse.error("Each insurance must exist and belong to the selected patient.");
        }

        for (PatientInsurance patientInsurance : patientInsurances) {
            if (visitInsuranceRepository.existsByVisitIdAndPatientInsuranceId(visitId, patientInsurance.getId())) {
                return ApiResponse.error("One or more insurances are already linked to this visit.");
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
        return ApiResponse.success("Insurance linked to visit.", visitToDto(refreshedVisit));
    }

    @Transactional
    public ApiResponse<VisitDto> unlinkVisitInsurances(UUID visitId, List<UUID> insuranceIds, AuthenticatedUser authUser) {
        if (visitId == null || insuranceIds == null) {
            return ApiResponse.error("visitId and insuranceIds are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot remove insurances from a completed visit.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot remove insurances from a cancelled visit.");
        }

        List<UUID> uniqueIds = normalizeUniqueIds(insuranceIds);
        if (uniqueIds.isEmpty()) {
            return ApiResponse.error("insuranceIds must not be empty.");
        }

        List<VisitInsurance> visitInsurances = visitInsuranceRepository.findByVisitId(visitId);
        List<VisitInsurance> linksToRemove = new ArrayList<>();
        for (VisitInsurance visitInsurance : visitInsurances) {
            if (visitInsurance.getPatientInsurance() != null && uniqueIds.contains(visitInsurance.getPatientInsurance().getId())) {
                linksToRemove.add(visitInsurance);
            }
        }

        if (linksToRemove.size() != uniqueIds.size()) {
            return ApiResponse.error("One or more insurances are not linked to this visit.");
        }

        visitInsuranceRepository.deleteAll(linksToRemove);
        Visit refreshedVisit = visitRepository.findById(visitId).orElse(visit);
        return ApiResponse.success("Insurance unlinked from visit.", visitToDto(refreshedVisit));
    }

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
    public ApiResponse<VisitDto> addVisitVitalSigns(AddVisitVitalSignsInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitId() == null || input.vitalSigns() == null || input.vitalSigns().isEmpty()) {
            return ApiResponse.error("visitId and vitalSigns are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot add vital signs to a completed visit.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add vital signs to a cancelled visit.");
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
                return ApiResponse.error("measurementName, value and unit are required for each vital sign.");
            }

            VitalMeasurement item = new VitalMeasurement();
            item.setGroup(group);
            item.setMeasurementName(vitalSignInput.measurementName().trim());
            item.setValue(vitalSignInput.value().trim());
            item.setUnit(vitalSignInput.unit().trim());
            measurements.add(item);
        }

        vitalMeasurementRepository.saveAll(measurements);
        return ApiResponse.success("Vital signs added to visit.", visitToDto(visit));
    }

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
                    Optional<com.nexxserve.nexxclinic.entity.Product> productOptional = productRepository.findById(prodIn.productId());
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

        if (input.quantity().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            // Delete product from visit department when quantity is 0 or less
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

        // Delete the product from visit department
        visitDepartmentProductRepository.delete(item);
        reopenVisitIfCompleted(visit);

        VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartment);
        return ApiResponse.success("Visit department product removed.", visitDepartmentToDto(mappedDepartment == null ? affectedDepartment : mappedDepartment));
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
                return ApiResponse.error("departmentId is required for each visit department.");
            }

            if (!seenDepartments.add(departmentInput.departmentId())) {
                return ApiResponse.error("Duplicate departmentId found in visit departments.");
            }

            Optional<Department> departmentOptional = departmentRepository.findById(departmentInput.departmentId());
            if (departmentOptional.isEmpty()) {
                return ApiResponse.error("Department not found.");
            }

            VisitDepartment visitDepartment = new VisitDepartment();
            visitDepartment.setVisit(visit);
            visitDepartment.setDepartment(departmentOptional.get());
            if (departmentInput.encounterType() != null) {
                visitDepartment.setEncounterType(departmentInput.encounterType());
            }
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
                return ApiResponse.error("productId is required for each department product.");
            }

            if (!seenProducts.add(productInput.productId())) {
                return ApiResponse.error("Duplicate productId found in visit department products.");
            }

            Optional<Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.");
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















    private VisitDto visitToDto(Visit visit) {
        return visitToDto(visit, Set.of());
    }

    private VisitDto visitToDto(Visit visit, Set<UUID> departmentIds) {
        Set<UUID> visitInsuranceProviderIds = resolveVisitInsuranceProviderIds(visit.getId());
        List<PatientInsuranceDto> linkedInsurances = visitInsuranceRepository.findByVisitId(visit.getId())
                .stream()
                .map(link -> patientInsuranceMapper.toDto(link.getPatientInsurance()))
                .toList();

        List<VisitDepartmentDto> departments = resolveVisitDepartmentsForResponse(visit.getId(), departmentIds)
                .stream()
                .map(visitDepartment -> visitDepartmentToDto(visitDepartment, visitInsuranceProviderIds))
                .toList();

        List<VisitVitalSignsGroupDto> vitalSigns = visitVitalSignsGroupRepository.findByVisitIdOrderByCreatedAtAsc(visit.getId())
                .stream()
                .map(this::visitVitalSignsGroupToDto)
                .toList();

        return new VisitDto(
                visit.getId(),
                patientMapper.toDto(visit.getPatient(), patientInsuranceRepository.findByPatientId(visit.getPatient().getId())),
                visit.getStatus(),
                visit.getVisitDate(),
                linkedInsurances,
                departments,
                vitalSigns
        );
    }

    private VisitDepartmentDto visitDepartmentToDto(VisitDepartment visitDepartment) {
        return visitDepartmentToDto(
                visitDepartment,
                resolveVisitInsuranceProviderIds(visitDepartment.getVisit().getId()),
                new LinkedHashSet<>()
        );
    }

    private VisitDepartmentDto visitDepartmentToDto(VisitDepartment visitDepartment, Set<UUID> visitInsuranceProviderIds) {
        return visitDepartmentToDto(visitDepartment, visitInsuranceProviderIds, new LinkedHashSet<>());
    }

    private VisitDepartmentDto visitDepartmentToDto(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            Set<UUID> visitedDepartmentIds
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
                .map(child -> visitDepartmentToDto(child, visitInsuranceProviderIds, new LinkedHashSet<>(visitedDepartmentIds)))
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

    private VisitVitalSignsGroupDto visitVitalSignsGroupToDto(VisitVitalSignsGroup group) {
        List<VitalMeasurementDto> measurements = vitalMeasurementRepository.findByGroupIdOrderByCreatedAtAsc(group.getId())
                .stream()
                .map(this::vitalMeasurementToDto)
                .toList();

        return new VisitVitalSignsGroupDto(
                group.getId(),
                group.getCreatedAt(),
                workerMapper.toDto(group.getAddedBy()),
                measurements
        );
    }

    private VitalMeasurementDto vitalMeasurementToDto(VitalMeasurement item) {
        return new VitalMeasurementDto(
                item.getId(),
                item.getMeasurementName(),
                item.getValue(),
                item.getUnit(),
                item.getCreatedAt()
        );
    }


    private Set<UUID> resolveVisitInsuranceProviderIds(UUID visitId) {
        return visitInsuranceRepository.findByVisitId(visitId).stream()
                .map(link -> link.getPatientInsurance().getInsuranceProvider().getId())
                .collect(Collectors.toSet());
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
}
