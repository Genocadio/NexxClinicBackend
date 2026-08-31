package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitVitalSignsGroup;
import com.nexxserve.nexxclinic.entity.VitalMeasurement;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignItemInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitInput;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
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
import java.util.*;
import java.util.stream.Collectors;
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
    private final VisitBillingRepository visitBillingRepository;
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

    public VisitService(
            VisitRepository visitRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            VisitBillingRepository visitBillingRepository,
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
            ConsultationAnswerRepository consultationAnswerRepository
    ) {
        this.visitRepository = visitRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.visitBillingRepository = visitBillingRepository;
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

    @Transactional(readOnly = true)
    public ApiResponse<VisitDto> visit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        return visitOptional
                .map(visit -> ApiResponse.success("Visit fetched.", visitToDto(visit, Set.of(), authUser)))
                .orElseGet(() -> ApiResponse.error("Visit not found."));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDto>> visits(SearchVisitsInput input, AuthenticatedUser authUser) {
        int page = normalizePage(input == null ? null : input.page());
        int size = normalizeSize(input == null ? null : input.size());
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "visitDate").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        String patientName = input == null ? null : blankToNull(input.patientName());
        com.nexxserve.nexxclinic.model.VisitStatus statusFilter = input == null ? null : input.status();
        Long visitDateEpochDay = null;
        if (input != null && input.visitDate() != null) {
            visitDateEpochDay = input.visitDate().toLocalDate().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        // Meilisearch first (typo-tolerant, ranked); the DB spec below is the fallback.
        // Only invoke Meilisearch when there's a patientName search param;
        // plain status/date-only listing goes straight to the database.
        boolean hasSearchParam = patientName != null;
        if (hasSearchParam && meilisearchIndexService.isEnabled()) {
            try {
                MeilisearchIndexService.SearchHit hit = meilisearchIndexService.searchVisits(
                        patientName, statusFilter, visitDateEpochDay, page, size
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

        if (input != null && input.visitDate() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("visitDate"), input.visitDate()));
        }

        if (input != null && input.status() != null) {
            spec = spec.and((root, queryDef, builder) -> builder.equal(root.get("status"), input.status()));
        }

        if (patientName != null) {
            String normalized = patientName.toLowerCase();
            spec = spec.and((root, queryDef, builder) ->
                    builder.like(builder.lower(root.join("patient").get("fullName")), "%" + normalized + "%")
            );
        }

        Page<Visit> visitPage = visitRepository.findAll(spec, pageable);
        List<VisitDto> visits = visitPage.getContent().stream().map(visit -> visitToDto(visit, Set.of(), authUser)).toList();

        return ApiResponse.success("Visits fetched.", visits, new PaginationDto(visitPage.getTotalElements(), visitPage.getSize(), visitPage.getNumber(), visitPage.getTotalPages()));
    }

    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDto>> getPatientHistory(UUID patientId, SearchPatientHistoryInput input, AuthenticatedUser authUser) {
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
                .map(visit -> visitToDto(visit, departmentIds, authUser))
                .toList();

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

        // D1/D2 fix: enforce unread-notes gate during completion.
        long unreadNotes = visitDepartmentNoteRepository.countUnreadNotesForVisit(visitId, actingUser.getId());
        if (unreadNotes > 0) {
            return ApiResponse.error("You have unread notes. Please read them before completing the visit.");
        }

        // D2 fix: verify a billing container exists before completion.
        boolean hasBilling = !visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty();
        if (!hasBilling) {
            return ApiResponse.error("Cannot complete a visit that has not been billed. Use billVisit first.");
        }

        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        boolean hasUnbilledProducts = visitProducts.stream()
                .anyMatch(product -> product.getStatus() == VisitProductStatus.PENDING
                        || product.getStatus() == VisitProductStatus.UNPAID
                        || product.getStatus() == VisitProductStatus.CORRECTION_PENDING);

        if (hasUnbilledProducts) {
            return ApiResponse.error("Cannot complete visit with unbilled products. All products must be billed first.");
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        // Requirement: don't auto-complete while any department is non-CANCELLED/non-COMPLETED.
        // This likely refers to medical/clinical status transitions.
        // In the context of completeVisit(UUID), if we find any department that is not COMPLETED or CANCELLED,
        // we should probably error or at least ensure we are not force-completing an active clinical flow.
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

        // Only a FINAL submission hands the departments to finance (BILLING). Draft
        // saves (finalAnswer = false) must NOT freeze clinical/product work — otherwise
        // the BILLING guards in VisitDepartmentService would block clinicians from
        // adding products, diagnosis or medication mid-consultation.
        if (!finalAnswer) {
            return ApiResponse.success("Consultation answers saved.", visitToDto(visit));
        }

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

        // Billing guard: a visit that has been billed (any billing container exists)
        // cannot be cancelled — it has a financial and audit trail.
        if (!visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
            return ApiResponse.error("Cannot cancel a billed visit. Use editBillVisit to correct the billing.");
        }

        List<VisitDepartmentProduct> visitProducts = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        if (!visitProducts.isEmpty()) {
            return ApiResponse.error("Cannot cancel visit with existing products. Remove all products first.");
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            dept.setStatus(VisitDepartmentStatus.CANCELLED);
            visitDepartmentRepository.save(dept);
        }

        visit.setStatus(VisitStatus.CANCELLED);
        Visit saved = visitRepository.save(visit);
        meilisearchIndexService.indexVisit(saved);
        return ApiResponse.success("Visit cancelled.", visitToDto(saved));
    }

    /**
     * Permanently delete a visit and all its associated data.
     * Only allowed for unbilled visits that are in CREATED or CANCELLED status.
     * A billed visit cannot be deleted — it has a financial and audit trail.
     */
    @Transactional
    public ApiResponse deleteVisit(UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOptional = visitRepository.findById(visitId);
        if (visitOptional.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOptional.get();

        if (visit.getStatus() == VisitStatus.COMPLETED) {
            return ApiResponse.error("Cannot delete a completed visit. Cancel it first.");
        }

        // Billing guard: a billed visit cannot be deleted.
        if (!visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(visitId).isEmpty()) {
            return ApiResponse.error("Cannot delete a billed visit. Use editBillVisit to correct the billing, or cancel the visit instead.");
        }

        // Delete all associated data in dependency order.
        // VisitDepartmentProduct -> VisitDepartment -> VisitInsurance -> Visit
        List<VisitDepartmentProduct> products = visitDepartmentProductRepository.findByVisitDepartmentVisitId(visitId);
        for (VisitDepartmentProduct p : products) {
            visitDepartmentProductRepository.delete(p);
        }

        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);
        for (VisitDepartment dept : departments) {
            visitDepartmentRepository.delete(dept);
        }

        List<com.nexxserve.nexxclinic.entity.VisitInsurance> insurances =
            visitInsuranceRepository.findByVisitId(visitId);
        for (com.nexxserve.nexxclinic.entity.VisitInsurance vi : insurances) {
            visitInsuranceRepository.delete(vi);
        }

        visitRepository.deleteById(visitId);
        meilisearchIndexService.deleteVisit(visitId);
        return ApiResponse.success("Visit deleted.", null);
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
                        p.getStatus() == VisitProductStatus.PENDING
                        || p.getStatus() == VisitProductStatus.UNPAID
                        || p.getStatus() == VisitProductStatus.CORRECTION_PENDING
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
        meilisearchIndexService.indexVisit(saved);
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
        return ApiResponse.success("Vital signs added to visit.", visitToDto(visit));
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
        Set<UUID> visitInsuranceProviderIds = resolveVisitInsuranceProviderIds(visit.getId());
        List<PatientInsuranceDto> linkedInsurances = visitInsuranceRepository.findByVisitId(visit.getId())
                .stream()
                .map(link -> patientInsuranceMapper.toDto(link.getPatientInsurance()))
                .toList();

        List<VisitDepartment> resolvedVds = resolveVisitDepartmentsForResponse(visit.getId(), departmentIds);
        java.util.Map<UUID, com.nexxserve.nexxclinic.dto.out.DepartmentDto> preloadedDeptDtos =
                visitDepartmentService.loadDepartmentDtosWithProfiles(
                        resolvedVds.stream().map(VisitDepartment::getDepartment).toList());

        List<VisitDepartmentDto> departments = resolvedVds
                .stream()
                .map(vd -> visitDepartmentService.visitDepartmentToDto(vd, visitInsuranceProviderIds, new java.util.LinkedHashSet<>(), authUser, preloadedDeptDtos))
                .toList();

        List<VisitVitalSignsGroupDto> vitalSigns = visitVitalSignsGroupRepository.findByVisitIdOrderByCreatedAtAsc(visit.getId())
                .stream()
                .map(this::visitVitalSignsGroupToDto)
                .toList();

        // Compute quickBillEligible: ≤1 linked insurance AND 0 unread notes
        // AND ≤1 unbilled department (so quick-bill is only for simple visits)
        boolean hasOneOrFewerInsurances = linkedInsurances.size() <= 1;
        boolean noUnreadNotes = true;
        if (authUser != null && authUser.userId() != null) {
            long unreadNotes = visitDepartmentNoteRepository.countUnreadNotesForVisit(visit.getId(), authUser.userId());
            noUnreadNotes = unreadNotes == 0;
        }
        // Count departments that still have unbilled products
        List<VisitDepartment> visitDepartments = visitDepartmentRepository.findByVisitId(visit.getId());
        long unbilledDeptCount = 0;
        for (VisitDepartment dept : visitDepartments) {
            List<VisitDepartmentProduct> products = visitDepartmentProductRepository.findByVisitDepartmentId(dept.getId());
            boolean hasUnbilled = products.stream().anyMatch(p ->
                    p.getStatus() == VisitProductStatus.PENDING
                    || p.getStatus() == VisitProductStatus.UNPAID
                    || p.getStatus() == VisitProductStatus.CORRECTION_PENDING
            );
            if (hasUnbilled) unbilledDeptCount++;
        }
        boolean hasOneOrFewerUnbilledDepts = unbilledDeptCount <= 1;
        boolean quickBillEligible = hasOneOrFewerInsurances && noUnreadNotes && hasOneOrFewerUnbilledDepts
                && visit.getStatus() != VisitStatus.COMPLETED
                && visit.getStatus() != VisitStatus.CANCELLED;

        return new VisitDto(
                visit.getId(),
                patientMapper.toDto(visit.getPatient(), patientInsuranceRepository.findByPatientId(visit.getPatient().getId())),
                visit.getStatus(),
                visit.getVisitDate(),
                linkedInsurances,
                departments,
                vitalSigns,
                visit.getEstimatedTotal(),
                visit.getEstimatedInsurancePay(),
                visit.getEstimatedPatientPay(),
                quickBillEligible
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
        List<VisitDto> ordered = new ArrayList<>();
        for (UUID id : ids) {
            Visit visit = byId.get(id);
            if (visit != null) {
                ordered.add(visitToDto(visit, Set.of(), authUser));
            }
        }
        return ordered;
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
