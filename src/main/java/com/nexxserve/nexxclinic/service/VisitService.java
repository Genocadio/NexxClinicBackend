package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitVitalSignsGroup;
import com.nexxserve.nexxclinic.entity.VitalMeasurement;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignItemInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.mappers.out.*;
import com.nexxserve.nexxclinic.model.PermissionName;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.BillingState;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import com.nexxserve.nexxclinic.entity.VisitPreInstruction;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentMedicationRepository;
import com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.entity.ConsultationAnswer;
import com.nexxserve.nexxclinic.repository.ConsultationAnswerRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.VisitVitalSignsGroupRepository;
import com.nexxserve.nexxclinic.repository.VitalMeasurementRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VisitService {

    private static final Logger log = LoggerFactory.getLogger(VisitService.class);

    private final VisitRepository visitRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentProductRepository visitDepartmentProductRepository;
    private final VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    private final VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;
    private final VisitPreInstructionRepository visitPreInstructionRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitBillingVersionRepository visitBillingVersionRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;
    private final VisitVitalSignsGroupRepository visitVitalSignsGroupRepository;
    private final VitalMeasurementRepository vitalMeasurementRepository;
    private final DepartmentFormService departmentFormService;
    private final PatientRepository patientRepository;
    private final PatientInsuranceRepository patientInsuranceRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;    private final WorkerMapper workerMapper;
    private final PatientMapper patientMapper;
    private final PatientInsuranceMapper patientInsuranceMapper;

    // Delegate to the split services for DTO building
    private final VisitDepartmentService visitDepartmentService;

    private final com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final com.nexxserve.nexxclinic.service.billing.InvoiceGenerator invoiceGenerator;
    private final MeilisearchIndexService meilisearchIndexService;
    private final VisitPriceEstimateService visitPriceEstimateService;
    private final ConsultationAnswerRepository consultationAnswerRepository;
    private final RolePermissionService rolePermissionService;
    private final com.nexxserve.nexxclinic.service.billing.BillingProductService billingProductService;
    private final VisitDepartmentNoteService visitDepartmentNoteService;
    private final com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher;
    private final VisitArchiveService visitArchiveService;

    public VisitService(
            VisitRepository visitRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository,
            VisitDepartmentMedicationRepository visitDepartmentMedicationRepository,
            VisitPreInstructionRepository visitPreInstructionRepository,
            VisitBillingRepository visitBillingRepository,
            @Lazy VisitBillingVersionRepository visitBillingVersionRepository,
            VisitDepartmentNoteRepository visitDepartmentNoteRepository,
            VisitVitalSignsGroupRepository visitVitalSignsGroupRepository,
            VitalMeasurementRepository vitalMeasurementRepository,
            DepartmentFormService departmentFormService,
            PatientRepository patientRepository,
            PatientInsuranceRepository patientInsuranceRepository,
            DepartmentRepository departmentRepository,
            ProductRepository productRepository,
            WorkerRepository workerRepository,
            WorkerMapper workerMapper,
            PatientMapper patientMapper,
            PatientInsuranceMapper patientInsuranceMapper,
            VisitDepartmentService visitDepartmentService,
            com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
            com.nexxserve.nexxclinic.service.billing.InvoiceGenerator invoiceGenerator,
            MeilisearchIndexService meilisearchIndexService,
            @Lazy VisitPriceEstimateService visitPriceEstimateService,
            ConsultationAnswerRepository consultationAnswerRepository,
            @Lazy RolePermissionService rolePermissionService,
            @Lazy com.nexxserve.nexxclinic.service.billing.BillingProductService billingProductService,
            @Lazy VisitDepartmentNoteService visitDepartmentNoteService,
            com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher,
            VisitArchiveService visitArchiveService
    ) {
        this.visitRepository = visitRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitDepartmentDiagnosisRepository = visitDepartmentDiagnosisRepository;
        this.visitDepartmentMedicationRepository = visitDepartmentMedicationRepository;
        this.visitPreInstructionRepository = visitPreInstructionRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitBillingVersionRepository = visitBillingVersionRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.visitVitalSignsGroupRepository = visitVitalSignsGroupRepository;
        this.vitalMeasurementRepository = vitalMeasurementRepository;
        this.departmentFormService = departmentFormService;
        this.patientRepository = patientRepository;
        this.patientInsuranceRepository = patientInsuranceRepository;
        this.departmentRepository = departmentRepository;
        this.productRepository = productRepository;
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
        this.patientMapper = patientMapper;
        this.patientInsuranceMapper = patientInsuranceMapper;
        this.visitDepartmentService = visitDepartmentService;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.invoiceGenerator = invoiceGenerator;
        this.meilisearchIndexService = meilisearchIndexService;
        this.visitPriceEstimateService = visitPriceEstimateService;
        this.consultationAnswerRepository = consultationAnswerRepository;
        this.rolePermissionService = rolePermissionService;
        this.billingProductService = billingProductService;
        this.visitDepartmentNoteService = visitDepartmentNoteService;
        this.clinicEventPublisher = clinicEventPublisher;
        this.visitArchiveService = visitArchiveService;
    }

    // ─────────────────────────────────────────────────────────────
    //  VISIT CRUD
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDto> createVisit(CreateVisitInput input, AuthenticatedUser authUser) {
        if (input == null || input.patientId() == null) {
            return ApiResponse.error("patientId is required.");
        }

        // Lock the patient row (PESSIMISTIC_WRITE) before the duplicate-open-visit
        // check: two concurrent createVisit calls for the same patient would otherwise
        // both pass the check and each commit a CREATED visit. Serializing on the
        // patient row makes the second request see the first's committed visit.
        Optional<Patient> patientOptional = patientRepository.findByIdForUpdate(input.patientId());
        if (patientOptional.isEmpty()) {
            return ApiResponse.error("Patient not found.");
        }

        // V1 fix: check for duplicate open visits for the same patient.
        List<Visit> openVisits = visitRepository.findByPatientIdAndStatusIn(
            input.patientId(),
            List.of(VisitStatus.CREATED, VisitStatus.IN_PROGRESS)
        );
        if (!openVisits.isEmpty()) {
            return ApiResponse.error("Patient already has an active visit.");
        }

        List<PatientInsurance> linkedInsurances = resolveLinkedInsurances(input.patientId(), input.linkedPatientInsuranceIds());
        if (linkedInsurances == null) {
            return ApiResponse.error("Each linked insurance must exist, belong to the selected patient and be active.");
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
            // C1 fix: the visit + insurance links were already saved above. Returning an
            // error ApiResponse would COMMIT them (Spring only rolls back on exceptions),
            // leaving an orphan CREATED visit with no departments. Mark rollback-only so
            // a failed createVisit is atomic.
            org.springframework.transaction.interceptor.TransactionAspectSupport
                .currentTransactionStatus()
                .setRollbackOnly();
            return departmentError;
        }

        Visit latest = visitRepository.findById(saved.getId()).orElse(saved);
        meilisearchIndexService.indexVisit(latest);
        clinicEventPublisher.publishVisitCreated(latest, actingUser);
        return ApiResponse.success("Visit created.", visitToDto(latest, null, authUser));
    }

    @Transactional
    public ApiResponse<VisitDto> changeVisitDate(
            ChangeVisitDateInput input,
            AuthenticatedUser authUser
    ) {
        if (input == null || input.visitId() == null || input.visitDate() == null) {
            return ApiResponse.error("visitId and visitDate are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(input.visitId());
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit existingVisit = visitOptional.get();
        LocalDateTime oldVisitDate = existingVisit.getVisitDate();
        LocalDateTime newVisitDate = input.visitDate();
        if (existingVisit.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot change the date of a cancelled visit.");
        }
        // Per-department editing: check if any department is in DEPARTMENT_EDITING
        boolean hasEditingDept = visitDepartmentRepository.findByVisitId(input.visitId()).stream()
            .anyMatch(d -> d.getStatus() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.DEPARTMENT_EDITING);
        if (hasEditingDept) {
            return ApiResponse.error("Cannot change the visit date while a department is in billing edit mode.");
        }

        int updatedRows = visitRepository.updateVisitDate(input.visitId(), input.visitDate());
        if (updatedRows == 0) {
            return ApiResponse.error("Visit date could not be updated.");
        }

        Visit updatedVisit = visitRepository.findById(input.visitId()).orElse(existingVisit);
        visitArchiveService.scheduleFolderMove(updatedVisit, oldVisitDate, newVisitDate);

        // When the visit date changes after billing, recalculate each billing's
        // billing date by applying the same shift as the visit date moved, then
        // clamp so the billing date is never before the (new) visit date.
        List<com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling> billingsToRegenerate =
            departmentInsuranceBillingRepository.findAllByVisitId(input.visitId());
        if (!billingsToRegenerate.isEmpty()) {
            for (com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling b : billingsToRegenerate) {
                if (oldVisitDate != null && b.getBillingDate() != null) {
                    Duration visitDelta = Duration.between(oldVisitDate, newVisitDate);
                    java.time.LocalDateTime shiftedBillingDate = b.getBillingDate().plus(visitDelta);
                    java.time.LocalDateTime minBilling = newVisitDate.plusMinutes(5);
                    if (shiftedBillingDate.isBefore(minBilling)) {
                        shiftedBillingDate = minBilling;
                    }
                    b.setBillingDate(shiftedBillingDate);
                }
                b.setInvoiceUrl(null);
                departmentInsuranceBillingRepository.save(b);
            }
            // Regenerate invoices after the transaction commits so the expensive
            // PDF render and upload do not hold the DB connection open.
            List<UUID> billingIds = billingsToRegenerate.stream()
                .map(com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling::getId)
                .toList();
            org.springframework.transaction.support.TransactionSynchronizationManager
                .registerSynchronization(
                    new org.springframework.transaction.support.TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            for (UUID bid : billingIds) {
                                try {
                                    com.nexxserve.nexxclinic.dto.out.ApiResponse<?> result =
                                        invoiceGenerator.generateInvoice(bid, authUser);
                                    if (result.status() != com.nexxserve.nexxclinic.model.ResponseStatus.SUCCESS) {
                                        log.warn(
                                            "Invoice regeneration after visit date change for {} returned: {}",
                                            bid, result.message()
                                        );
                                    }
                                } catch (Exception e) {
                                    log.error(
                                        "Failed to regenerate invoice after visit date change for {}: {}",
                                        bid, e.getMessage(), e
                                    );
                                }
                            }
                        }
                    }
                );
        }

        meilisearchIndexService.indexVisit(updatedVisit);
        return ApiResponse.success("Visit date changed.", visitToDto(updatedVisit));
    }

    /**
     * Read-only preview of how each billing row's billing date will change if the
     * visit date is moved to {@code newVisitDate}. Applies the exact same shift +
     * clamp (never before newVisitDate + 5 minutes) that changeVisitDate performs,
     * but does NOT persist anything. Used by the manager-facing change-visit-date
     * flow to show proposed billing dates before the change is confirmed.
     */
    @Transactional(readOnly = true)
    public ApiResponse<java.util.List<VisitBillingDatePreviewDto>> previewChangeVisitDate(
            UUID visitId,
            LocalDateTime newVisitDate
    ) {
        if (visitId == null || newVisitDate == null) {
            return ApiResponse.error("visitId and newVisitDate are required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit existingVisit = visitOptional.get();
        if (existingVisit.getStatus() == com.nexxserve.nexxclinic.model.VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot change the date of a cancelled visit.");
        }
        // Per-department editing: check if any department is in DEPARTMENT_EDITING
        boolean hasEditingDeptPreview = visitDepartmentRepository.findByVisitId(visitId).stream()
            .anyMatch(d -> d.getStatus() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.DEPARTMENT_EDITING);
        if (hasEditingDeptPreview) {
            return ApiResponse.error("Cannot change the visit date while a department is in billing edit mode.");
        }

        LocalDateTime oldVisitDate = existingVisit.getVisitDate();
        Duration visitDelta = oldVisitDate == null
            ? Duration.ZERO
            : Duration.between(oldVisitDate, newVisitDate);
        LocalDateTime minBilling = newVisitDate.plusMinutes(5);

        List<VisitBillingDatePreviewDto> previews = new ArrayList<>();
        for (com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling b
                : departmentInsuranceBillingRepository.findAllByVisitId(visitId)) {
            LocalDateTime current = b.getBillingDate();
            LocalDateTime proposed = current == null ? null : current.plus(visitDelta);
            if (proposed != null && proposed.isBefore(minBilling)) {
                proposed = minBilling;
            }

            String departmentName = null;
            String insuranceLabel = "Private";
            if (b.getVisitDepartmentBilling() != null
                    && b.getVisitDepartmentBilling().getVisitDepartment() != null
                    && b.getVisitDepartmentBilling().getVisitDepartment().getDepartment() != null) {
                departmentName = b.getVisitDepartmentBilling()
                    .getVisitDepartment().getDepartment().getName();
            }
            if (b.getPatientInsurance() != null
                    && b.getPatientInsurance().getInsuranceProvider() != null
                    && b.getPatientInsurance().getInsuranceProvider().getInsuranceName() != null) {
                insuranceLabel = b.getPatientInsurance()
                    .getInsuranceProvider().getInsuranceName();
            }

            previews.add(new VisitBillingDatePreviewDto(
                b.getId(),
                departmentName,
                insuranceLabel,
                current,
                proposed,
                b.getInvoiceUrl()
            ));
        }

        return ApiResponse.success("Preview computed.", previews);
    }

    @Transactional
    public ApiResponse<VisitDto> visit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        return visitOptional
                .map(visit -> ApiResponse.success("Visit fetched.", visitToDto(visit, Set.of(), authUser)))
                .orElseGet(() -> ApiResponse.error("Visit not found."));
    }

    @Transactional
    public ApiResponse<List<VisitDto>> visits(SearchVisitsInput input, AuthenticatedUser authUser) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        String patientName = input == null ? null : blankToNull(input.patientName());
        com.nexxserve.nexxclinic.model.VisitStatus statusFilter = input == null ? null : input.status();
        boolean activeOnly = input != null && Boolean.TRUE.equals(input.activeOnly());

        LocalDateTime parsedVisitDate = parseDateTimeOrStartOfDay(input == null ? null : input.visitDate());
        LocalDateTime parsedFromDate = parseDateTimeOrStartOfDay(input == null ? null : input.fromDate());
        LocalDateTime parsedToDate = parseDateTimeOrStartOfDay(input == null ? null : input.toDate());

        Long visitDateEpochDay = null;
        if (parsedVisitDate != null) {
            visitDateEpochDay = parsedVisitDate.toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        // Meilisearch first (typo-tolerant, ranked); the DB spec below is the fallback.
        // Only invoke Meilisearch when there's a simple patientName search param without complex date range
        boolean hasSearchParam = patientName != null;
        boolean restrictedClinicianSearch = authUser != null
                && authUser.roles() != null
                && authUser.roles().contains(RoleName.CLINICIAN)
                && !authUser.roles().contains(RoleName.ADMIN)
                && !authUser.roles().contains(RoleName.CLINIC_ADMIN)
                && !authUser.roles().contains(RoleName.MANAGER)
                && !authUser.roles().contains(RoleName.RECEPTION)
                && !authUser.roles().contains(RoleName.NURSE)
                && !(rolePermissionService != null
                        && rolePermissionService.userHasAnyPermission(authUser, PermissionName.VISIT_VIEW_ALL));

        if (hasSearchParam && !restrictedClinicianSearch
                && parsedFromDate == null && parsedToDate == null && meilisearchIndexService.isEnabled()) {
            try {
                MeilisearchIndexService.SearchHit hit = meilisearchIndexService.searchVisits(
                        patientName, statusFilter, activeOnly, visitDateEpochDay, page, size
                );
                List<VisitDto> dtos = loadVisitsByHits(hit.ids(), authUser);
                int totalPages = size == 0 ? 0 : (int) Math.ceil((double) hit.total() / size);
                return ApiResponse.success("Visits fetched.", dtos,
                        new PaginationDto(hit.total(), size, page, totalPages));
            } catch (MeilisearchIndexService.SearchUnavailableException e) {
                log.warn("Meilisearch unavailable for visits, falling back to DB: {}", e.getMessage());
            }
        }

        Specification<Visit> spec = (root, queryDef, builder) -> builder.conjunction();

        if (parsedVisitDate != null) {
            String rawVisitDate = input.visitDate().trim();
            if (!rawVisitDate.contains("T")) {
                LocalDateTime dayStart = parsedVisitDate.toLocalDate().atStartOfDay();
                LocalDateTime dayEnd = parsedVisitDate.toLocalDate().plusDays(1).atStartOfDay();
                spec = spec.and((root, queryDef, builder) -> builder.and(
                        builder.greaterThanOrEqualTo(root.get("visitDate"), dayStart),
                        builder.lessThan(root.get("visitDate"), dayEnd)
                ));
            } else {
                spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("visitDate"), parsedVisitDate));
            }
        } else if (parsedFromDate != null || parsedToDate != null) {
            if (parsedFromDate != null) {
                spec = spec.and((root, queryDef, builder) -> builder.greaterThanOrEqualTo(root.get("visitDate"), parsedFromDate));
            }
            if (parsedToDate != null) {
                String rawToDate = input.toDate().trim();
                if (!rawToDate.contains("T")) {
                    LocalDateTime dayEnd = parsedToDate.toLocalDate().plusDays(1).atStartOfDay();
                    spec = spec.and((root, queryDef, builder) -> builder.lessThan(root.get("visitDate"), dayEnd));
                } else {
                    spec = spec.and((root, queryDef, builder) -> builder.lessThanOrEqualTo(root.get("visitDate"), parsedToDate));
                }
            }
        } else if (input != null && input.recentDays() != null && input.recentDays() == 0) {
            // Explicit recentDays == 0 means "all historical visits, no date cutoff"
        } else if (statusFilter != null) {
            // When querying a specific status (e.g. COMPLETED), allow full historical pagination
            // unless recentDays is explicitly provided.
            if (input != null && input.recentDays() != null && input.recentDays() > 0) {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(input.recentDays()).toLocalDate().atStartOfDay();
                spec = spec.and((root, queryDef, builder) -> builder.greaterThanOrEqualTo(root.get("visitDate"), cutoff));
            }
        } else if (!activeOnly) {
            // Default rolling operational window:
            // 1. All open / in-progress visits regardless of date.
            // 2. Visits completed/finalized/edited within recentDays (default 7 days).
            int days = (input != null && input.recentDays() != null && input.recentDays() > 0) ? input.recentDays() : 7;
            LocalDateTime cutoff = LocalDateTime.now().minusDays(days).toLocalDate().atStartOfDay();
            spec = spec.and((root, queryDef, builder) -> builder.or(
                    root.get("status").in(VisitStatus.CREATED, VisitStatus.IN_PROGRESS),
                    builder.greaterThanOrEqualTo(root.get("visitDate"), cutoff),
                    builder.greaterThanOrEqualTo(root.get("updatedAt"), cutoff)
            ));
        }

        if (activeOnly) {
            spec = spec.and((root, queryDef, builder) -> root.get("status").in(VisitStatus.CREATED, VisitStatus.IN_PROGRESS));
        }

        if (statusFilter != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("status"), statusFilter));
        }

        if (patientName != null) {
            String normalized = patientName.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.like(builder.lower(root.join("patient").get("fullName")), "%" + normalized + "%")
            );
        }

        // Role-based Scoping:
        // Admin, Clinic Admin, Manager, Reception, and Nurse see all visits (hasViewAll).
        // Finance sees all visits billed or with department status BILLING.
        // Clinician sees visits in their departments with no operator, or where they are the assigned operator.
        if (authUser != null && authUser.roles() != null) {
            boolean hasViewAll = authUser.roles().contains(RoleName.ADMIN)
                    || authUser.roles().contains(RoleName.CLINIC_ADMIN)
                    || authUser.roles().contains(RoleName.MANAGER)
                    || authUser.roles().contains(RoleName.RECEPTION)
                    || authUser.roles().contains(RoleName.NURSE)
                    || (rolePermissionService != null && rolePermissionService.userHasAnyPermission(authUser, PermissionName.VISIT_VIEW_ALL));

            if (!hasViewAll) {
                Set<UUID> allowedVisitIds = new LinkedHashSet<>();
                boolean roleScoped = false;

                // 1. Finance Scoping: sees all visits billed or with department status BILLING
                if (authUser.roles().contains(RoleName.FINANCE)) {
                    roleScoped = true;
                    List<UUID> billedVisits = visitBillingRepository.findAllBilledVisitIds();
                    List<UUID> billingDeptVisits = visitDepartmentRepository.findVisitIdsWithBillingDepartment();
                    allowedVisitIds.addAll(billedVisits);
                    allowedVisitIds.addAll(billingDeptVisits);
                }

                // 2. Clinician Scoping: sees visits with departments in their specialty if no operator yet,
                //    or if assigned operator is current user
                if (authUser.roles().contains(RoleName.CLINICIAN) && authUser.userId() != null) {
                    Worker worker = workerRepository.findById(authUser.userId()).orElse(null);
                    Set<UUID> userDeptIds = (worker != null && worker.getDepartments() != null)
                            ? worker.getDepartments().stream().map(com.nexxserve.nexxclinic.entity.Department::getId).collect(Collectors.toSet())
                            : Set.of();

                    if (!userDeptIds.isEmpty()) {
                        roleScoped = true;
                        for (VisitDepartment vd : visitDepartmentRepository.findByDepartmentIdIn(userDeptIds)) {
                            if (vd.getVisit() != null && vd.getVisit().getId() != null && vd.getStatus() != VisitDepartmentStatus.CANCELLED) {
                                boolean unassigned = vd.getProcessors() == null || vd.getProcessors().isEmpty();
                                boolean isAssignedToMe = vd.getProcessors() != null && vd.getProcessors().stream().anyMatch(p -> p != null && authUser.userId().equals(p.getId()));
                                if (unassigned || isAssignedToMe) {
                                    allowedVisitIds.add(vd.getVisit().getId());
                                }
                            }
                        }
                    }
                }

                if (roleScoped) {
                    if (allowedVisitIds.isEmpty()) {
                        return ApiResponse.success("Visits fetched.", List.of(), new PaginationDto(0, size, page, 0));
                    }
                    spec = spec.and((root, queryDef, builder) -> root.get("id").in(allowedVisitIds));
                }
            }
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<VisitDto> visits = visitsToDtos(visitPage.getContent(), Set.of(), authUser);

        return ApiResponse.success("Visits fetched.", visits, new PaginationDto(visitPage.getTotalElements(), visitPage.getSize(), visitPage.getNumber(), visitPage.getTotalPages()));
    }

    private LocalDateTime parseDateTimeOrStartOfDay(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String trimmed = raw.trim();
        try {
            if (trimmed.contains("T")) {
                String clean = trimmed.endsWith("Z") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
                return LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            return LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay();
        } catch (Exception e) {
            try {
                return LocalDateTime.ofInstant(java.time.Instant.parse(trimmed), java.time.ZoneId.systemDefault());
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    @Transactional
    public ApiResponse<List<VisitDto>> getPatientHistory(UUID patientId, SearchPatientHistoryInput input, AuthenticatedUser authUser) {
        if (authUser != null && authUser.roles() != null) {
            boolean hasHistoryAccess = authUser.roles().contains(RoleName.ADMIN)
                    || authUser.roles().contains(RoleName.CLINIC_ADMIN)
                    || authUser.roles().contains(RoleName.MANAGER)
                    || authUser.roles().contains(RoleName.CLINICIAN)
                    || (rolePermissionService != null && rolePermissionService.userHasAnyPermission(authUser, PermissionName.PATIENT_HISTORY_VIEW));
            if (!hasHistoryAccess) {
                return ApiResponse.error("You do not have permission to view patient history.");
            }
        }

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
        spec = spec.and((root, queryDef, builder) ->
                builder.notEqual(root.get("status"), VisitStatus.CANCELLED));

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
        List<VisitDto> visits = visitsToDtos(visitPage.getContent(), departmentIds, authUser);

        return ApiResponse.success("Patient history fetched.", visits, new PaginationDto(visitPage.getTotalElements(), visitPage.getSize(), visitPage.getNumber(), visitPage.getTotalPages()));
    }

    // ─────────────────────────────────────────────────────────────
    //  LAST PATIENT DEPARTMENT VISIT
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ApiResponse<LastPatientDepartmentVisitDto> lastPatientDepartmentVisit(
            UUID visitId,
            UUID departmentId,
            AuthenticatedUser authUser
    ) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }
        if (departmentId == null) {
            return ApiResponse.error("departmentId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        if (!departmentRepository.existsById(departmentId)) {
            return ApiResponse.error("Department not found.");
        }

        Visit referenceVisit = visitOptional.get();
        UUID patientId = referenceVisit.getPatient() == null ? null : referenceVisit.getPatient().getId();
        if (patientId == null) {
            return ApiResponse.error("Patient not found for visit.");
        }

        List<Visit> previousVisits = visitRepository.findPreviousVisitsByPatientId(
                patientId,
                referenceVisit.getVisitDate(),
                referenceVisit.getCreatedAt(),
                referenceVisit.getId(),
                PageRequest.of(0, 1)
        );
        VisitDto previousVisitDto = previousVisits.isEmpty()
                ? null
                : visitToDto(previousVisits.get(0), Set.of(), authUser);

        List<VisitDepartment> lastDeptVisits = visitDepartmentRepository.findPreviousByPatientIdAndDepartmentId(
                patientId,
                departmentId,
                referenceVisit.getVisitDate(),
                referenceVisit.getCreatedAt(),
                referenceVisit.getId(),
                PageRequest.of(0, 1)
        );
        LastDepartmentVisitInfoDto lastDeptVisitDto = null;
        if (!lastDeptVisits.isEmpty()) {
            VisitDepartment vd = lastDeptVisits.get(0);
            lastDeptVisitDto = new LastDepartmentVisitInfoDto(
                    vd.getVisit().getId(),
                    visitDepartmentService.visitDepartmentToDto(vd)
            );
        }

        return ApiResponse.success(
                "Last patient department visit fetched.",
                new LastPatientDepartmentVisitDto(previousVisitDto, lastDeptVisitDto)
        );
    }


    // ─────────────────────────────────────────────────────────────
    //  VISIT STATUS TRANSITIONS
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<VisitDto> completeVisit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required to complete a visit.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cancelled visit cannot be completed.");
        }

        // D1/D2 fix: enforce unread-notes gate during completion (ADMIN/MANAGER exempt).
        if (!visitDepartmentNoteService.isExemptFromNotesBlock(authUser)) {
            long unreadNotes = visitDepartmentNoteRepository.countUnreadNotesForVisit(visitId, actingUser.getId());
            if (unreadNotes > 0) {
                return ApiResponse.error("You have unread notes. Please read them before completing the visit.");
            }
        }

        // D2 fix: verify a billing container exists before completion.
        boolean hasBilling = !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty();
        if (!hasBilling) {
            return ApiResponse.error("Cannot complete a visit that has not been billed. Use billVisit first.");
        }

        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        boolean hasUnbilledProducts = visitProducts.stream()
                .anyMatch(product -> product.getBillingState() != BillingState.BILLED);

        if (hasUnbilledProducts) {
            return ApiResponse.error("Cannot complete visit with unbilled products. All products must be billed first.");
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        // Requirement: don't auto-complete while any department is non-CANCELLED/non-COMPLETED.
        // This likely refers to medical/clinical status transitions.
        // In the context of completeVisit(UUID), if we find any department that is not COMPLETED or CANCELLED,
        // we should probably error or at least ensure we are not force-completing an active clinical flow.
        boolean hasDeptEditing = departments.stream()
                .anyMatch(dept -> dept.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING);
        if (hasDeptEditing) {
            String editingDepts = departments.stream()
                    .filter(dept -> dept.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING)
                    .map(dept -> dept.getDepartment() != null ? dept.getDepartment().getName() : "Unknown")
                    .collect(java.util.stream.Collectors.joining(", "));
            return ApiResponse.error(
                    "Cannot complete visit: department(s) " + editingDepts
                    + " are in billing edit mode. Complete or cancel the billing edit first."
            );
        }

        boolean hasActiveDepartments = departments.stream()
                .anyMatch(dept -> dept.getStatus() != VisitDepartmentStatus.CANCELLED
                        && dept.getStatus() != VisitDepartmentStatus.COMPLETED
                        && dept.getStatus() != VisitDepartmentStatus.BILLING); // BILLING is okay as it's at finance

        if (hasActiveDepartments) {
            return ApiResponse.error("Cannot complete visit while some departments are still active (not COMPLETED or CANCELLED).");
        }

        for (VisitDepartment dept : departments) {
            if (dept.getStatus() == VisitDepartmentStatus.BILLING) {
                dept.setStatus(VisitDepartmentStatus.COMPLETED);
                visitDepartmentRepository.save(dept);
            }
        }

        visit.setStatus(VisitStatus.COMPLETED);
        Visit saved = visitRepository.save(visit);
        clinicEventPublisher.publishVisitDischarged(saved, resolveWorker(authUser));
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

        // Both final and draft consultation saves set department status to BILLING if there are
        // unbilled products. If all products are billed or no products exist, no status change is made
        // (departments are never transitioned to COMPLETED by consultation save).
        //
        // DEPARTMENT_EDITING is owned by the billing-edit workflow
        // (startBillEditing/completeBillEditing/cancelBillEditing). Marking it BILLING
        // here would silently exit edit mode while its products are still
        // CORRECTION_PENDING, and every restore path then refuses because the
        // department is no longer DEPARTMENT_EDITING — permanently blocking invoices
        // and quick-bill. Skip departments in billing edit mode entirely.
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(input.visitId());
        List<VisitDepartment> newlyMarkedForBilling = new ArrayList<>();
        for (VisitDepartment dept : departments) {
            if (dept.getStatus() != VisitDepartmentStatus.CANCELLED
                    && dept.getStatus() != VisitDepartmentStatus.COMPLETED
                    && dept.getStatus() != VisitDepartmentStatus.DEPARTMENT_EDITING) {
                List<VisitDepartmentProduct> products = visitDepartmentProductRepository.findByVisitDepartmentId(dept.getId());
                boolean hasUnbilled = products.stream().anyMatch(p -> !p.isDeleted()
                        && p.getBillingState() != BillingState.BILLED);
                if (hasUnbilled) {
                    boolean justMarked = dept.getStatus() != VisitDepartmentStatus.BILLING;
                    dept.setStatus(VisitDepartmentStatus.BILLING);
                    VisitDepartment savedDept = visitDepartmentRepository.save(dept);
                    if (justMarked) {
                        newlyMarkedForBilling.add(savedDept);
                    }
                }
            }
        }

        Visit saved = visitRepository.save(visit);
        // Broadcast the handoff to billing: connected finance/dashboard clients refetch on
        // VISIT_DEPARTMENT_STATE_CHANGED. Without this publish, marking a department for
        // billing was silent and other sessions only saw it after a manual refresh.
        Worker actingUser = resolveWorker(authUser);
        for (VisitDepartment markedDept : newlyMarkedForBilling) {
            clinicEventPublisher.publishDepartmentStateChanged(markedDept, actingUser);
        }
        return ApiResponse.success(finalAnswer ? "Visit submitted for billing." : "Consultation answers saved.", visitToDto(saved));
    }

    @Transactional
    public ApiResponse<VisitDto> saveAnswerAndCompleteVisit(ConsultationAnswersInput input, boolean finalAnswer, AuthenticatedUser authUser) {
        return completeVisit(input, finalAnswer, authUser);
    }

    private boolean isManagerOrAboveRole(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(RoleName.ADMIN)
                || authUser.roles().contains(RoleName.CLINIC_ADMIN)
                || authUser.roles().contains(RoleName.MANAGER);
    }

    @Transactional
    public ApiResponse<VisitDto> cancelVisit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        boolean isManagerOrAbove = isManagerOrAboveRole(authUser);

        if (!isManagerOrAbove) {
            if (visit.getStatus() == VisitStatus.COMPLETED) {
                return ApiResponse.error("Completed visit cannot be cancelled.");
            }

            // Billing guard: non-managers cannot cancel a billed visit
            if (!visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
                return ApiResponse.error("Cannot cancel a billed visit. Use editBillVisit to correct the billing.");
            }

            List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
            if (!visitProducts.isEmpty()) {
                return ApiResponse.error("Cannot cancel visit with existing products. Remove all products first.");
            }
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            dept.setStatus(VisitDepartmentStatus.CANCELLED);
            visitDepartmentRepository.save(dept);
        }

        visit.setStatus(VisitStatus.CANCELLED);
        Visit saved = visitRepository.save(visit);
        meilisearchIndexService.indexVisit(saved);
        clinicEventPublisher.publishVisitCancelled(saved, null);
        return ApiResponse.success("Visit cancelled.", visitToDto(saved, null, authUser));
    }

    @Transactional
    public ApiResponse<VisitDto> cancelVisit(UUID visitId) {
        return cancelVisit(visitId, null);
    }

    /**
     * Permanently delete a visit and all its associated data.
     * Managers and Administrators can delete any visit (including billed or completed visits).
     */
    @Transactional
    public ApiResponse deleteVisit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();
        boolean isManagerOrAbove = isManagerOrAboveRole(authUser);

        if (!isManagerOrAbove) {
            if (visit.getStatus() == VisitStatus.COMPLETED) {
                return ApiResponse.error("Cannot delete a completed visit. Cancel it first.");
            }

            // Billing guard: a billed visit cannot be deleted by non-managers.
            if (!visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
                return ApiResponse.error("Cannot delete a billed visit. Use editBillVisit to correct the billing, or cancel the visit instead.");
            }
        }

        // 1. Delete all departments (and their children, notes, diagnoses, medications,
        // products, consultation answers, billing buckets, items, payments, snapshots)
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            if (dept.getParentVisitDepartment() == null) {
                visitDepartmentService.hardDeleteVisitDepartment(dept);
            }
        }

        // 2. Delete any remaining consultation answers for this visit
        try {
            List<ConsultationAnswer> remainingAnswers = consultationAnswerRepository.findByVisitIdOrderByUpdatedAtDesc(visitId);
            if (!remainingAnswers.isEmpty()) {
                consultationAnswerRepository.deleteAllInBatch(remainingAnswers);
            }
        } catch (Exception e) {
            log.warn("Could not delete remaining consultation answers for visit {}: {}", visitId, e.getMessage());
        }

        // 3. Delete any remaining billing containers for this visit
        try {
            List<VisitBilling> billings = visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId);
            if (!billings.isEmpty()) {
                visitBillingRepository.deleteAll(billings);
                visitBillingRepository.flush();
            }
        } catch (Exception e) {
            log.warn("Could not delete remaining billing containers for visit {}: {}", visitId, e.getMessage());
        }

        // 4. Delete billing versions for this visit
        try {
            List<VisitBillingVersion> versions = visitBillingVersionRepository.findByVisitIdOrderByVersionDesc(visitId);
            if (!versions.isEmpty()) {
                visitBillingVersionRepository.deleteAll(versions);
                visitBillingVersionRepository.flush();
            }
        } catch (Exception e) {
            log.warn("Could not delete billing versions for visit {}: {}", visitId, e.getMessage());
        }

        // 5. Delete price estimates for this visit
        try {
            visitPriceEstimateService.deleteEstimates(visitId);
        } catch (Exception e) {
            log.warn("Could not delete price estimates for visit {}: {}", visitId, e.getMessage());
        }

        // 6. Delete vital signs
        try {
            List<VisitVitalSignsGroup> vitalGroups = visitVitalSignsGroupRepository.findByVisitIdOrderByCreatedAtAsc(visitId);
            for (VisitVitalSignsGroup g : vitalGroups) {
                vitalMeasurementRepository.deleteByGroupId(g.getId());
                visitVitalSignsGroupRepository.delete(g);
            }
        } catch (Exception e) {
            log.warn("Could not delete vital signs for visit {}: {}", visitId, e.getMessage());
        }

        // 7. Delete linked insurances
        try {
            List<com.nexxserve.nexxclinic.entity.VisitInsurance> insurances = visitInsuranceRepository.findByVisitId(visitId);
            for (com.nexxserve.nexxclinic.entity.VisitInsurance vi : insurances) {
                visitInsuranceRepository.delete(vi);
            }
        } catch (Exception e) {
            log.warn("Could not delete linked insurances for visit {}: {}", visitId, e.getMessage());
        }

        // 8. Finally delete the visit record itself
        visitRepository.deleteById(visitId);
        meilisearchIndexService.deleteVisit(visitId);
        return ApiResponse.success("Visit deleted.", null);
    }

    @Transactional
    public ApiResponse deleteVisit(UUID visitId) {
        return deleteVisit(visitId, null);
    }

    /**
     * Finalise a visit: set all COMPLETED departments to FINALISED and the
     * visit itself to FINALISED. No further edits are allowed once finalised
     * unless an ADMIN re-opens it.
     */
    @Transactional
    public ApiResponse<VisitDto> finaliseVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();

        if (visit.getStatus() == VisitStatus.FINALISED) {
            return ApiResponse.error("Visit is already finalised.");
        }

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot finalise a cancelled visit.");
        }

        if (visit.getStatus() != VisitStatus.COMPLETED) {
            return ApiResponse.error("Only completed visits can be finalised.");
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);

        // Pre-check: all COMPLETED departments must have finalised consultation
        // answers and no unbilled products before the visit can be finalised.
        for (VisitDepartment dept : departments) {
            if (dept.getStatus() == VisitDepartmentStatus.COMPLETED) {
                // Check consultation answers
                if (dept.getAnswerId() != null) {
                    Optional<ConsultationAnswer> answerOpt = consultationAnswerRepository.findById(dept.getAnswerId());
                    if (answerOpt.isPresent()) {
                        AnswerStatus answerStatus = answerOpt.get().getStatus();
                        if (answerStatus != AnswerStatus.FINAL && answerStatus != AnswerStatus.SUBMITTED) {
                            return ApiResponse.error("Cannot finalise: department '" + dept.getDepartment().getName() + "' has draft consultation answers.");
                        }
                    } else {
                        return ApiResponse.error("Cannot finalise: department '" + dept.getDepartment().getName() + "' has a missing consultation answer record.");
                    }
                } else {
                    return ApiResponse.error("Cannot finalise: department '" + dept.getDepartment().getName() + "' has no consultation answers.");
                }

                // Check unbilled products
                List<VisitDepartmentProduct> products = visitDepartmentProductRepository.findByVisitDepartmentId(dept.getId());
                boolean hasUnbilled = products.stream().anyMatch(p ->
                        p.getBillingState() != BillingState.BILLED
                );
                if (hasUnbilled) {
                    return ApiResponse.error("Cannot finalise: department '" + dept.getDepartment().getName() + "' has unbilled products. All products must be billed first.");
                }
            }
        }

        boolean allFinalised = true;
        for (VisitDepartment dept : departments) {
            if (dept.getStatus() == VisitDepartmentStatus.COMPLETED) {
                dept.setStatus(VisitDepartmentStatus.FINALISED);
                visitDepartmentRepository.save(dept);
            }
            if (dept.getStatus() != VisitDepartmentStatus.FINALISED && dept.getStatus() != VisitDepartmentStatus.CANCELLED) {
                allFinalised = false;
            }
        }

        if (!allFinalised && !departments.isEmpty()) {
            boolean canFinaliseVisit = true;
            for (VisitDepartment dept : departments) {
                if (dept.getStatus() != VisitDepartmentStatus.FINALISED && dept.getStatus() != VisitDepartmentStatus.CANCELLED) {
                    canFinaliseVisit = false;
                    break;
                }
            }
            if (!canFinaliseVisit) {
                return ApiResponse.error("Some departments are not yet completed. Complete all departments before finalising the visit.");
            }
        }

        visit.setStatus(VisitStatus.FINALISED);
        Visit saved = visitRepository.save(visit);
        visitArchiveService.scheduleFolderCreation(saved, departments);
        meilisearchIndexService.indexVisit(saved);
        clinicEventPublisher.publishVisitDischarged(saved, null);
        return ApiResponse.success("Visit finalised.", visitToDto(saved));
    }

    /**
     * Reopen a completed visit back to PENDING. This allows adding new departments
     * but existing billed departments cannot be edited (they stay locked).
     * Only ADMIN and MANAGER roles can do this.
     */
    @Transactional
    public ApiResponse<VisitDto> reopenVisit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();

        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot reopen a cancelled visit.");
        }

        if (visit.getStatus() == VisitStatus.IN_PROGRESS) {
            return ApiResponse.error("Visit is already in progress.");
        }

        // Only ADMIN and MANAGER can reopen
        if (authUser == null || authUser.roles() == null) {
            return ApiResponse.error("Authentication is required.");
        }
        boolean authorised = authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.CLINIC_ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.MANAGER);
        if (!authorised) {
            return ApiResponse.error("Only admin or manager can reopen a visit.");
        }

        // Reopen: set visit to IN_PROGRESS, leave completed departments as-is
        // (they stay locked — billing on them is immutable unless edit mode is entered)
        visit.setStatus(VisitStatus.IN_PROGRESS);
        Visit saved = visitRepository.save(visit);
        meilisearchIndexService.indexVisit(saved);
        clinicEventPublisher.publishVisitReopened(saved, resolveWorker(authUser));
        return ApiResponse.success("Visit reopened. You can now add new departments.", visitToDto(saved));
    }

    // ─────────────────────────────────────────────────────────────
    //  VISIT INSURANCE
    // ─────────────────────────────────────────────────────────────

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
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add insurances to a cancelled visit.");
        }
        // Allow insurance changes on completed visits when any department is in DEPARTMENT_EDITING
        boolean hasEditingDept = visitDepartmentRepository.findByVisitId(visitId).stream()
            .anyMatch(d -> d.getStatus() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.DEPARTMENT_EDITING);
        if (visit.getStatus() == VisitStatus.COMPLETED && !hasEditingDept) {
            return ApiResponse.error("Cannot add insurances to a completed visit. Use startBillEditing on a department to enter billing edit mode first.");
        }

        List<UUID> uniqueIds = normalizeUniqueIds(insuranceIds);
        if (uniqueIds.isEmpty()) {
            return ApiResponse.error("insuranceIds must not be empty.");
        }

        List<PatientInsurance> patientInsurances = resolveLinkedInsurances(visit.getPatient().getId(), uniqueIds);
        if (patientInsurances == null) {
            return ApiResponse.error("Each insurance must exist, belong to the selected patient and be active.");
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
        visitPriceEstimateService.recomputeEstimates(visitId);
        Visit refreshedVisit = visitRepository.findById(visitId).orElse(visit);
        clinicEventPublisher.publishInsuranceChanged(refreshedVisit, "LINK", null, resolveWorker(authUser));
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
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot remove insurances from a cancelled visit.");
        }
        boolean hasEditingDept = visitDepartmentRepository.findByVisitId(visitId).stream()
            .anyMatch(d -> d.getStatus() == com.nexxserve.nexxclinic.model.VisitDepartmentStatus.DEPARTMENT_EDITING);
        if (visit.getStatus() == VisitStatus.COMPLETED && !hasEditingDept) {
            return ApiResponse.error("Cannot remove insurances from a completed visit. Use startBillEditing on a department to enter billing edit mode first.");
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

        // Guard: block removal of insurances that are already used in billing
        for (UUID insuranceId : uniqueIds) {
            if (departmentInsuranceBillingRepository.existsByVisitIdAndPatientInsuranceId(visitId, insuranceId)) {
                return ApiResponse.error(
                    "Cannot remove this insurance — it is already used in a bill. " +
                    "Edit the bill first, change the insurance on the billed items, then remove it."
                );
            }
        }

        visitInsuranceRepository.deleteAll(linksToRemove);
        visitPriceEstimateService.recomputeEstimates(visitId);
        Visit refreshedVisit = visitRepository.findById(visitId).orElse(visit);
        clinicEventPublisher.publishInsuranceChanged(refreshedVisit, "UNLINK", null, resolveWorker(authUser));
        return ApiResponse.success("Insurance unlinked from visit.", visitToDto(refreshedVisit));
    }

    // ─────────────────────────────────────────────────────────────
    //  VITAL SIGNS
    // ─────────────────────────────────────────────────────────────

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

        // C1: once a visit is fully billed, clinical records should be frozen to prevent
        // retrospective medical entries that aren't marked as addenda.
        boolean alreadyBilled = !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visit.getId()).isEmpty();
        if (alreadyBilled) {
            return ApiResponse.error("Cannot add vital signs to a visit that has already been billed.");
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
        clinicEventPublisher.publishVitalsChanged(visit, actingUser);
        return ApiResponse.success("Vital signs added to visit.", visitToDto(visit));
    }

    @Transactional
    public ApiResponse<VisitDto> updateVisitVitalSigns(UpdateVisitVitalSignsInput input, AuthenticatedUser authUser) {
        if (input == null || input.groupId() == null || input.vitalSigns() == null || input.vitalSigns().isEmpty()) {
            return ApiResponse.error("groupId and vitalSigns are required.");
        }

        Optional<VisitVitalSignsGroup> groupOptional = visitVitalSignsGroupRepository.findById(input.groupId());
        if (groupOptional.isEmpty()) {
            return ApiResponse.error("Vital signs group not found.");
        }

        VisitVitalSignsGroup group = groupOptional.get();
        Visit visit = group.getVisit();
        if (visit == null) {
            return ApiResponse.error("Associated visit not found.");
        }

        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot edit vital signs for a completed visit.");
        }
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot edit vital signs for a cancelled visit.");
        }

        boolean alreadyBilled = !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visit.getId()).isEmpty();
        if (alreadyBilled) {
            return ApiResponse.error("Cannot edit vital signs for a visit that has already been billed.");
        }

        if (group.getCreatedAt() != null && group.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusMinutes(10))) {
            return ApiResponse.error("Vital signs can only be edited within 10 minutes of creation.");
        }

        vitalMeasurementRepository.deleteByGroupId(group.getId());

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
        clinicEventPublisher.publishVitalsChanged(visit, resolveWorker(authUser));
        return ApiResponse.success("Vital signs updated successfully.", visitToDto(visit));
    }

    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS – department bootstrap during visit creation
    // ─────────────────────────────────────────────────────────────

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

            Optional<com.nexxserve.nexxclinic.entity.Department> departmentOptional = departmentRepository.findById(departmentInput.departmentId());
            if (departmentOptional.isEmpty()) {
                return ApiResponse.error("Department not found.");
            }

            VisitDepartment visitDepartment = new VisitDepartment();
            visitDepartment.setVisit(visit);
            visitDepartment.setDepartment(departmentOptional.get());
            visitDepartment.setAddedBy(actingUser);
            if (departmentInput.encounterType() != null) {
                visitDepartment.setEncounterType(departmentInput.encounterType());
            }
            visitDepartment.setStatus(VisitDepartmentStatus.PENDING);

            if (departmentInput.processorId() != null) {
                Optional<Worker> processorOptional = workerRepository.findById(departmentInput.processorId());
                if (processorOptional.isEmpty()) {
                    return ApiResponse.error("Processor not found.");
                }
                visitDepartment.setProcessors(java.util.List.of(processorOptional.get()));
            }

            VisitDepartment savedVisitDepartment = visitDepartmentRepository.save(visitDepartment);

            // Apply the department profile ONLY when an explicit profileId is provided
            // (profiles are never auto-applied) so its products are added as
            // source=PROFILE. Explicitly listed products are still applied afterwards
            // (duplicates are skipped by addProductsToVisitDepartment's seen-products
            // set).
            ApiResponse profileError = visitDepartmentService.applyProfileToVisitDepartment(
                    savedVisitDepartment,
                    departmentInput.profileId(),
                    actingUser
            );
            if (profileError != null) {
                return profileError;
            }

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

            Optional<com.nexxserve.nexxclinic.entity.Product> productOptional = productRepository.findById(productInput.productId());
            if (productOptional.isEmpty()) {
                return ApiResponse.error("Product not found.");
            }

            // If a department profile was applied first (applyProfileToVisitDepartment),
            // it may already have added this product as source=PROFILE. Skip it instead
            // of creating a duplicate row that violates the partial unique index.
            if (visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(
                    visitDepartment.getId(), productInput.productId()).isPresent()) {
                continue;
            }

            // S4: BILLED/EXEMPTED/CORRECTION_PENDING are managed exclusively by the
            // billing service; clients may only create products as PENDING or UNPAID.
            VisitProductStatus requestedStatus = productInput.status() == null
                    ? VisitProductStatus.PENDING
                    : productInput.status();
            if (requestedStatus == VisitProductStatus.BILLED
                    || requestedStatus == VisitProductStatus.EXEMPTED
                    || requestedStatus == VisitProductStatus.PATIENT_SHARE_EXEMPTED
                    || requestedStatus == VisitProductStatus.CORRECTION_PENDING) {
                return ApiResponse.error("Status " + requestedStatus + " cannot be set manually. Only PENDING or UNPAID can be set when adding a product.");
            }

            // SD1: if the product exists but was soft-deleted, restore it instead of
            // creating a new row. We query all rows (active + deleted) because a product
            // can be added and removed multiple times, leaving multiple soft-deleted rows;
            // using Optional here would throw NonUniqueResultException in that case.
            java.util.List<VisitDepartmentProduct> allRows = visitDepartmentProductRepository
                .findAllByVisitDepartmentIdAndProductIdIncludingDeleted(
                    visitDepartment.getId(),
                    productInput.productId()
                );

            if (allRows.stream().anyMatch(r -> !r.isDeleted())) {
                // Should have been caught by findByVisitDepartmentIdAndProductId above,
                // but handle it here for safety.
                continue;
            }

            // Pick the most-recent soft-deleted row to restore (list is ordered newest first).
            VisitDepartmentProduct item = allRows.stream()
                .filter(VisitDepartmentProduct::isDeleted)
                .findFirst()
                .orElse(null);

            if (item == null) {
                item = new VisitDepartmentProduct();
                item.setVisitDepartment(visitDepartment);
                item.setProduct(productOptional.get());
            }

            item.setDeleted(false);
            item.setQuantity(normalizeQuantity(productInput.quantity()));
            item.setStatus(requestedStatus);
            ApiResponse processorError = visitDepartmentService.assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, productInput.processorId());
            if (processorError != null) {
                return processorError;
            }
            item.setAddedBy(actingUser);
            if (item.getStatus() != VisitProductStatus.PENDING) {
                item.setBilledBy(actingUser);
            }
            try {
                // saveAndFlush (not save): the partial unique index is only checked at
                // flush time, so a plain save() would defer the violation to a later
                // query/commit and the catch below would never fire.
                visitDepartmentProductRepository.saveAndFlush(item);
            } catch (org.springframework.dao.DataIntegrityViolationException ex) {
                // Partial unique index (visit_department_id, product_id): a concurrent
                // request added the same product between the check above and this save.
                // Roll back so a failed visit creation is atomic (C1 pattern).
                log.debug("Concurrent product addition detected for visitDepartment {} and product {}: {}",
                    visitDepartment.getId(), productInput.productId(), ex.getMessage());
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

    // ─────────────────────────────────────────────────────────────
    //  DTO MAPPING
    // ─────────────────────────────────────────────────────────────

    public VisitDto visitToDto(Visit visit) {
        return visitToDto(visit, Set.of(), null);
    }

    public VisitDto visitToDto(Visit visit, Set<UUID> departmentIds) {
        return visitToDto(visit, departmentIds, null);
    }

    public VisitDto visitToDto(Visit visit, Set<UUID> departmentIds, AuthenticatedUser authUser) {
        if (visit == null) {
            return null;
        }
        if (visit.getId() != null && billingProductService != null) {
            billingProductService.syncBilledProductsToVisitDepartments(visit.getId());
        }
        List<VisitDto> list = visitsToDtos(List.of(visit), departmentIds, authUser);
        return list.isEmpty() ? null : list.get(0);
    }

    public List<VisitDto> visitsToDtos(List<Visit> visits, AuthenticatedUser authUser) {
        return visitsToDtos(visits, Set.of(), authUser);
    }

    public List<VisitDto> visitsToDtos(List<Visit> visits, Set<UUID> departmentFilterIds, AuthenticatedUser authUser) {
        if (visits == null || visits.isEmpty()) {
            return List.of();
        }

        List<UUID> visitIds = visits.stream().map(Visit::getId).filter(Objects::nonNull).toList();
        if (visitIds.isEmpty()) {
            return List.of();
        }

        Set<UUID> patientIds = visits.stream()
                .map(v -> v.getPatient() != null ? v.getPatient().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // Visit.patient is a lazy @ManyToOne. PatientInsurance's @EntityGraph only
        // covers patients that actually have an insurance row, so a patient with
        // no insurance stayed an uninitialised proxy and patientMapper.toDto()
        // initialised it one at a time — one query per distinct patient on the
        // page. Resolve them in one shot instead.
        Map<UUID, Patient> patientsById = patientIds.isEmpty()
                ? Map.of()
                : patientRepository.findAllById(patientIds).stream()
                        .filter(p -> p.getId() != null)
                        .collect(Collectors.toMap(Patient::getId, p -> p, (a, b) -> a));

        Map<UUID, List<PatientInsurance>> patientInsurancesByPatientId = patientIds.isEmpty()
                ? Map.of()
                : patientInsuranceRepository.findByPatientIdIn(patientIds).stream()
                        .filter(pi -> pi.getPatient() != null && pi.getPatient().getId() != null)
                        .collect(Collectors.groupingBy(pi -> pi.getPatient().getId()));

        List<VisitInsurance> allVisitInsurances = visitInsuranceRepository.findByVisitIdIn(visitIds);
        Map<UUID, List<VisitInsurance>> visitInsurancesByVisitId = allVisitInsurances.stream()
                .filter(vi -> vi.getVisit() != null && vi.getVisit().getId() != null)
                .collect(Collectors.groupingBy(vi -> vi.getVisit().getId()));

        Map<UUID, Set<UUID>> visitInsuranceProviderIdsByVisitId = allVisitInsurances.stream()
                .filter(vi -> vi.getVisit() != null && vi.getVisit().getId() != null
                        && vi.getPatientInsurance() != null
                        && vi.getPatientInsurance().getInsuranceProvider() != null
                        && vi.getPatientInsurance().getInsuranceProvider().getId() != null)
                .collect(Collectors.groupingBy(
                        vi -> vi.getVisit().getId(),
                        Collectors.mapping(vi -> vi.getPatientInsurance().getInsuranceProvider().getId(), Collectors.toSet())
                ));

        List<VisitDepartment> allVisitDepartments = visitDepartmentRepository.findByVisitIdIn(visitIds);
        Map<UUID, List<VisitDepartment>> allVdsByVisitId = allVisitDepartments.stream()
                .filter(vd -> vd.getVisit() != null && vd.getVisit().getId() != null)
                .collect(Collectors.groupingBy(vd -> vd.getVisit().getId()));

        Map<UUID, List<VisitDepartment>> topLevelVdsByVisitId = allVisitDepartments.stream()
                .filter(vd -> vd.getVisit() != null && vd.getVisit().getId() != null && vd.getParentVisitDepartment() == null)
                .collect(Collectors.groupingBy(vd -> vd.getVisit().getId()));

        Map<UUID, List<VisitDepartment>> childVdsByParentId = allVisitDepartments.stream()
                .filter(vd -> vd.getParentVisitDepartment() != null && vd.getParentVisitDepartment().getId() != null)
                .collect(Collectors.groupingBy(vd -> vd.getParentVisitDepartment().getId()));

        Set<UUID> allVdIds = allVisitDepartments.stream().map(VisitDepartment::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        List<Department> departmentsToLoad = allVisitDepartments.stream()
                .map(VisitDepartment::getDepartment)
                .filter(Objects::nonNull)
                .toList();
        Map<UUID, DepartmentDto> preloadedDeptDtos = visitDepartmentService.loadDepartmentDtosWithProfiles(departmentsToLoad);

        // The applied-profile DTOs, keyed by profile ID. Previously each
        // department re-fetched its own profile + product links via
        // departmentProfileToDto, which is 2 queries per department.
        Map<UUID, DepartmentProfileDto> preloadedProfileDtos = visitDepartmentService.loadProfileDtosById(
                allVisitDepartments.stream()
                        .map(VisitDepartment::getProfile)
                        .filter(Objects::nonNull)
                        .map(p -> p.getId())
                        .filter(Objects::nonNull)
                        .toList());

        Map<UUID, List<VisitDepartmentProduct>> productsByVdId = allVdIds.isEmpty()
                ? Map.of()
                : visitDepartmentProductRepository.findByVisitDepartmentIdIn(allVdIds).stream()
                        .filter(p -> p.getVisitDepartment() != null && p.getVisitDepartment().getId() != null)
                        .collect(Collectors.groupingBy(p -> p.getVisitDepartment().getId()));

        boolean canViewClinical = visitDepartmentService.canViewClinicalData(authUser);

        Map<UUID, List<VisitDepartmentDiagnosis>> diagnosesByVdId = (canViewClinical && !allVdIds.isEmpty())
                ? visitDepartmentDiagnosisRepository.findByVisitDepartmentIdIn(allVdIds).stream()
                        .filter(d -> d.getVisitDepartment() != null && d.getVisitDepartment().getId() != null)
                        .collect(Collectors.groupingBy(d -> d.getVisitDepartment().getId()))
                : Map.of();

        Map<UUID, List<VisitDepartmentMedication>> medicationsByVdId = (canViewClinical && !allVdIds.isEmpty())
                ? visitDepartmentMedicationRepository.findByVisitDepartmentIdIn(allVdIds).stream()
                        .filter(m -> m.getVisitDepartment() != null && m.getVisitDepartment().getId() != null)
                        .collect(Collectors.groupingBy(m -> m.getVisitDepartment().getId()))
                : Map.of();

        List<VisitPreInstruction> allPreInstructions = (canViewClinical && !allVdIds.isEmpty())
                ? visitPreInstructionRepository.findByVisitDepartmentIdInOrderByCreatedAtAsc(allVdIds)
                : List.of();
        Map<UUID, List<VisitPreInstruction>> preInstructionsByVdId = allPreInstructions.stream()
                .filter(pi -> pi.getVisitDepartment() != null && pi.getVisitDepartment().getId() != null)
                .collect(Collectors.groupingBy(pi -> pi.getVisitDepartment().getId()));

        Map<UUID, Long> totalNotesByVdId = allVdIds.isEmpty()
                ? Map.of()
                : visitDepartmentNoteRepository.countTotalNotesForDepartments(allVdIds).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a));

        Map<UUID, Long> newNotesByVdId = (authUser != null && authUser.userId() != null && !allVdIds.isEmpty())
                ? visitDepartmentNoteRepository.countNewNotesForDepartments(allVdIds, authUser.userId()).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a))
                : Map.of();

        Map<UUID, Long> unreadNotesByVisitId = (authUser != null && authUser.userId() != null && !visitIds.isEmpty())
                ? visitDepartmentNoteRepository.countUnreadNotesForVisits(visitIds, authUser.userId()).stream()
                        .collect(Collectors.toMap(r -> (UUID) r[0], r -> ((Number) r[1]).longValue(), (a, b) -> a))
                : Map.of();

        Set<UUID> answerIds = allVisitDepartments.stream()
                .map(VisitDepartment::getAnswerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, ConsultationAnswer> answersById = answerIds.isEmpty()
                ? Map.of()
                : consultationAnswerRepository.findAllById(answerIds).stream()
                        .collect(Collectors.toMap(ConsultationAnswer::getId, a -> a, (a, b) -> a));

        List<VisitVitalSignsGroup> allVitalGroups = visitVitalSignsGroupRepository.findByVisitIdInOrderByCreatedAtAsc(visitIds);
        Set<UUID> allGroupIds = allVitalGroups.stream().map(VisitVitalSignsGroup::getId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<UUID, List<VitalMeasurement>> measurementsByGroupId = allGroupIds.isEmpty()
                ? Map.of()
                : vitalMeasurementRepository.findByGroupIdInOrderByCreatedAtAsc(allGroupIds).stream()
                        .filter(m -> m.getGroup() != null && m.getGroup().getId() != null)
                        .collect(Collectors.groupingBy(m -> m.getGroup().getId()));

        // Every worker referenced anywhere on this page (department addedBy /
        // completedBy / processors, product operator refs, pre-instruction refs,
        // vital-sign author) in one findAllById. Without this each reference
        // initialised its own lazy proxy, and every initialised Worker dragged
        // its roles and departments collections with it.
        VisitDepartmentService.PreloadedWorkers preloadedWorkers = visitDepartmentService.loadPreloadedWorkers(
                allVisitDepartments,
                productsByVdId.values().stream().flatMap(List::stream).toList(),
                allPreInstructions,
                allVitalGroups.stream().map(VisitVitalSignsGroup::getAddedBy).filter(Objects::nonNull).toList());

        Map<UUID, List<VisitVitalSignsGroupDto>> vitalSignsByVisitId = new HashMap<>();
        for (VisitVitalSignsGroup group : allVitalGroups) {
            if (group.getVisit() == null || group.getVisit().getId() == null) continue;
            List<VitalMeasurementDto> measurementDtos = measurementsByGroupId.getOrDefault(group.getId(), List.of())
                    .stream()
                    .map(this::vitalMeasurementToDto)
                    .toList();
            VisitVitalSignsGroupDto groupDto = new VisitVitalSignsGroupDto(
                    group.getId(),
                    group.getCreatedAt(),
                    resolveWorkerDto(group.getAddedBy(), preloadedWorkers),
                    measurementDtos
            );
            vitalSignsByVisitId.computeIfAbsent(group.getVisit().getId(), k -> new ArrayList<>()).add(groupDto);
        }

        List<VisitDto> result = new ArrayList<>(visits.size());
        for (Visit visit : visits) {
            UUID visitId = visit.getId();
            Set<UUID> providerIds = visitInsuranceProviderIdsByVisitId.getOrDefault(visitId, Set.of());

            List<PatientInsuranceDto> linkedInsurances = visitInsurancesByVisitId.getOrDefault(visitId, List.of())
                    .stream()
                    .map(link -> patientInsuranceMapper.toDto(link.getPatientInsurance()))
                    .toList();

            List<VisitDepartment> targetVds;
            if (departmentFilterIds != null && !departmentFilterIds.isEmpty()) {
                targetVds = allVdsByVisitId.getOrDefault(visitId, List.of()).stream()
                        .filter(vd -> vd.getDepartment() != null && departmentFilterIds.contains(vd.getDepartment().getId()))
                        .toList();
            } else {
                targetVds = topLevelVdsByVisitId.getOrDefault(visitId, List.of());
            }

            List<VisitDepartmentDto> departments = targetVds.stream()
                    .map(vd -> mapPreloadedVisitDepartmentDto(
                            vd,
                            providerIds,
                            new LinkedHashSet<>(),
                            authUser,
                            preloadedDeptDtos,
                            preloadedProfileDtos,
                            preloadedWorkers,
                            childVdsByParentId,
                            productsByVdId,
                            diagnosesByVdId,
                            medicationsByVdId,
                            preInstructionsByVdId,
                            totalNotesByVdId,
                            newNotesByVdId,
                            answersById,
                            canViewClinical
                    ))
                    .toList();

            List<VisitVitalSignsGroupDto> vitalSigns = vitalSignsByVisitId.getOrDefault(visitId, List.of());

            // Compute quickBillEligible: <= 1 linked insurance AND 0 unread notes
            // AND <= 1 unbilled department (so quick-bill is only for simple visits)
            boolean hasOneOrFewerInsurances = linkedInsurances.size() <= 1;
            long unreadNotes = unreadNotesByVisitId.getOrDefault(visitId, 0L);
            boolean noUnreadNotes = (unreadNotes == 0);

            long unbilledDeptCount = 0;
            List<VisitDepartment> visitDepartments = allVdsByVisitId.getOrDefault(visitId, List.of());
            for (VisitDepartment dept : visitDepartments) {
                List<VisitDepartmentProduct> products = productsByVdId.getOrDefault(dept.getId(), List.of());
                boolean hasUnbilled = products.stream().anyMatch(p ->
                        p.getBillingState() != BillingState.BILLED
                );
                if (hasUnbilled) unbilledDeptCount++;
            }
            boolean hasOneOrFewerUnbilledDepts = unbilledDeptCount <= 1;
            boolean quickBillEligible = hasOneOrFewerInsurances && noUnreadNotes && hasOneOrFewerUnbilledDepts
                    && visit.getStatus() != VisitStatus.COMPLETED
                    && visit.getStatus() != VisitStatus.CANCELLED;

            // Read the initialised Patient out of the preloaded map rather than
            // off the entity: visit.getPatient() is still an uninitialised proxy,
            // and re-pointing the association via setPatient() would mark every
            // managed Visit dirty and fire an UPDATE per row on flush.
            Patient patient = visit.getPatient() == null || visit.getPatient().getId() == null
                    ? visit.getPatient()
                    : patientsById.getOrDefault(visit.getPatient().getId(), visit.getPatient());

            List<PatientInsurance> patientInsurances = patient != null && patient.getId() != null
                    ? patientInsurancesByPatientId.getOrDefault(patient.getId(), List.of())
                    : List.of();

            result.add(new VisitDto(
                    visit.getId(),
                    patientMapper.toDto(patient, patientInsurances),
                    visit.getStatus(),
                    visit.getVisitDate(),
                    linkedInsurances,
                    departments,
                    vitalSigns,
                    visit.getEstimatedTotal(),
                    visit.getEstimatedInsurancePay(),
                    visit.getEstimatedPatientPay(),
                    quickBillEligible
            ));
        }

        return result;
    }

    private VisitDepartmentDto mapPreloadedVisitDepartmentDto(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            Set<UUID> visitedDepartmentIds,
            AuthenticatedUser authUser,
            Map<UUID, DepartmentDto> preloadedDeptDtos,
            Map<UUID, DepartmentProfileDto> preloadedProfileDtos,
            VisitDepartmentService.PreloadedWorkers preloadedWorkers,
            Map<UUID, List<VisitDepartment>> childVdsByParentId,
            Map<UUID, List<VisitDepartmentProduct>> productsByVdId,
            Map<UUID, List<VisitDepartmentDiagnosis>> diagnosesByVdId,
            Map<UUID, List<VisitDepartmentMedication>> medicationsByVdId,
            Map<UUID, List<VisitPreInstruction>> preInstructionsByVdId,
            Map<UUID, Long> totalNotesByVdId,
            Map<UUID, Long> newNotesByVdId,
            Map<UUID, ConsultationAnswer> answersById,
            boolean canViewClinical
    ) {
        if (visitDepartment == null) {
            return null;
        }

        DepartmentDto departmentDto = visitDepartment.getDepartment() != null
                ? preloadedDeptDtos.get(visitDepartment.getDepartment().getId())
                : null;

        if (visitedDepartmentIds.contains(visitDepartment.getId())) {
            return new VisitDepartmentDto(
                    visitDepartment.getId(),
                    departmentDto,
                    visitDepartment.getStatus(),
                    visitDepartment.getEncounterType(),
                    visitDepartmentService.resolveProfileDto(visitDepartment.getProfile(), preloadedProfileDtos),
                    visitDepartment.getStartedAt(),
                    visitDepartment.getCompletedAt(),
                    visitDepartmentService.resolveAddedBy(visitDepartment, preloadedWorkers),
                    visitDepartmentService.resolveCompletedBy(visitDepartment, preloadedWorkers),
                    visitDepartmentService.resolveProcessors(visitDepartment, preloadedWorkers),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    new VisitDepartmentNotesSummaryDto(
                            totalNotesByVdId.getOrDefault(visitDepartment.getId(), 0L).intValue(),
                            newNotesByVdId.getOrDefault(visitDepartment.getId(), 0L).intValue()
                    ),
                    visitDepartment.getAnswerId(),
                    false,
                    false,
                    visitDepartment.getCreatedAt(),
                    visitDepartment.getUpdatedAt()
            );
        }

        visitedDepartmentIds.add(visitDepartment.getId());

        List<WorkerDto> processors = visitDepartmentService.resolveProcessors(visitDepartment, preloadedWorkers);
        WorkerDto addedBy = visitDepartmentService.resolveAddedBy(visitDepartment, preloadedWorkers);
        WorkerDto completedBy = visitDepartmentService.resolveCompletedBy(visitDepartment, preloadedWorkers);

        List<VisitDepartmentProductDto> products = productsByVdId.getOrDefault(visitDepartment.getId(), List.of())
                .stream()
                .map(item -> visitDepartmentService.visitDepartmentProductToDto(item, preloadedWorkers))
                .toList();

        List<VisitDepartmentDiagnosisDto> diagnostics = canViewClinical
                ? diagnosesByVdId.getOrDefault(visitDepartment.getId(), List.of()).stream().map(visitDepartmentService::visitDepartmentDiagnosisToDto).toList()
                : List.of();

        List<VisitDepartmentMedicationDto> medications = canViewClinical
                ? medicationsByVdId.getOrDefault(visitDepartment.getId(), List.of()).stream().map(visitDepartmentService::visitDepartmentMedicationToDto).toList()
                : List.of();

        List<VisitPreInstructionDto> preInstructions = canViewClinical
                ? preInstructionsByVdId.getOrDefault(visitDepartment.getId(), List.of()).stream().map(pi -> visitDepartmentService.visitPreInstructionToDto(pi, preloadedWorkers)).toList()
                : List.of();

        UUID answerId = canViewClinical ? visitDepartment.getAnswerId() : null;

        List<VisitDepartment> children = childVdsByParentId.getOrDefault(visitDepartment.getId(), List.of());
        List<VisitDepartmentDto> childVisitDepartments = children.stream()
                .map(child -> mapPreloadedVisitDepartmentDto(
                        child,
                        visitInsuranceProviderIds,
                        new LinkedHashSet<>(visitedDepartmentIds),
                        authUser,
                        preloadedDeptDtos,
                        preloadedProfileDtos,
                        preloadedWorkers,
                        childVdsByParentId,
                        productsByVdId,
                        diagnosesByVdId,
                        medicationsByVdId,
                        preInstructionsByVdId,
                        totalNotesByVdId,
                        newNotesByVdId,
                        answersById,
                        canViewClinical
                ))
                .toList();

        ConsultationAnswer ans = visitDepartment.getAnswerId() != null ? answersById.get(visitDepartment.getAnswerId()) : null;
        boolean hasFinalized = ans != null && (ans.getStatus() == AnswerStatus.FINAL || ans.getStatus() == AnswerStatus.SUBMITTED);

        boolean directBillable = productsByVdId.getOrDefault(visitDepartment.getId(), List.of()).stream()
                .filter(p -> !p.isDeleted())
                .anyMatch(p -> p.getBillingState() != BillingState.BILLED);
        boolean childBillable = children.stream().anyMatch(child ->
                productsByVdId.getOrDefault(child.getId(), List.of()).stream()
                        .filter(p -> !p.isDeleted())
                        .anyMatch(p -> p.getBillingState() != BillingState.BILLED)
        );
        boolean hasBillable = directBillable || childBillable;

        return new VisitDepartmentDto(
                visitDepartment.getId(),
                departmentDto,
                visitDepartment.getStatus(),
                visitDepartment.getEncounterType(),
                visitDepartmentService.resolveProfileDto(visitDepartment.getProfile(), preloadedProfileDtos),
                visitDepartment.getStartedAt(),
                visitDepartment.getCompletedAt(),
                addedBy,
                completedBy,
                processors,
                products,
                diagnostics,
                medications,
                preInstructions,
                childVisitDepartments,
                new VisitDepartmentNotesSummaryDto(
                        totalNotesByVdId.getOrDefault(visitDepartment.getId(), 0L).intValue(),
                        newNotesByVdId.getOrDefault(visitDepartment.getId(), 0L).intValue()
                ),
                answerId,
                hasFinalized,
                hasBillable,
                visitDepartment.getCreatedAt(),
                visitDepartment.getUpdatedAt()
        );
    }

    /**
     * Reuse a page-level pre-mapped worker DTO when available, so the many
     * worker references across a page of visits are mapped once each instead of
     * once per reference.
     */
    private WorkerDto resolveWorkerDto(Worker worker, VisitDepartmentService.PreloadedWorkers preloaded) {
        if (worker == null) {
            return null;
        }
        if (preloaded != null) {
            WorkerDto cached = preloaded.find(worker);
            if (cached != null) {
                return cached;
            }
        }
        return workerMapper.toDto(worker);
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

    // ─────────────────────────────────────────────────────────────
    //  SHARED UTILITIES
    // ─────────────────────────────────────────────────────────────

    public Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    public void reopenVisitIfCompleted(Visit visit) {
        // COMPLETED visits are no longer auto-reopened — use startBillEditing mutation instead.
        if (visit.getStatus() == VisitStatus.COMPLETED) {
            // No longer auto-reopen. Use startBillEditing mutation.
        }
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
            // A deactivated (soft-deleted) policy can no longer be linked to a visit.
            if (insurance.isDeactivated()) {
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

    private Set<UUID> resolveVisitInsuranceProviderIds(UUID visitId) {
        return visitInsuranceRepository.findByVisitId(visitId).stream()
                .map(link -> link.getPatientInsurance().getInsuranceProvider().getId())
                .collect(Collectors.toSet());
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

    /** Hydrates full visit DTOs from Meilisearch hit ids, preserving hit order. */
    private List<VisitDto> loadVisitsByHits(List<UUID> ids, AuthenticatedUser authUser) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Visit> byId = new HashMap<>();
        visitRepository.findAllById(ids).forEach(v -> byId.put(v.getId(), v));
        List<Visit> orderedVisits = ids.stream().map(byId::get).filter(Objects::nonNull).toList();
        return visitsToDtos(orderedVisits, Set.of(), authUser);
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

    public java.math.BigDecimal normalizeQuantity(java.math.BigDecimal value) {
        if (value == null || value.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return java.math.BigDecimal.ONE;
        }
        return value;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record DateWindow(LocalDateTime start, LocalDateTime endExclusive) {}
}
