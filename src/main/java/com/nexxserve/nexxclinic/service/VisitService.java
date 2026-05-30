package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
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

        Specification<Visit> spec = (root, queryDef, builder) -> builder.equal(root.join("patient").get("id"), patientId);

        if (dateWindow.start() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.greaterThanOrEqualTo(root.get("visitDate"), dateWindow.start()));
        }

        if (dateWindow.endExclusive() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.lessThan(root.get("visitDate"), dateWindow.endExclusive()));
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<Map<String, Object>> visits = visitPage.getContent().stream().map(this::visitToMap).toList();

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

        VisitDepartment saved = visitDepartmentRepository.save(department);
        return ApiResponse.success("Visit department status updated.", visitDepartmentToMap(saved));
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
            visitDepartmentProductRepository.delete(item);
            reopenVisitIfCompleted(visit);
            return ApiResponse.success("Visit department product removed.", visitDepartmentToMap(item.getVisitDepartment()));
        }

        item.setQuantity(input.quantity());

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
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

        // Delete the product from visit department
        visitDepartmentProductRepository.delete(item);
        reopenVisitIfCompleted(visit);

        return ApiResponse.success("Visit department product removed.", visitDepartmentToMap(item.getVisitDepartment()));
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

    private Map<String, Object> visitToMap(Visit visit) {
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
                visitDepartmentRepository.findByVisitId(visit.getId())
                        .stream()
                        .map(this::visitDepartmentToMap)
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
                    pre.getProducts().stream().map(this::visitPreInstructionProductToMap).toList()
                );
        return data;
    }

    private Map<String, Object> visitDepartmentToMap(VisitDepartment visitDepartment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", visitDepartment.getId());
        data.put("department", departmentToMap(visitDepartment.getDepartment()));
        data.put("status", visitDepartment.getStatus());
        data.put("completedAt", visitDepartment.getCompletedAt());
        data.put(
                "products",
                visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream()
                        .map(this::visitDepartmentProductToMap)
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
        data.put("createdAt", visitDepartment.getCreatedAt());
        data.put("updatedAt", visitDepartment.getUpdatedAt());
        return data;
    }

    private Map<String, Object> visitPreInstructionProductToMap(VisitPreInstructionProductRequest item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("product", productToMap(item.getProduct()));
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

    private Map<String, Object> visitDepartmentProductToMap(VisitDepartmentProduct item) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("product", productToMap(item.getProduct()));
        data.put("quantity", item.getQuantity());
        data.put("price", item.getPrice());
        data.put("status", item.getStatus());
        data.put("addedBy", workerToMap(item.getAddedBy()));
        data.put("billedBy", workerToMap(item.getBilledBy()));
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

    private Map<String, Object> productToMap(Product product) {
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
        data.put("createdAt", product.getCreatedAt());
        data.put("updatedAt", product.getUpdatedAt());
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
        data.put("createdAt", patient.getCreatedAt());
        data.put("updatedAt", patient.getUpdatedAt());
        return data;
    }

    private Map<String, Object> patientInsuranceToMap(PatientInsurance patientInsurance) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", patientInsurance.getId());
        data.put("patient", patientToMap(patientInsurance.getPatient()));
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
