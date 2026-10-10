package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentProfile;
import com.nexxserve.nexxclinic.entity.DepartmentProfileProduct;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.VisitDepartmentSymptom;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.entity.VisitPreInstruction;
import com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionMedication;
import com.nexxserve.nexxclinic.entity.VisitPreInstructionProductRequest;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddSymptomInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitPreInstructionsInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductStatusInput;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.mappers.out.*;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.PermissionName;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.BillingConfirmationStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.BillingState;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentProfileProductRepository;
import com.nexxserve.nexxclinic.repository.DepartmentProfileRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentSymptomRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentMedicationRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.model.AnswerStatus;
import com.nexxserve.nexxclinic.entity.ConsultationAnswer;
import com.nexxserve.nexxclinic.repository.ConsultationAnswerRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingPaymentRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
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
    private final ConsultationAnswerRepository consultationAnswerRepository;
    private final VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    private final VisitDepartmentSymptomRepository visitDepartmentSymptomRepository;
    private final VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;
    private final com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository;
    private final DepartmentRepository departmentRepository;
    private final ProductRepository productRepository;
    private final WorkerRepository workerRepository;
    private final VisitBillingItemRepository visitBillingItemRepository;
    private final com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository visitDepartmentBillingRepository;
    private final DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;
    private final VisitBillingPaymentRepository visitBillingPaymentRepository;
    private final VisitBillingRepository visitBillingRepository;
    private final VisitInsuranceRepository visitInsuranceRepository;
    private final DepartmentProfileRepository departmentProfileRepository;
    private final DepartmentProfileProductRepository departmentProfileProductRepository;

    private final WorkerMapper workerMapper;
    private final DepartmentMapper departmentMapper;
    private final ProductMapper productMapper;

    // Lazy to break circular dependency with VisitService
    private final VisitDepartmentNoteService visitDepartmentNoteService;
    private final VisitService visitService;
    private final com.nexxserve.nexxclinic.service.VisitPriceEstimateService visitPriceEstimateService;
    private final MeilisearchIndexService meilisearchIndexService;
    private final com.nexxserve.nexxclinic.repository.VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository;
    private final RolePermissionService rolePermissionService;
    private final com.nexxserve.nexxclinic.service.billing.BillingProductService billingProductService;
    private final com.nexxserve.nexxclinic.service.billing.InvoiceGenerator invoiceGenerator;
    private final com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher;

    public VisitDepartmentService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentProductRepository visitDepartmentProductRepository,
            ConsultationAnswerRepository consultationAnswerRepository,
            VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository,
            VisitDepartmentSymptomRepository visitDepartmentSymptomRepository,
            VisitDepartmentMedicationRepository visitDepartmentMedicationRepository,
            com.nexxserve.nexxclinic.repository.VisitPreInstructionRepository visitPreInstructionRepository,
            DepartmentRepository departmentRepository,
            ProductRepository productRepository,
            WorkerRepository workerRepository,
            VisitBillingItemRepository visitBillingItemRepository,
            com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository visitDepartmentBillingRepository,
            DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository,
            VisitBillingPaymentRepository visitBillingPaymentRepository,
            VisitBillingRepository visitBillingRepository,
            VisitInsuranceRepository visitInsuranceRepository,
            DepartmentProfileRepository departmentProfileRepository,
            DepartmentProfileProductRepository departmentProfileProductRepository,
            WorkerMapper workerMapper,
            DepartmentMapper departmentMapper,
            ProductMapper productMapper,
            @Lazy VisitDepartmentNoteService visitDepartmentNoteService,
            @Lazy VisitService visitService,
            @Lazy com.nexxserve.nexxclinic.service.VisitPriceEstimateService visitPriceEstimateService,
            MeilisearchIndexService meilisearchIndexService,
            com.nexxserve.nexxclinic.repository.VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository,
            @Lazy RolePermissionService rolePermissionService,
            @Lazy com.nexxserve.nexxclinic.service.billing.BillingProductService billingProductService,
            com.nexxserve.nexxclinic.service.billing.InvoiceGenerator invoiceGenerator,
            com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentProductRepository = visitDepartmentProductRepository;
        this.consultationAnswerRepository = consultationAnswerRepository;
        this.visitDepartmentDiagnosisRepository = visitDepartmentDiagnosisRepository;
        this.visitDepartmentSymptomRepository = visitDepartmentSymptomRepository;
        this.visitDepartmentMedicationRepository = visitDepartmentMedicationRepository;
        this.visitPreInstructionRepository = visitPreInstructionRepository;
        this.departmentRepository = departmentRepository;
        this.productRepository = productRepository;
        this.workerRepository = workerRepository;
        this.visitBillingItemRepository = visitBillingItemRepository;
        this.visitDepartmentBillingRepository = visitDepartmentBillingRepository;
        this.departmentInsuranceBillingRepository = departmentInsuranceBillingRepository;
        this.visitBillingPaymentRepository = visitBillingPaymentRepository;
        this.visitBillingRepository = visitBillingRepository;
        this.visitInsuranceRepository = visitInsuranceRepository;
        this.departmentProfileRepository = departmentProfileRepository;
        this.departmentProfileProductRepository = departmentProfileProductRepository;
        this.workerMapper = workerMapper;
        this.departmentMapper = departmentMapper;
        this.productMapper = productMapper;
        this.visitDepartmentNoteService = visitDepartmentNoteService;
        this.visitService = visitService;
        this.visitPriceEstimateService = visitPriceEstimateService;
        this.meilisearchIndexService = meilisearchIndexService;
        this.visitDepartmentProductSnapshotRepository = visitDepartmentProductSnapshotRepository;
        this.rolePermissionService = rolePermissionService;
        this.billingProductService = billingProductService;
        this.invoiceGenerator = invoiceGenerator;
        this.clinicEventPublisher = clinicEventPublisher;
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
        if (visit.getStatus() != VisitStatus.CREATED && visit.getStatus() != VisitStatus.IN_PROGRESS) {
            return ApiResponse.error("Departments can only be added to a visit in CREATED or IN_PROGRESS status.");
        }

        // Note: per-department edit mode (DEPARTMENT_EDITING) does not block adding
        // new departments — editing one department is independent of others.

        Optional<Department> departmentOptional = departmentRepository.findById(departmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Department not found.");
        }

        Optional<VisitDepartment> existingOpt = visitDepartmentRepository.findByVisitIdAndDepartmentId(visitId, departmentId);
        if (existingOpt.isPresent()) {
            VisitDepartment existing = existingOpt.get();
            if (existing.getStatus() == VisitDepartmentStatus.CANCELLED) {
                // Re-enable and clean previous state for the cancelled department
                Department department = existing.getDepartment();
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
                existing.setProfile(profile);
                existing.setAddedBy(resolveWorker(authUser));
                if (profile != null && profile.getEncounterType() != null) {
                    existing.setEncounterType(profile.getEncounterType());
                } else if (encounterType != null) {
                    existing.setEncounterType(encounterType);
                } else {
                    existing.setEncounterType(com.nexxserve.nexxclinic.model.EncounterType.OUTPATIENT);
                }
                existing.setStatus(VisitDepartmentStatus.PENDING);
                existing.setStartedAt(null);
                existing.setCompletedAt(null);
                existing.setCompletedBy(null);
                existing.setBillingEditSourceStatus(null);

                if (processorId != null) {
                    Optional<Worker> processorOptional = workerRepository.findById(processorId);
                    if (processorOptional.isEmpty()) {
                        return ApiResponse.error("Processor not found.");
                    }
                    addProcessorToVisitDepartment(existing, processorOptional.get());
                }

                VisitDepartment saved = visitDepartmentRepository.saveAndFlush(existing);

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

                Visit refreshed = visitRepository.findById(visitId).orElse(visit);
                return ApiResponse.success("Department re-enabled and added to visit.", visitService.visitToDto(refreshed, null, authUser));
            } else {
                return ApiResponse.error("Department is already added to this visit.");
            }
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
        visitDepartment.setAddedBy(resolveWorker(authUser));

        // When a profile is provided, derive encounterType from it.
        // When no profile, encounterType must be provided explicitly.
        if (profile != null && profile.getEncounterType() != null) {
            visitDepartment.setEncounterType(profile.getEncounterType());
        } else if (encounterType != null) {
            visitDepartment.setEncounterType(encounterType);
        } else {
            // No profile and no encounter type — default to OUTPATIENT
            visitDepartment.setEncounterType(com.nexxserve.nexxclinic.model.EncounterType.OUTPATIENT);
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
        clinicEventPublisher.publishDepartmentStateChanged(saved, resolveWorker(authUser));
        return ApiResponse.success("Department added to visit.", visitService.visitToDto(refreshed, null, authUser));
    }

    /**
     * Starts or continues a consultation for a visit department by a clinician.
     * On first call (when department is PENDING): marks department as ACTIVE,
     * transitions visit status to IN_PROGRESS if needed, and applies profile if provided.
     * On all calls: ensures the calling clinician is added to department processors.
     */
    @Transactional
    public ApiResponse<VisitDepartmentDto> consultVisit(UUID visitDepartmentId, UUID profileId, AuthenticatedUser authUser) {
        if (authUser == null || (!authUser.roles().contains(RoleName.CLINICIAN) && !authUser.roles().contains(RoleName.ADMIN))) {
            return ApiResponse.error("Only clinicians can start or continue consultations.");
        }

        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDepartment = departmentOptional.get();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot consult a cancelled department.");
        }

        Visit visit = visitDepartment.getVisit();
        if (visit != null && visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot consult a cancelled visit.");
        }

        if (profileId == null
                && visitDepartment.getProfile() == null
                && visitDepartment.getDepartment() != null
                && departmentProfileRepository.findByDepartmentId(visitDepartment.getDepartment().getId()).size() > 1) {
            return ApiResponse.error("Select a department profile before starting the consultation.");
        }

        Worker actingUser = resolveWorker(authUser);

        // 1. If profileId is provided and different from current profile, apply it
        if (profileId != null && (visitDepartment.getProfile() == null || !profileId.equals(visitDepartment.getProfile().getId()))) {
            ApiResponse<VisitDepartmentDto> profileRes = changeVisitDepartmentProfile(visitDepartmentId, profileId, authUser);
            if (profileRes.status() != ResponseStatus.SUCCESS) {
                return profileRes;
            }
            visitDepartment = visitDepartmentRepository.findById(visitDepartmentId).orElse(visitDepartment);
        }

        // 2. If status is PENDING (or not yet active), transition to ACTIVE
        if (visitDepartment.getStatus() == VisitDepartmentStatus.PENDING) {
            visitDepartment.setStatus(VisitDepartmentStatus.ACTIVE);
            if (visit != null && visit.getStatus() == VisitStatus.CREATED) {
                visit.setStatus(VisitStatus.IN_PROGRESS);
                visitRepository.save(visit);
            }
        }

        // 3. Automatically add the calling clinician to processors if not already in processors
        if (actingUser != null) {
            List<Worker> processors = visitDepartment.getProcessors();
            if (processors == null) {
                processors = new ArrayList<>();
                visitDepartment.setProcessors(processors);
            }
            final UUID actingUserId = actingUser.getId();
            boolean alreadyProcessor = processors.stream()
                    .anyMatch(p -> p != null && p.getId() != null && p.getId().equals(actingUserId));
            if (!alreadyProcessor) {
                processors.add(actingUser);
            }
        }

        VisitDepartment saved = visitDepartmentRepository.save(visitDepartment);
        UUID visitId = saved.getVisit() != null ? saved.getVisit().getId() : null;
        if (visitId != null) {
            visitPriceEstimateService.recomputeEstimates(visitId);
        }

        // recomputeEstimates() runs a @Modifying(clearAutomatically=true) bulk
        // delete, which clears the persistence context and detaches `saved`.
        // Re-fetch so the DTO mapping (notably the lazy DepartmentProfile
        // proxy) runs against managed entities. Mirrors changeVisitDepartmentProfile.
        VisitDepartment fresh = visitId != null
                ? visitDepartmentRepository.findByIdWithVisitAndDepartment(saved.getId()).orElse(saved)
                : saved;

        return ApiResponse.success(
                "Consultation active.",
                visitDepartmentToDto(fresh, resolveVisitInsuranceProviderIds(visitId), authUser)
        );
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
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot change the profile on a cancelled department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED
                || visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error(
                "Cannot change the profile on a completed or finalised department. "
                + "Ask a manager to enable billing edit mode on this department first."
            );
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING) {
            return ApiResponse.error("Cannot change the profile while the department is in billing edit mode.");
        }
        // Billing guard: profiles can only be changed BEFORE billing is done.
        // Once any billing record exists for this department, profile changes are
        // locked to prevent desynchronizing clinical data from the bill.
        if (visitDepartmentBillingRepository.existsByVisitDepartmentId(visitDepartment.getId())) {
            return ApiResponse.error("Cannot change the profile after billing has been created. Profile changes are only allowed before billing.");
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
            for (VisitDepartmentProduct p : profileProducts) {
                visitPriceEstimateService.deleteEstimateByVisitDepartmentProductId(p.getId());
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
        visitPriceEstimateService.recomputeEstimates(saved.getVisit().getId());
        UUID targetDeptId = mapped != null ? mapped.getId() : saved.getId();
        VisitDepartment freshDept = visitDepartmentRepository.findById(targetDeptId).orElse(saved);
        return ApiResponse.success(
            "Visit department profile changed.",
            visitDepartmentToDto(freshDept)
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
        if (parent.getVisit() != null && parent.getVisit().getStatus() != VisitStatus.CREATED && parent.getVisit().getStatus() != VisitStatus.IN_PROGRESS) {
            return ApiResponse.error("Child departments can only be added to a visit in CREATED or IN_PROGRESS status.");
        }
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
        child.setAddedBy(actingUser != null ? actingUser : resolveWorker(authUser));
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

            Product product = productOptional.get();
            VisitDepartmentProduct item = new VisitDepartmentProduct();
            item.setVisitDepartment(savedChild);
            item.setProduct(product);
            item.setQuantity(product.isQuantifiable()
                    ? normalizeQuantity(BigDecimal.valueOf(productInput.quantity()))
                    : BigDecimal.ONE);
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

        visitPriceEstimateService.recomputeEstimates(visit.getId());
        // recomputeEstimates() clears the persistence context (@Modifying(clearAutomatically=true)),
        // detaching `parent`. Re-fetch so DTO mapping runs against a managed entity.
        VisitDepartment freshParent = visitDepartmentRepository
                .findByIdWithVisitAndDepartment(parent.getId()).orElse(parent);
        return ApiResponse.success("Child visit department added.", visitDepartmentToDto(freshParent));
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

    @Transactional
    public ApiResponse<com.nexxserve.nexxclinic.dto.out.VisitDto> removeVisitDepartment(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment vd = departmentOptional.get();
        if (vd.getParentVisitDepartment() != null) {
            return ApiResponse.error("Use removeChildVisitDepartment for child departments.");
        }

        Visit visit = vd.getVisit();
        boolean isManagerOrAbove = isManagerOrAboveRole(authUser);

        if (isManagerOrAbove) {
            // Manager/Admin can remove any department at any time.
            // Hard-delete: cascade all dependents (billing, items, payments, snapshots,
            // products, diagnoses, medications, pre-instructions, notes, consultation answers,
            // processors, estimates) so the department row can be removed without FK violations.
            List<String> warnings = hardDeleteVisitDepartment(vd);
            if (!warnings.isEmpty()) {
                log.warn("Department {} deleted with {} warnings: {}", vd.getId(), warnings.size(), warnings);
            }
        } else {
            return ApiResponse.error("Only administrators and managers can delete visit departments.");
        }

        // Re-index the visit in Meilisearch so search results reflect the removed department.
        try {
            meilisearchIndexService.indexVisit(visit);
        } catch (Exception e) {
            log.warn("Could not re-index visit {} after department removal: {}", visit.getId(), e.getMessage());
        }

        // Reload visit from repository so fresh state without detached deleted department is used for DTO
        Visit refreshedVisit = visitRepository.findById(visit.getId()).orElse(visit);
        String message = "Department removed from visit.";
        return ApiResponse.success(message, visitService.visitToDto(refreshedVisit, null, authUser));
    }

    /**
     * Hard-delete a visit department and all its dependents in strict foreign key order.
     * Used by managers/admins who can remove departments regardless of status or billing history.
     *
     * @return warning messages about cleanup steps that failed (empty = clean delete)
     */
    public List<String> hardDeleteVisitDepartment(VisitDepartment vd) {
        UUID deptId = vd.getId();
        UUID visitId = vd.getVisit() != null ? vd.getVisit().getId() : null;
        UUID departmentId = vd.getDepartment() != null ? vd.getDepartment().getId() : null;
        List<String> warnings = new ArrayList<>();

        // 1. Delete child visit departments first (depth-first)
        List<VisitDepartment> children = visitDepartmentRepository.findByParentVisitDepartmentId(deptId);
        for (VisitDepartment child : children) {
            warnings.addAll(hardDeleteVisitDepartment(child));
        }

        // 2. Delete notes + viewers (the service handles viewer FK cleanup before deleting notes)
        try {
            visitDepartmentNoteService.deleteAllNotesForDepartment(deptId);
        } catch (Exception e) {
            log.warn("Could not delete notes for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete notes: " + e.getMessage());
        }

        // 3. Delete pre-billing price estimates for this department
        try {
            visitPriceEstimateService.deleteEstimatesByVisitDepartmentId(deptId);
        } catch (Exception e) {
            log.warn("Could not delete price estimates for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete price estimates: " + e.getMessage());
        }

        // 4. Delete billing records associated with this department in reverse FK order:
        //    Payments -> Billing Items -> Department Insurance Billings -> Department Billings -> Empty Billing Containers
        try {
            List<VisitDepartmentBilling> deptBillings = visitDepartmentBillingRepository.findByVisitDepartmentId(deptId);
            Set<VisitBilling> affectedContainers = new HashSet<>();

            for (VisitDepartmentBilling db : deptBillings) {
                if (db.getVisitBilling() != null) {
                    affectedContainers.add(db.getVisitBilling());
                }

                // Delete payments directly tied to this department billing
                List<VisitBillingPayment> payments = visitBillingPaymentRepository.findByVisitDepartmentBillingId(db.getId());
                if (!payments.isEmpty()) {
                    visitBillingPaymentRepository.deleteAll(payments);
                    visitBillingPaymentRepository.flush();
                }

                // Delete insurance billings (buckets) and their items & payments
                List<DepartmentInsuranceBilling> insBillings =
                        departmentInsuranceBillingRepository.findByVisitDepartmentBillingId(db.getId());
                for (DepartmentInsuranceBilling dib : insBillings) {
                    List<VisitBillingPayment> dibPayments =
                            visitBillingPaymentRepository.findByDepartmentInsuranceBillingId(dib.getId());
                    if (!dibPayments.isEmpty()) {
                        visitBillingPaymentRepository.deleteAll(dibPayments);
                        visitBillingPaymentRepository.flush();
                    }

                    List<VisitBillingItem> items =
                            visitBillingItemRepository.findByDepartmentInsuranceBillingId(dib.getId());
                    if (!items.isEmpty()) {
                        visitBillingItemRepository.deleteAll(items);
                        visitBillingItemRepository.flush();
                    }
                }

                if (!insBillings.isEmpty()) {
                    departmentInsuranceBillingRepository.deleteAll(insBillings);
                    departmentInsuranceBillingRepository.flush();
                }
            }

            if (!deptBillings.isEmpty()) {
                visitDepartmentBillingRepository.deleteAll(deptBillings);
                visitDepartmentBillingRepository.flush();
            }

            // Also clean up any remaining VisitBillingItem pointing directly to this department's products
            List<VisitDepartmentProduct> productsForCleanup =
                    visitDepartmentProductRepository.findByVisitDepartmentIdIncludingDeleted(deptId);
            for (VisitDepartmentProduct p : productsForCleanup) {
                List<VisitBillingItem> orphanItems = visitBillingItemRepository.findByVisitDepartmentProductId(p.getId());
                if (!orphanItems.isEmpty()) {
                    visitBillingItemRepository.deleteAllInBatch(orphanItems);
                }
            }

            // Check affected VisitBilling containers: delete empty containers
            for (VisitBilling vb : affectedContainers) {
                long remainingCount = visitDepartmentBillingRepository.countByVisitBillingId(vb.getId());
                if (remainingCount == 0) {
                    visitBillingRepository.delete(vb);
                    visitBillingRepository.flush();
                }
            }
        } catch (Exception e) {
            log.warn("Could not delete billing records for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete billing records: " + e.getMessage());
        }

        // 5. Delete product snapshots (immutable billing history rows with FK to this department)
        try {
            List<VisitDepartmentProductSnapshot> snapshots =
                    visitDepartmentProductSnapshotRepository.findByVisitDepartmentId(deptId);
            if (!snapshots.isEmpty()) {
                visitDepartmentProductSnapshotRepository.deleteAllInBatch(snapshots);
            }
        } catch (Exception e) {
            log.warn("Could not delete product snapshots for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete billing snapshots: " + e.getMessage());
        }

        // 6. Batch-delete products
        try {
            List<VisitDepartmentProduct> products =
                    visitDepartmentProductRepository.findByVisitDepartmentIdIncludingDeleted(deptId);
            if (!products.isEmpty()) {
                visitDepartmentProductRepository.deleteAllInBatch(products);
            }
        } catch (Exception e) {
            log.warn("Could not delete products for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete products: " + e.getMessage());
        }

        // 7. Batch-delete diagnoses
        try {
            List<VisitDepartmentDiagnosis> diagnoses =
                    visitDepartmentDiagnosisRepository.findByVisitDepartmentId(deptId);
            if (!diagnoses.isEmpty()) {
                visitDepartmentDiagnosisRepository.deleteAllInBatch(diagnoses);
            }
        } catch (Exception e) {
            log.warn("Could not delete diagnoses for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete diagnoses: " + e.getMessage());
        }

        // 8. Batch-delete medications
        try {
            List<VisitDepartmentMedication> meds =
                    visitDepartmentMedicationRepository.findByVisitDepartmentId(deptId);
            if (!meds.isEmpty()) {
                visitDepartmentMedicationRepository.deleteAllInBatch(meds);
            }
        } catch (Exception e) {
            log.warn("Could not delete medications for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete medications: " + e.getMessage());
        }

        // 9. Batch-delete pre-instructions
        try {
            List<VisitPreInstruction> preInstructions =
                    visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(deptId);
            if (!preInstructions.isEmpty()) {
                visitPreInstructionRepository.deleteAllInBatch(preInstructions);
            }
        } catch (Exception e) {
            log.warn("Could not delete pre-instructions for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete pre-instructions: " + e.getMessage());
        }

        // 10. Delete consultation answers for this visit + department
        try {
            if (vd.getAnswerId() != null) {
                consultationAnswerRepository.deleteById(vd.getAnswerId());
            }
            if (visitId != null && departmentId != null) {
                List<ConsultationAnswer> answers =
                        consultationAnswerRepository.findByVisitIdAndDepartmentIdOrderByUpdatedAtDesc(visitId, departmentId);
                if (!answers.isEmpty()) {
                    consultationAnswerRepository.deleteAllInBatch(answers);
                }
            }
        } catch (Exception e) {
            log.warn("Could not delete consultation answers for department {}: {}", deptId, e.getMessage());
            warnings.add("Could not delete consultation answers: " + e.getMessage());
        }

        // 11. Clear the ManyToMany processors join table before deleting the department
        vd.setProcessors(new ArrayList<>());
        visitDepartmentRepository.saveAndFlush(vd);

        // 12. Finally delete the department itself and flush
        visitDepartmentRepository.delete(vd);
        visitDepartmentRepository.flush();

        // 14. Recompute price estimates for the remaining visit
        if (visitId != null) {
            try {
                visitPriceEstimateService.recomputeEstimates(visitId);
            } catch (Exception e) {
                log.warn("Could not recompute price estimates after removing department {}: {}", deptId, e.getMessage());
            }
        }

        return warnings;
    }

    private boolean isManagerOrAboveRole(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.CLINIC_ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.MANAGER);
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
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled visit.");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findByVisitIdAndDepartmentId(input.visitId(), input.departmentId());
        if (visitDepartmentOptional.isEmpty()) {
            visitDepartmentOptional = visitDepartmentRepository.findById(input.departmentId())
                    .filter(vd -> vd.getVisit() != null && vd.getVisit().getId().equals(input.visitId()));
        }
        if (visitDepartmentOptional.isEmpty()) {
            Optional<Department> deptOpt = departmentRepository.findById(input.departmentId());
            if (deptOpt.isPresent()) {
                Department dept = deptOpt.get();
                VisitDepartment existingVd = visitDepartmentRepository.findByVisitIdAndDepartmentId(visit.getId(), dept.getId())
                        .orElse(null);
                if (existingVd != null) {
                    visitDepartmentOptional = Optional.of(existingVd);
                } else {
                    VisitDepartment newVd = new VisitDepartment();
                    newVd.setVisit(visit);
                    newVd.setDepartment(dept);
                    newVd.setStatus(VisitDepartmentStatus.PENDING);
                    newVd.setAddedBy(resolveWorker(authUser));
                    VisitDepartment rootVd = visitDepartmentRepository.findByVisitIdAndParentVisitDepartmentIsNull(visit.getId())
                            .stream().findFirst().orElse(null);
                    if (dept.isSupportRequests() && rootVd != null) {
                        newVd.setParentVisitDepartment(rootVd);
                    }
                    newVd = visitDepartmentRepository.saveAndFlush(newVd);
                    visitDepartmentOptional = Optional.of(newVd);
                }
            }
        }
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Department is not linked to this visit.");
        }

        // Lock for update to prevent TOCTOU race with consultation completion
        VisitDepartment visitDepartment = visitDepartmentRepository.findByIdForUpdate(visitDepartmentOptional.get().getId())
                .orElseThrow(() -> new RuntimeException("Visit department disappeared while locking"));

        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add products to a cancelled department.");
        }

        // Block adding products to a COMPLETED/FINALISED department only if already billed and not in DEPARTMENT_EDITING mode
        VisitDepartment rootDeptForBillingCheck = visitDepartment.getParentVisitDepartment() != null
                ? visitDepartment.getParentVisitDepartment()
                : visitDepartment;
        boolean hasExistingBilling = !visitDepartmentBillingRepository.findByVisitDepartmentId(rootDeptForBillingCheck.getId()).isEmpty();
        if ((visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED
                || visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED)
                && visitDepartment.getStatus() != VisitDepartmentStatus.DEPARTMENT_EDITING
                && hasExistingBilling) {
            return ApiResponse.error("Cannot add products to a completed department. Use startBillEditing to enter billing edit mode.");
        }

        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDepartment.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
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

        // S4: BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED/CORRECTION_PENDING are managed
        // exclusively by the billing service; clients may only create products as
        // PENDING (default) or UNPAID.
        VisitProductStatus requestedStatus = input.status() == null
                ? VisitProductStatus.PENDING
                : input.status();
        if (requestedStatus == VisitProductStatus.BILLED
                || requestedStatus == VisitProductStatus.EXEMPTED
                || requestedStatus == VisitProductStatus.PATIENT_SHARE_EXEMPTED
                || requestedStatus == VisitProductStatus.CORRECTION_PENDING) {
            return ApiResponse.error("Status " + requestedStatus + " cannot be set manually. Only PENDING or UNPAID can be set when adding a product.");
        }

        // SD1: if the product exists but was soft-deleted, restore it instead of
        // creating a new row. We query all rows (active + deleted) because a product
        // can be added and removed multiple times, leaving several soft-deleted rows;
        // using Optional here would throw NonUniqueResultException in that case.
        List<VisitDepartmentProduct> allRows = visitDepartmentProductRepository
            .findAllByVisitDepartmentIdAndProductIdIncludingDeleted(
                visitDepartment.getId(),
                input.productId()
            );

        // Active row safety-net (should have been caught above, but guards concurrent paths).
        VisitDepartmentProduct activeRow = allRows.stream()
            .filter(r -> !r.isDeleted())
            .findFirst()
            .orElse(null);
        if (activeRow != null) {
            return ApiResponse.error("Product already exists for this visit department.");
        }

        // Pick the most-recent soft-deleted row to restore (list is ordered newest first).
        VisitDepartmentProduct item = allRows.stream()
            .filter(VisitDepartmentProduct::isDeleted)
            .findFirst()
            .orElse(null);

        Product product = productOptional.get();
        if (item == null) {
            item = new VisitDepartmentProduct();
            item.setVisitDepartment(visitDepartment);
            item.setProduct(product);
        }

        item.setDeleted(false);
        item.setQuantity(product.isQuantifiable()
                ? normalizeQuantity(input.quantity())
                : BigDecimal.ONE);
        item.setStatus(requestedStatus);

        Worker actingUser = resolveWorker(authUser);
        ApiResponse processorError = assignVisitDepartmentProductProcessor(visitDepartment, item, actingUser, input.processorId());
        if (processorError != null) {
            return processorError;
        }
        item.setAddedBy(actingUser);
        boolean isDepartmentDoctor = actingUser != null && actingUser.getDepartments() != null
                && actingUser.getDepartments().stream().anyMatch(d -> d.getId().equals(visitDepartment.getDepartment().getId()));
        if (!isDepartmentDoctor) {
            item.setBillingConfirmationStatus(BillingConfirmationStatus.PENDING_OPERATOR_CONFIRMATION);
        }
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
        try {
            visitPriceEstimateService.recomputeEstimates(input.visitId());
        } catch (Exception ex) {
            log.warn("Failed to recompute price estimates after adding product {} to visit {}: {}",
                input.productId(), input.visitId(), ex.getMessage());
        }
        // recomputeEstimates() clears the persistence context (@Modifying(clearAutomatically=true)),
        // detaching `item`/`visitDepartment`. Re-fetch both so the billing sync and DTO mapping
        // below run against managed entities.
        VisitDepartmentProduct freshAddedItem = visitDepartmentProductRepository.findById(item.getId()).orElse(item);
        VisitDepartment freshDept = visitDepartment.getId() != null
                ? visitDepartmentRepository.findByIdWithVisitAndDepartment(visitDepartment.getId()).orElse(visitDepartment)
                : visitDepartment;
        try {
            billingProductService.syncClinicalProductChangeToBilling(freshAddedItem);
        } catch (Exception ex) {
            log.warn("Failed to sync clinical product change to billing: {}", ex.getMessage());
        }
        clinicEventPublisher.publishProductChanged(
            freshDept,
            product.getName(),
            freshAddedItem.getQuantity() != null ? freshAddedItem.getQuantity().intValue() : 1,
            "ADD",
            actingUser
        );
        return ApiResponse.success("Product added to visit department.", visitDepartmentToDto(freshDept));
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
        VisitDepartmentStatus deptStatus = item.getVisitDepartment().getStatus();
        // Allow edits when department is in DEPARTMENT_EDITING mode
        boolean isDeptEditing = deptStatus == VisitDepartmentStatus.DEPARTMENT_EDITING;
        // D2 fix: products in a department that has been BILLED are frozen for finance.
        // Direct edits here would desync the clinical view from the bill. Use editBillVisit.
        if (!isDeptEditing && deptStatus == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(item.getVisitDepartment().getId())) {
            return ApiResponse.error("Cannot change the status of a product in a billed department. Use startBillEditing to enter edit mode first.");
        }
        // S4: BILLED/EXEMPTED/PATIENT_SHARE_EXEMPTED/CORRECTION_PENDING are managed
        // exclusively by the billing service. Externally, product status is effectively
        // read-only for those values (only PENDING and UNPAID are externally settable).
        VisitProductStatus requestedStatus = input.status();
        if (requestedStatus == VisitProductStatus.BILLED
                || requestedStatus == VisitProductStatus.EXEMPTED
                || requestedStatus == VisitProductStatus.PATIENT_SHARE_EXEMPTED
                || requestedStatus == VisitProductStatus.CORRECTION_PENDING) {
            return ApiResponse.error("Status " + requestedStatus + " can only be set by the billing service. Allowed external statuses: PENDING, UNPAID.");
        }

        // F5 fix: billedBy is only meaningful for BILLED/EXEMPTED, which are set
        // exclusively by the billing service. Flipping PENDING -> UNPAID here previously
        // stamped a bogus billedBy on an unbilled product.
        item.setStatus(requestedStatus);

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        deleteChildVisitDepartmentIfEmpty(saved.getVisitDepartment());
        UUID statusDeptId = saved.getVisitDepartment() != null ? saved.getVisitDepartment().getId() : null;
        UUID statusVisitId = saved.getVisitDepartment() != null && saved.getVisitDepartment().getVisit() != null
                ? saved.getVisitDepartment().getVisit().getId() : null;
        if (statusVisitId != null) {
            visitPriceEstimateService.recomputeEstimates(statusVisitId);
        }
        // recomputeEstimates() clears the persistence context (@Modifying(clearAutomatically=true)),
        // detaching the department. Re-fetch so DTO mapping runs against a managed entity.
        VisitDepartment statusDept = statusDeptId != null
                ? visitDepartmentRepository.findByIdWithVisitAndDepartment(statusDeptId).orElse(saved.getVisitDepartment())
                : saved.getVisitDepartment();
        return ApiResponse.success("Visit department product status updated.", visitDepartmentToDto(statusDept));
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
        if (item.getProduct() != null && !item.getProduct().isQuantifiable()) {
            return ApiResponse.error("Quantity cannot be modified for non-quantifiable products.");
        }
        VisitDepartmentStatus deptStatus = item.getVisitDepartment().getStatus();
        boolean isDeptEditing = deptStatus == VisitDepartmentStatus.DEPARTMENT_EDITING;

        // A billed line's quantity must remain aligned with its billing snapshot.
        // New unbilled lines may be adjusted normally even when their department
        // contains other billed products.
        if (!isDeptEditing
                && item.getBillingState() == BillingState.BILLED) {
            return ApiResponse.error("Cannot change the quantity of a billed or exempted product. Use startBillEditing to enter edit mode first.");
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
            UUID affectedDepartmentId = affectedDepartment != null ? affectedDepartment.getId() : null;
            UUID visitId = (affectedDepartment != null && affectedDepartment.getVisit() != null) ? affectedDepartment.getVisit().getId() : null;
            UUID productId = item.getId();
            String productName = item.getProduct() != null ? item.getProduct().getName() : "Product";

            visitDepartmentProductRepository.delete(item);
            visitDepartmentProductRepository.flush();
            visitPriceEstimateService.deleteEstimateByVisitDepartmentProductId(productId);

            if (visitId != null) {
                reopenVisitIfCompleted(visitId);
                visitPriceEstimateService.recomputeEstimates(visitId);
            }
            VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartmentId);
            UUID targetDepartmentId = mappedDepartment != null ? mappedDepartment.getId() : affectedDepartmentId;
            VisitDepartment freshDepartment = targetDepartmentId != null
                    ? visitDepartmentRepository.findByIdWithVisitAndDepartment(targetDepartmentId).orElse(affectedDepartment)
                    : affectedDepartment;
            clinicEventPublisher.publishProductChanged(
                freshDepartment,
                productName,
                0,
                "REMOVE",
                null
            );
            return ApiResponse.success("Visit department product removed.", visitDepartmentToDto(freshDepartment));
        }

        item.setQuantity(input.quantity());

        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        deleteChildVisitDepartmentIfEmpty(saved.getVisitDepartment() != null ? saved.getVisitDepartment().getId() : null);
        UUID qtyDeptId = saved.getVisitDepartment() != null ? saved.getVisitDepartment().getId() : null;
        UUID qtyVisitId = saved.getVisitDepartment() != null && saved.getVisitDepartment().getVisit() != null
                ? saved.getVisitDepartment().getVisit().getId() : null;
        String qtyProductName = saved.getProduct() != null ? saved.getProduct().getName() : "Product";
        Integer qtyQuantity = saved.getQuantity() != null ? saved.getQuantity().intValue() : 1;
        UUID qtyProductId = saved.getId();
        if (qtyVisitId != null) {
            visitPriceEstimateService.recomputeEstimates(qtyVisitId);
        }
        // recomputeEstimates() clears the persistence context (@Modifying(clearAutomatically=true)),
        // detaching `saved` and its lazy Product/VisitDepartment proxies. Re-fetch both so the
        // billing sync and DTO mapping below run against managed entities.
        VisitDepartmentProduct freshItem = qtyProductId != null
                ? visitDepartmentProductRepository.findById(qtyProductId).orElse(saved)
                : saved;
        try {
            billingProductService.syncClinicalProductChangeToBilling(freshItem);
        } catch (Exception ex) {
            log.warn("Failed to sync clinical product change to billing: {}", ex.getMessage());
        }
        VisitDepartment qtyDept = qtyDeptId != null
                ? visitDepartmentRepository.findByIdWithVisitAndDepartment(qtyDeptId).orElse(freshItem.getVisitDepartment())
                : freshItem.getVisitDepartment();
        clinicEventPublisher.publishProductChanged(
            qtyDept,
            qtyProductName,
            qtyQuantity,
            "UPDATE",
            null
        );
        return ApiResponse.success("Visit department product quantity updated.", visitDepartmentToDto(qtyDept));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeVisitDepartmentProduct(UUID visitDepartmentProductId) {
        if (visitDepartmentProductId == null) {
            return ApiResponse.error("visitDepartmentProductId is required.");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findByIdWithDepartmentAndVisit(visitDepartmentProductId);
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOptional.get();
        VisitDepartment affectedDepartment = item.getVisitDepartment();
        UUID affectedDepartmentId = affectedDepartment != null ? affectedDepartment.getId() : null;
        UUID visitId = (affectedDepartment != null && affectedDepartment.getVisit() != null) ? affectedDepartment.getVisit().getId() : null;
        VisitDepartmentStatus deptStatus = affectedDepartment != null ? affectedDepartment.getStatus() : null;
        boolean isDeptEditing = deptStatus == VisitDepartmentStatus.DEPARTMENT_EDITING;
        String productName = item.getProduct() != null ? item.getProduct().getName() : "Product";

        // Profile products are managed by the profile — they cannot be removed
        // individually. Only changeVisitDepartmentProfile can replace them.
        if (item.getSource() == VisitDepartmentProductSource.PROFILE) {
            return ApiResponse.error("Profile products cannot be removed individually. Change the visit department's profile instead.");
        }

        // A department can contain billed history and still receive new products.
        // Only the billed line itself (or a line with billing history) is frozen.
        if (!isDeptEditing && deptStatus == VisitDepartmentStatus.BILLING && affectedDepartmentId != null
                && item.getBillingState() == BillingState.BILLED) {
            return ApiResponse.error("Cannot remove a product from a billed department. Use startBillEditing to enter edit mode first.");
        }

        // B5: never hard-delete a product that has billing history. If a billing item
        // references this row, a hard delete would leave a dangling FK in
        // visit_billing_items and corrupt invoice generation. Use editBillVisit.
        List<VisitBillingItem> billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(item.getId());
        if (!billingItems.isEmpty()) {
            return ApiResponse.error("Cannot remove a product that has billing history. Use editBillVisit to correct the billing instead.");
        }

        visitDepartmentProductRepository.delete(item);
        visitDepartmentProductRepository.flush();
        visitPriceEstimateService.deleteEstimateByVisitDepartmentProductId(visitDepartmentProductId);

        if (visitId != null) {
            reopenVisitIfCompleted(visitId);
            visitPriceEstimateService.recomputeEstimates(visitId);
        }

        VisitDepartment mappedDepartment = deleteChildVisitDepartmentIfEmpty(affectedDepartmentId);
        UUID targetDepartmentId = mappedDepartment != null ? mappedDepartment.getId() : affectedDepartmentId;
        VisitDepartment freshDepartment = targetDepartmentId != null
                ? visitDepartmentRepository.findByIdWithVisitAndDepartment(targetDepartmentId).orElse(affectedDepartment)
                : affectedDepartment;
        clinicEventPublisher.publishProductChanged(
            freshDepartment,
            productName,
            0,
            "REMOVE",
            null
        );
        return ApiResponse.success("Visit department product removed.", visitDepartmentToDto(freshDepartment));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> confirmVisitDepartmentProduct(UUID visitDepartmentProductId, AuthenticatedUser authUser) {
        if (visitDepartmentProductId == null) {
            return ApiResponse.error("visitDepartmentProductId is required.");
        }

        Optional<VisitDepartmentProduct> itemOptional = visitDepartmentProductRepository.findById(visitDepartmentProductId);
        if (itemOptional.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOptional.get();
        Worker actingUser = authUser != null && authUser.userId() != null
                ? workerRepository.findById(authUser.userId()).orElse(null)
                : null;

        item.setConfirmedBy(actingUser);
        item.setBillingConfirmationStatus(com.nexxserve.nexxclinic.model.BillingConfirmationStatus.CONFIRMED);
        VisitDepartmentProduct saved = visitDepartmentProductRepository.save(item);
        clinicEventPublisher.publishProductChanged(
            saved.getVisitDepartment(),
            saved.getProduct() != null ? saved.getProduct().getName() : "Product",
            saved.getQuantity() != null ? saved.getQuantity().intValue() : 1,
            "CONFIRM",
            actingUser
        );

        return ApiResponse.success("Product confirmed by operator.", visitDepartmentToDto(item.getVisitDepartment()));
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
        // D1 follow-up: a department in billing edit mode must only be exited through
        // completeBillEditing/cancelBillEditing (which restore its CORRECTION_PENDING
        // products). Allowing the generic mutation to move it — e.g. to BILLING —
        // silently strands those products and permanently blocks invoice generation.
        if (currentStatus == VisitDepartmentStatus.DEPARTMENT_EDITING) {
            return ApiResponse.error(
                "Department is in billing edit mode. Use completeBillEditing or cancelBillEditing to exit edit mode first."
            );
        }
        // FINANCE may only move a department to a terminal state (COMPLETED/CANCELLED).
        // Clinical/administrative roles keep the full transition set.
        boolean reopeningBilledDepartment = currentStatus == VisitDepartmentStatus.BILLING
                && requestedStatus == VisitDepartmentStatus.ACTIVE;
        boolean openingCompletedDepartment = (currentStatus == VisitDepartmentStatus.COMPLETED
                || currentStatus == VisitDepartmentStatus.FINALISED)
                && requestedStatus == VisitDepartmentStatus.ACTIVE;
        if (openingCompletedDepartment && !hasAdministrativeBillingRole(authUser)) {
            return ApiResponse.error("Only finance, manager, or administrator users can open a completed visit.");
        }
        if (isFinanceOnlyRole(authUser)
                && requestedStatus != VisitDepartmentStatus.COMPLETED
                && requestedStatus != VisitDepartmentStatus.CANCELLED
                && !reopeningBilledDepartment
                && !openingCompletedDepartment) {
            return ApiResponse.error("FINANCE can only mark a department COMPLETED or CANCELLED.");
        }
        if (currentStatus == VisitDepartmentStatus.COMPLETED) {
            if (!openingCompletedDepartment) {
                return ApiResponse.error("A COMPLETED department is terminal and cannot be changed.");
            }
        }
        if (currentStatus == VisitDepartmentStatus.CANCELLED) {
            Visit visit = department.getVisit();
            if (visit != null && (visit.getStatus() == VisitStatus.CANCELLED
                    || visit.getStatus() == VisitStatus.COMPLETED
                    || visit.getStatus() == VisitStatus.FINALISED)) {
                return ApiResponse.error("Cannot re-enable a department on a " + visit.getStatus() + " visit.");
            }
            if (openingCompletedDepartment) {
                department.setCompletedAt(null);
                department.setCompletedBy(null);
                department.setBillingEditSourceStatus(null);
            }
            boolean canUncancel = authUser != null && authUser.roles() != null
                    && authUser.roles().stream().anyMatch(r ->
                        r == RoleName.RECEPTION || r == RoleName.ADMIN || r == RoleName.CLINIC_ADMIN
                                || r == RoleName.MANAGER || r == RoleName.NURSE || r == RoleName.CLINICIAN);
            if (!canUncancel) {
                return ApiResponse.error("You do not have permission to re-enable this department.");
            }
            if (requestedStatus != VisitDepartmentStatus.PENDING && requestedStatus != VisitDepartmentStatus.ACTIVE) {
                return ApiResponse.error("A cancelled department can only be re-enabled to PENDING or ACTIVE status.");
            }
            // Clean previous state
            department.setCompletedAt(null);
            department.setCompletedBy(null);
            department.setBillingEditSourceStatus(null);
            if (requestedStatus == VisitDepartmentStatus.PENDING) {
                department.setStartedAt(null);
            }
        }
        if (currentStatus == VisitDepartmentStatus.BILLING
                && requestedStatus != VisitDepartmentStatus.COMPLETED
                && requestedStatus != VisitDepartmentStatus.ACTIVE
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(department.getId())) {
            return ApiResponse.error("A department that has been billed is frozen. Use editBillVisit to correct the billing.");
        }
        if (requestedStatus == VisitDepartmentStatus.COMPLETED) {
            boolean isFinanceOrAdminOrManager = authUser != null && authUser.roles() != null
                    && authUser.roles().stream().anyMatch(r ->
                        r == RoleName.FINANCE || r == RoleName.ADMIN || r == RoleName.CLINIC_ADMIN || r == RoleName.MANAGER);
            if (!isFinanceOrAdminOrManager) {
                return ApiResponse.error("Doctors and clinical staff cannot set department status to completed.");
            }
            List<VisitDepartmentProduct> products = visitDepartmentProductRepository
                .findByVisitDepartmentId(department.getId());
            boolean hasUnbilled = products.stream().anyMatch(p -> !p.isDeleted()
                    && p.getBillingState() != BillingState.BILLED);
            if (hasUnbilled) {
                return ApiResponse.error("Cannot complete a department with unbilled products. All products must be billed first.");
            }
        }
        if (requestedStatus == VisitDepartmentStatus.CANCELLED) {
            boolean isManagerOrAbove = isManagerOrAboveRole(authUser);
            boolean isAssignedProcessor = authUser != null && authUser.userId() != null
                    && department.getProcessors() != null
                    && department.getProcessors().stream().anyMatch(p -> p != null && authUser.userId().equals(p.getId()));
            boolean isReceptionOrFinance = authUser != null && authUser.roles() != null
                    && authUser.roles().stream().anyMatch(r ->
                        r == RoleName.RECEPTION || r == RoleName.FINANCE || r == RoleName.ADMIN || r == RoleName.CLINIC_ADMIN || r == RoleName.MANAGER || r == RoleName.NURSE || r == RoleName.CLINICIAN);

            List<VisitDepartmentProduct> products = visitDepartmentProductRepository
                    .findByVisitDepartmentIdIncludingDeleted(department.getId());
            if (!products.isEmpty() && !isManagerOrAbove && !isAssignedProcessor) {
                return ApiResponse.error("Only the assigned processor or an admin/manager can cancel a department with products.");
            }
            if (products.isEmpty() && !isManagerOrAbove && !isAssignedProcessor && !isReceptionOrFinance) {
                return ApiResponse.error("You do not have permission to cancel this department.");
            }
        }
        department.setStatus(requestedStatus);
        if (requestedStatus == VisitDepartmentStatus.COMPLETED || requestedStatus == VisitDepartmentStatus.FINALISED) {
            department.setCompletedAt(java.time.LocalDateTime.now());
            Worker actingUser = resolveWorker(authUser);
            if (actingUser != null) {
                department.setCompletedBy(actingUser);
            }
        }

        if (requestedStatus == VisitDepartmentStatus.ACTIVE) {
            Worker actingUser = resolveWorker(authUser);
            if (actingUser != null) {
                addProcessorToVisitDepartment(department, actingUser);
            }
        }

        VisitDepartment saved = visitDepartmentRepository.save(department);
        clinicEventPublisher.publishDepartmentStateChanged(saved, resolveWorker(authUser));
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

        boolean elevated = authUser != null && authUser.roles() != null
                && authUser.roles().stream().anyMatch(role ->
                        role == RoleName.ADMIN
                                || role == RoleName.CLINIC_ADMIN
                                || role == RoleName.MANAGER);
        Worker actingUser = resolveWorker(authUser);
        boolean isCurrentProcessor = actingUser != null
                && department.getProcessors() != null
                && department.getProcessors().stream().anyMatch(worker ->
                        worker != null && actingUser.getId().equals(worker.getId()));
        if (!elevated && !isCurrentProcessor) {
            return ApiResponse.error("Only an existing processor or manager can add a processor.");
        }

        Optional<Worker> processorOptional = workerRepository.findById(processorId);
        if (processorOptional.isEmpty()) {
            return ApiResponse.error("Processor not found.");
        }

        Worker processor = processorOptional.get();
        boolean clinician = processor.getRoles() != null && processor.getRoles().contains(RoleName.CLINICIAN);
        if (!clinician) {
            return ApiResponse.error("The selected processor must be an active clinician.");
        }

        if (!processor.isActive()) {
            return ApiResponse.error("The selected processor is not active.");
        }

        addProcessorToVisitDepartment(department, processor);
        VisitDepartment saved = visitDepartmentRepository.save(department);
        clinicEventPublisher.publishDepartmentStateChanged(saved, resolveWorker(authUser));
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

    /**
     * Change (or clear) the processor assigned to an existing visit-department product.
     *
     * <p>Ownership guard: only the worker who originally added the product
     * ({@code addedBy}) may reassign its processor, unless the caller has FINANCE
     * or ADMIN role. FINANCE/ADMIN may assign any worker; other roles may only pick
     * from workers already linked to the visit department.
     */
    @Transactional
    public ApiResponse<VisitDepartmentDto> updateVisitDepartmentProductProcessor(
            com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductProcessorInput input,
            AuthenticatedUser authUser
    ) {
        if (input == null || input.visitDepartmentProductId() == null) {
            return ApiResponse.error("visitDepartmentProductId is required.");
        }

        Optional<VisitDepartmentProduct> itemOpt = visitDepartmentProductRepository.findById(input.visitDepartmentProductId());
        if (itemOpt.isEmpty()) {
            return ApiResponse.error("Visit department product not found.");
        }

        VisitDepartmentProduct item = itemOpt.get();
        VisitDepartment visitDepartment = item.getVisitDepartment();

        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot change processor on a cancelled department.");
        }

        // DEPARTMENT_EDITING mode: allow processor changes on completed/billing departments.
        boolean deptEditingMode = visitDepartment.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING;

        if ((visitDepartment.getStatus() == VisitDepartmentStatus.COMPLETED
                || visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED)
                && !deptEditingMode) {
            return ApiResponse.error("Cannot change processor on a completed department. Use startBillEditing to enter edit mode.");
        }

        // BILLING guard: once a department is billed, only FINANCE/ADMIN can change processor.
        if (visitDepartment.getStatus() == VisitDepartmentStatus.BILLING
                && visitDepartmentBillingRepository.existsByVisitDepartmentId(visitDepartment.getId())
                && !deptEditingMode) {
            if (!hasFinanceOrAdminRole(authUser)) {
                return ApiResponse.error("Cannot change processor on a billed department. Only FINANCE or ADMIN can do this.");
            }
        }

        Worker actingUser = resolveWorker(authUser);
        boolean isFinanceOrAdmin = hasFinanceOrAdminRole(authUser);

        // Ownership guard: only the worker who added the product can change its processor,
        // unless the caller is FINANCE or ADMIN.
        if (!isFinanceOrAdmin && actingUser != null && item.getAddedBy() != null
                && !actingUser.getId().equals(item.getAddedBy().getId())) {
            return ApiResponse.error("Only the worker who added this product can change its processor.");
        }

        // Resolve the new processor (null means clear).
        Worker newProcessor = null;
        if (input.processorId() != null) {
            // FINANCE/ADMIN may assign any worker; others must pick from the department's processors.
            if (isFinanceOrAdmin) {
                Optional<Worker> workerOpt = workerRepository.findById(input.processorId());
                if (workerOpt.isEmpty()) {
                    return ApiResponse.error("Processor worker not found.");
                }
                newProcessor = workerOpt.get();
            } else {
                List<Worker> deptProcessors = normalizeVisitDepartmentProcessors(visitDepartment);
                newProcessor = findProcessorById(deptProcessors, input.processorId());
                if (newProcessor == null) {
                    return ApiResponse.error("processorId must belong to the visit department processors.");
                }
            }
        }

        item.setProcessor(newProcessor);
        visitDepartmentProductRepository.save(item);
        return ApiResponse.success("Product processor updated.", visitDepartmentToDto(visitDepartment));
    }

    private boolean hasFinanceOrAdminRole(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.FINANCE)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN);
    }

    private boolean hasAdministrativeBillingRole(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.FINANCE)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.CLINIC_ADMIN)
                || authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.MANAGER);
    }

    /**
     * True when the caller's only billable-for-status role is FINANCE (no ADMIN,
     * CLINIC_ADMIN, RECEPTION, NURSE or CLINICIAN). Used to restrict FINANCE to the
     * terminal transitions (COMPLETED/CANCELLED) in updateVisitDepartmentStatus.
     */
    private boolean isFinanceOnlyRole(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        if (!authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.FINANCE)) {
            return false;
        }
        return !authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.ADMIN)
                && !authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.CLINIC_ADMIN)
                && !authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.RECEPTION)
                && !authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.NURSE)
                && !authUser.roles().contains(com.nexxserve.nexxclinic.model.RoleName.CLINICIAN);
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
        return addDiagnosisToVisitDepartment(input, null);
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addDiagnosisToVisitDepartment(AddDiagnosisInput input, AuthenticatedUser authUser) {
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
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add diagnostics to a cancelled department.");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot add diagnostics to a finalised department.");
        }

        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDept.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }

        VisitDepartmentDiagnosis diagnosis = new VisitDepartmentDiagnosis();
        diagnosis.setVisitDepartment(visitDept);
        diagnosis.setDiagnosisName(input.diagnosisName().trim());
        diagnosis.setIcd11Code(input.icd11Code() == null || input.icd11Code().isBlank() ? null : input.icd11Code().trim());
        diagnosis.setType(input.type());
        diagnosis.setNotes(input.notes());

        visitDepartmentDiagnosisRepository.save(diagnosis);
        return ApiResponse.success("Diagnosis added successfully.", visitDepartmentToDto(visitDept));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateDiagnosisNotes(
            UUID diagnosisId,
            String notes,
            AuthenticatedUser authUser
    ) {
        if (diagnosisId == null) {
            return ApiResponse.error("diagnosisId is required.");
        }
        Optional<VisitDepartmentDiagnosis> diagnosisOptional =
                visitDepartmentDiagnosisRepository.findById(diagnosisId);
        if (diagnosisOptional.isEmpty()) {
            return ApiResponse.error("Diagnosis not found.");
        }

        VisitDepartmentDiagnosis diagnosis = diagnosisOptional.get();
        VisitDepartment visitDepartment = diagnosis.getVisitDepartment();
        if (visitDepartment == null) {
            return ApiResponse.error("Diagnosis visit department not found.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot update diagnostics in a cancelled department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot update diagnostics in a finalised department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDepartment.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }

        diagnosis.setNotes(notes);
        visitDepartmentDiagnosisRepository.save(diagnosis);
        return ApiResponse.success(
                "Diagnosis notes updated successfully.",
                visitDepartmentToDto(visitDepartment)
        );
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeDiagnosisFromVisitDepartment(
            UUID diagnosisId,
            AuthenticatedUser authUser
    ) {
        if (diagnosisId == null) {
            return ApiResponse.error("diagnosisId is required.");
        }

        Optional<VisitDepartmentDiagnosis> diagnosisOptional =
                visitDepartmentDiagnosisRepository.findById(diagnosisId);
        if (diagnosisOptional.isEmpty()) {
            return ApiResponse.error("Diagnosis not found.");
        }

        VisitDepartmentDiagnosis diagnosis = diagnosisOptional.get();
        VisitDepartment visitDepartment = diagnosis.getVisitDepartment();
        if (visitDepartment == null) {
            return ApiResponse.error("Diagnosis visit department not found.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot remove diagnostics from a cancelled department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot remove diagnostics from a finalised department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDepartment.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }

        visitDepartmentDiagnosisRepository.delete(diagnosis);
        visitDepartmentDiagnosisRepository.flush();
        return ApiResponse.success(
                "Diagnosis removed successfully.",
                visitDepartmentToDto(visitDepartment)
        );
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addSymptomToVisitDepartment(
            AddSymptomInput input, AuthenticatedUser authUser) {
        if (input == null || input.visitDepartmentId() == null
                || input.symptomName() == null || input.symptomName().isBlank()) {
            return ApiResponse.error("visitDepartmentId and symptomName are required.");
        }
        UUID visitDeptId;
        try {
            visitDeptId = UUID.fromString(input.visitDepartmentId());
        } catch (IllegalArgumentException e) {
            return ApiResponse.error("Invalid visitDepartmentId format.");
        }
        Optional<VisitDepartment> departmentOptional = visitDepartmentRepository.findById(visitDeptId);
        if (departmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }
        VisitDepartment department = departmentOptional.get();
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED
                || department.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot add symptoms to a "
                    + department.getStatus().name().toLowerCase() + " department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(department.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }
        VisitDepartmentSymptom symptom = new VisitDepartmentSymptom();
        symptom.setVisitDepartment(department);
        symptom.setSymptomName(input.symptomName().trim());
        symptom.setSonomedId(input.sonomedId() == null || input.sonomedId().isBlank()
                ? null : input.sonomedId().trim());
        symptom.setNotes(input.notes());
        visitDepartmentSymptomRepository.save(symptom);
        return ApiResponse.success("Symptom added successfully.", visitDepartmentToDto(department));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateSymptomNotes(
            UUID symptomId, String notes, AuthenticatedUser authUser) {
        if (symptomId == null) return ApiResponse.error("symptomId is required.");
        Optional<VisitDepartmentSymptom> optional = visitDepartmentSymptomRepository.findById(symptomId);
        if (optional.isEmpty()) return ApiResponse.error("Symptom not found.");
        VisitDepartmentSymptom symptom = optional.get();
        VisitDepartment department = symptom.getVisitDepartment();
        if (department == null) return ApiResponse.error("Symptom visit department not found.");
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED
                || department.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot update symptoms in a "
                    + department.getStatus().name().toLowerCase() + " department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(department.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }
        symptom.setNotes(notes);
        visitDepartmentSymptomRepository.save(symptom);
        return ApiResponse.success("Symptom notes updated successfully.", visitDepartmentToDto(department));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeSymptomFromVisitDepartment(
            UUID symptomId, AuthenticatedUser authUser) {
        if (symptomId == null) return ApiResponse.error("symptomId is required.");
        Optional<VisitDepartmentSymptom> optional = visitDepartmentSymptomRepository.findById(symptomId);
        if (optional.isEmpty()) return ApiResponse.error("Symptom not found.");
        VisitDepartmentSymptom symptom = optional.get();
        VisitDepartment department = symptom.getVisitDepartment();
        if (department == null) return ApiResponse.error("Symptom visit department not found.");
        if (department.getStatus() == VisitDepartmentStatus.CANCELLED
                || department.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot remove symptoms from a "
                    + department.getStatus().name().toLowerCase() + " department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(department.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }
        visitDepartmentSymptomRepository.delete(symptom);
        visitDepartmentSymptomRepository.flush();
        return ApiResponse.success("Symptom removed successfully.", visitDepartmentToDto(department));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addMedicationToVisitDepartment(AddMedicationInput input) {
        return addMedicationToVisitDepartment(input, null);
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> addMedicationToVisitDepartment(AddMedicationInput input, AuthenticatedUser authUser) {
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
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add medication to a cancelled department.");
        }
        if (visitDept.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot add medication to a finalised department.");
        }

        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDept.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }

        VisitDepartmentMedication medication = new VisitDepartmentMedication();
        medication.setVisitDepartment(visitDept);
        medication.setMedicationName(input.medicationName().trim());
        medication.setInstructions(input.instructions().trim());

        visitDepartmentMedicationRepository.save(medication);
        return ApiResponse.success("Medication added successfully.", visitDepartmentToDto(visitDept));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateMedication(
            UUID medicationId, String medicationName, String instructions, AuthenticatedUser authUser) {
        if (medicationId == null || medicationName == null || medicationName.isBlank()
                || instructions == null || instructions.isBlank()) {
            return ApiResponse.error("medicationId, medicationName and instructions are required.");
        }
        Optional<VisitDepartmentMedication> medicationOptional =
                visitDepartmentMedicationRepository.findById(medicationId);
        if (medicationOptional.isEmpty()) {
            return ApiResponse.error("Medication not found.");
        }
        VisitDepartmentMedication medication = medicationOptional.get();
        VisitDepartment visitDept = medication.getVisitDepartment();
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED
                || visitDept.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot edit medication in a " + visitDept.getStatus() + " department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDept.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }
        medication.setMedicationName(medicationName.trim());
        medication.setInstructions(instructions.trim());
        visitDepartmentMedicationRepository.save(medication);
        return ApiResponse.success("Medication updated successfully.", visitDepartmentToDto(visitDept));
    }

    @Transactional
    public ApiResponse<VisitDepartmentDto> removeMedication(UUID medicationId, AuthenticatedUser authUser) {
        if (medicationId == null) {
            return ApiResponse.error("medicationId is required.");
        }
        Optional<VisitDepartmentMedication> medicationOptional =
                visitDepartmentMedicationRepository.findById(medicationId);
        if (medicationOptional.isEmpty()) {
            return ApiResponse.error("Medication not found.");
        }
        VisitDepartmentMedication medication = medicationOptional.get();
        VisitDepartment visitDept = medication.getVisitDepartment();
        if (visitDept.getStatus() == VisitDepartmentStatus.CANCELLED
                || visitDept.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot remove medication in a " + visitDept.getStatus() + " department.");
        }
        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDept.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
        }
        visitDepartmentMedicationRepository.delete(medication);
        return ApiResponse.success("Medication removed successfully.", visitDepartmentToDto(visitDept));
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
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add pre-instructions to a cancelled department.");
        }
        if (visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot add pre-instructions to a finalised department.");
        }

        if (visitDepartmentNoteService.hasUnreadDepartmentNotes(visitDepartment.getId(), authUser)) {
            return ApiResponse.error("You have unread department notes. Please read them before continuing consultation.");
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
        if (visitDepartment == null) {
            return null;
        }
        UUID visitId = null;
        try {
            if (visitDepartment.getVisit() != null) {
                visitId = visitDepartment.getVisit().getId();
            }
        } catch (Exception e) {
            // lazy proxy without session
        }
        return visitDepartmentToDto(
                visitDepartment,
                resolveVisitInsuranceProviderIds(visitId),
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
        return visitDepartmentToDto(visitDepartment, visitInsuranceProviderIds, visitedDepartmentIds, authUser, null);
    }

    /**
     * Overload that accepts pre-loaded department DTOs to avoid N+1 queries
     * when building DTOs for multiple departments (e.g. visitToDto).
     * Pass {@code null} to fall back to per-department loading.
     */
    public VisitDepartmentDto visitDepartmentToDto(
            VisitDepartment visitDepartment,
            Set<UUID> visitInsuranceProviderIds,
            Set<UUID> visitedDepartmentIds,
            AuthenticatedUser authUser,
            java.util.Map<UUID, DepartmentDto> preloadedDepartmentDtos
    ) {
        if (visitDepartment == null) {
            return null;
        }

        // Resolve department DTO: use pre-loaded map if available, else load individually
        final DepartmentDto departmentDto;
        if (preloadedDepartmentDtos != null
                && preloadedDepartmentDtos.containsKey(visitDepartment.getDepartment().getId())) {
            departmentDto = preloadedDepartmentDtos.get(visitDepartment.getDepartment().getId());
        } else {
            departmentDto = departmentToDtoWithProfiles(visitDepartment.getDepartment());
        }

        if (visitedDepartmentIds.contains(visitDepartment.getId())) {
            // Cycle guard: limited data, but still compute finalization readiness
            boolean hasFinalizedAnswersCycleGuard = resolveHasFinalizedConsultationAnswers(visitDepartment);
            boolean hasBillableCycleGuard = resolveHasBillableProducts(visitDepartment.getId());
            return new VisitDepartmentDto(
                    visitDepartment.getId(),
                    departmentDto,
                    visitDepartment.getStatus(),
                    visitDepartment.getEncounterType(),
                    departmentProfileToDto(visitDepartment.getProfile()),
                    visitDepartment.getStartedAt(),
                    visitDepartment.getCompletedAt(),
                    resolveAddedBy(visitDepartment),
                    resolveCompletedBy(visitDepartment),
                    resolveProcessors(visitDepartment),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    visitDepartmentNoteService.buildNotesSummary(visitDepartment.getId(), authUser),
                    visitDepartment.getAnswerId(),
                    hasFinalizedAnswersCycleGuard,
                    hasBillableCycleGuard,
                    visitDepartment.getCreatedAt(),
                    visitDepartment.getUpdatedAt()
            );
        }

        visitedDepartmentIds.add(visitDepartment.getId());

        List<WorkerDto> processors = resolveProcessors(visitDepartment);
        WorkerDto addedBy = resolveAddedBy(visitDepartment);
        WorkerDto completedBy = resolveCompletedBy(visitDepartment);

        List<VisitDepartmentProductDto> products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(item -> visitDepartmentProductToDto(item, visitInsuranceProviderIds))
                .toList();

        boolean canViewClinical = canViewClinicalData(authUser);

        List<VisitDepartmentDiagnosisDto> diagnostics = canViewClinical
                ? visitDepartmentDiagnosisRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream().map(this::visitDepartmentDiagnosisToDto).toList()
                : List.of();

        List<VisitDepartmentSymptomDto> symptoms = canViewClinical
                ? visitDepartmentSymptomRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream().map(this::visitDepartmentSymptomToDto).toList()
                : List.of();

        List<VisitDepartmentMedicationDto> medications = canViewClinical
                ? visitDepartmentMedicationRepository.findByVisitDepartmentId(visitDepartment.getId())
                        .stream().map(this::visitDepartmentMedicationToDto).toList()
                : List.of();

        List<VisitPreInstructionDto> preInstructions = canViewClinical
                ? visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartment.getId())
                        .stream().map(this::visitPreInstructionToDto).toList()
                : List.of();

        UUID answerId = canViewClinical ? visitDepartment.getAnswerId() : null;

        List<VisitDepartmentDto> childVisitDepartments = visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId())
                .stream()
                .map(child -> visitDepartmentToDto(child, visitInsuranceProviderIds, new LinkedHashSet<>(visitedDepartmentIds), authUser, preloadedDepartmentDtos))
                .toList();

        return new VisitDepartmentDto(
                visitDepartment.getId(),
                departmentDto,
                visitDepartment.getStatus(),
                visitDepartment.getEncounterType(),
                departmentProfileToDto(visitDepartment.getProfile()),
                visitDepartment.getStartedAt(),
                visitDepartment.getCompletedAt(),
                addedBy,
                completedBy,
                processors,
                products,
                diagnostics,
                symptoms,
                medications,
                preInstructions,
                childVisitDepartments,
                visitDepartmentNoteService.buildNotesSummary(visitDepartment.getId(), authUser),
                answerId,
                resolveHasFinalizedConsultationAnswers(visitDepartment),
                resolveHasBillableProducts(visitDepartment.getId()),
                visitDepartment.getCreatedAt(),
                visitDepartment.getUpdatedAt()
        );
    }

    public WorkerDto resolveAddedBy(VisitDepartment visitDepartment) {
        return resolveAddedBy(visitDepartment, null);
    }

    /**
     * @param preloaded workers already mapped for the current page; when it covers
     *                 the target worker no lazy proxy is touched at all
     */
    public WorkerDto resolveAddedBy(
            VisitDepartment visitDepartment,
            PreloadedWorkers preloaded) {
        if (visitDepartment == null) {
            return null;
        }
        WorkerDto preloadedDto = preloaded == null ? null : preloaded.find(visitDepartment.getAddedBy());
        if (preloadedDto != null) {
            return preloadedDto;
        }
        try {
            if (org.hibernate.Hibernate.isInitialized(visitDepartment.getAddedBy()) && visitDepartment.getAddedBy() != null) {
                return workerMapper.toDto(visitDepartment.getAddedBy());
            }
            if (visitDepartment.getId() != null) {
                return visitDepartmentRepository.findById(visitDepartment.getId())
                        .map(vd -> {
                            try {
                                return vd.getAddedBy() != null ? workerMapper.toDto(vd.getAddedBy()) : null;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null);
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    public WorkerDto resolveCompletedBy(VisitDepartment visitDepartment) {
        return resolveCompletedBy(visitDepartment, null);
    }

    /**
     * @see #resolveAddedBy(VisitDepartment, PreloadedWorkers)
     */
    public WorkerDto resolveCompletedBy(
            VisitDepartment visitDepartment,
            PreloadedWorkers preloaded) {
        if (visitDepartment == null) {
            return null;
        }
        WorkerDto preloadedDto = preloaded == null ? null : preloaded.find(visitDepartment.getCompletedBy());
        if (preloadedDto != null) {
            return preloadedDto;
        }
        try {
            if (org.hibernate.Hibernate.isInitialized(visitDepartment.getCompletedBy()) && visitDepartment.getCompletedBy() != null) {
                return workerMapper.toDto(visitDepartment.getCompletedBy());
            }
            if (visitDepartment.getId() != null) {
                return visitDepartmentRepository.findById(visitDepartment.getId())
                        .map(vd -> {
                            try {
                                return vd.getCompletedBy() != null ? workerMapper.toDto(vd.getCompletedBy()) : null;
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null);
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    public List<WorkerDto> resolveProcessors(VisitDepartment visitDepartment) {
        return resolveProcessors(visitDepartment, null);
    }

    /**
     * @see #resolveAddedBy(VisitDepartment, Map)
     */
    public List<WorkerDto> resolveProcessors(
            VisitDepartment visitDepartment,
            PreloadedWorkers preloaded) {
        if (visitDepartment == null) {
            return null;
        }
        try {
            // A pre-loaded entry covers the department's processors without
            // touching the lazy collection. An entry that maps to an empty list
            // means "genuinely no processors" and must not fall through, or we
            // would initialise the collection just to rediscover that.
            if (preloaded != null) {
                List<UUID> ids = preloaded.processorIdsByVisitDepartmentId().get(visitDepartment.getId());
                if (ids != null) {
                    if (ids.isEmpty()) {
                        return null;
                    }
                    return ids.stream()
                            .map(preloaded.workerDtos()::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                }
            }
            if (org.hibernate.Hibernate.isInitialized(visitDepartment.getProcessors()) && visitDepartment.getProcessors() != null) {
                return visitDepartment.getProcessors().isEmpty() ? null : visitDepartment.getProcessors().stream().map(workerMapper::toDto).toList();
            }
            if (visitDepartment.getId() != null) {
                return visitDepartmentRepository.findById(visitDepartment.getId())
                        .map(vd -> {
                            try {
                                return vd.getProcessors() == null || vd.getProcessors().isEmpty() ? null : vd.getProcessors().stream().map(workerMapper::toDto).toList();
                            } catch (Exception e) {
                                return null;
                            }
                        })
                        .orElse(null);
            }
        } catch (Exception ex) {
            return null;
        }
        return null;
    }

    /**
     * Workers pre-loaded for one page of visit departments.
     *
     * <p>{@code processorIdsByVisitDepartmentId} preserves the per-department
     * grouping (and the collection's iteration order) that a flat
     * {@code Map<UUID, WorkerDto>} cannot express, so the processors collection
     * never has to be initialised. A department that is <em>absent</em> from the
     * map was not part of the batch; a department mapped to an <em>empty</em> list
     * genuinely has no processors.
     */
    public record PreloadedWorkers(
            Map<UUID, WorkerDto> workerDtos,
            Map<UUID, List<UUID>> processorIdsByVisitDepartmentId) {

        public static final PreloadedWorkers EMPTY =
                new PreloadedWorkers(Map.of(), Map.of());

        public WorkerDto find(Worker worker) {
            if (worker == null || worker.getId() == null) {
                return null;
            }
            return workerDtos.get(worker.getId());
        }
    }

    public boolean canViewClinicalData(AuthenticatedUser authUser) {
        if (authUser == null) {
            return true;
        }
        if (authUser.roles() != null) {
            if (authUser.roles().contains(RoleName.ADMIN)
                    || authUser.roles().contains(RoleName.CLINIC_ADMIN)
                    || authUser.roles().contains(RoleName.MANAGER)
                    || authUser.roles().contains(RoleName.CLINICIAN)) {
                return true;
            }
        }
        if (rolePermissionService != null) {
            return rolePermissionService.userHasAnyPermission(
                    authUser,
                    PermissionName.CONSULTATION_VIEW,
                    PermissionName.CLINICAL_NOTE_VIEW,
                    PermissionName.DIAGNOSIS_RECORD
            );
        }
        return false;
    }

    /**
     * Batch-load DepartmentDto (with profiles) for multiple departments in just
     * two queries instead of N×2.  Used by {@link VisitService#visitToDto} to
     * avoid the N+1 problem when a visit has many departments.
     *
     * @return map of department ID → fully-populated DepartmentDto
     */
    public java.util.Map<UUID, DepartmentDto> loadDepartmentDtosWithProfiles(
            java.util.Collection<Department> departments) {
        java.util.Set<UUID> deptIds = departments.stream()
                .map(Department::getId)
                .collect(java.util.stream.Collectors.toSet());
        if (deptIds.isEmpty()) {
            return java.util.Map.of();
        }

        // 1 query: all profiles for these departments
        java.util.Map<UUID, java.util.List<DepartmentProfile>> profilesByDept =
                departmentProfileRepository.findByDepartmentIdIn(deptIds)
                        .stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                p -> p.getDepartment().getId()));

        // Collect all profile IDs, then 1 query: all product links
        java.util.Set<UUID> profileIds = profilesByDept.values().stream()
                .flatMap(java.util.List::stream)
                .map(DepartmentProfile::getId)
                .collect(java.util.stream.Collectors.toSet());

        java.util.Map<UUID, java.util.List<DepartmentProfileProduct>> productsByProfile;
        if (profileIds.isEmpty()) {
            productsByProfile = java.util.Map.of();
        } else {
            productsByProfile = departmentProfileProductRepository.findByProfileIdIn(profileIds)
                    .stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            link -> link.getProfile().getId()));
        }

        // Build DTOs
        java.util.Map<UUID, DepartmentDto> result = new java.util.HashMap<>();
        for (Department dept : departments) {
            java.util.List<DepartmentProfileDto> profileDtos = profilesByDept
                    .getOrDefault(dept.getId(), java.util.List.of())
                    .stream()
                    .map(profile -> new DepartmentProfileDto(
                            profile.getId(),
                            profile.getName(),
                            profile.getEncounterType(),
                            profile.isDefault(),
                            productsByProfile.getOrDefault(profile.getId(), java.util.List.of())
                                    .stream()
                                    .map(link -> productMapper.toDto(link.getProduct()))
                                    .toList(),
                            profile.getCreatedAt(),
                            profile.getUpdatedAt()
                    ))
                    .toList();
            result.put(dept.getId(), departmentMapper.toDtoWithDetails(
                    dept, java.util.List.of(), profileDtos, java.util.List.of(), null));
        }
        return result;
    }

    /**
     * Batch-load {@link DepartmentProfileDto} keyed by profile ID in just two
     * queries, instead of the two-per-profile cost of
     * {@link #departmentProfileToDto(DepartmentProfile)}.
     *
     * <p>{@link VisitService#visitsToDtos} maps {@code VisitDepartmentDto.profile}
     * for every department on the page. Reading it through
     * {@code departmentProfileToDto} initialised a lazy {@code DepartmentProfile}
     * proxy and then issued a {@code findByProfileId} per department, even though
     * the page loader had already fetched the same profiles and product links a
     * few queries earlier.
     *
     * <p>Profile IDs are read off the proxies without forcing initialisation
     * (Hibernate serves the identifier from the proxy's own state), so this
     * method costs 2 queries no matter how many departments reference profiles.
     *
     * @return map of profile ID → fully-populated DepartmentProfileDto
     */
    public java.util.Map<UUID, DepartmentProfileDto> loadProfileDtosById(
            java.util.Collection<UUID> profileIds) {
        java.util.Set<UUID> ids = profileIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }

        // 1 query: the profiles themselves
        List<DepartmentProfile> profiles = departmentProfileRepository.findAllById(ids);

        // 1 query: every product link for those profiles
        java.util.Map<UUID, List<DepartmentProfileProduct>> productsByProfile =
                departmentProfileProductRepository.findByProfileIdIn(ids).stream()
                        .collect(java.util.stream.Collectors.groupingBy(
                                link -> link.getProfile().getId()));

        java.util.Map<UUID, DepartmentProfileDto> result = new java.util.HashMap<>(profiles.size());
        for (DepartmentProfile profile : profiles) {
            result.put(profile.getId(), new DepartmentProfileDto(
                    profile.getId(),
                    profile.getName(),
                    profile.getEncounterType(),
                    profile.isDefault(),
                    productsByProfile.getOrDefault(profile.getId(), java.util.List.of()).stream()
                            .map(link -> productMapper.toDto(link.getProduct()))
                            .toList(),
                    profile.getCreatedAt(),
                    profile.getUpdatedAt()));
        }
        return result;
    }

    /**
     * Resolve a visit department's profile from a pre-loaded map, falling back to
     * the per-department query when the profile was not pre-loaded (e.g. a
     * profile created between the two reads).
     */
    public DepartmentProfileDto resolveProfileDto(
            DepartmentProfile profile,
            java.util.Map<UUID, DepartmentProfileDto> preloadedProfileDtos) {
        if (profile == null) {
            return null;
        }
        if (preloadedProfileDtos != null && profile.getId() != null) {
            DepartmentProfileDto preloaded = preloadedProfileDtos.get(profile.getId());
            if (preloaded != null) {
                return preloaded;
            }
        }
        return departmentProfileToDto(profile);
    }

    /**
     * Batch-load the worker DTOs referenced by a page of visit departments so
     * {@link #resolveAddedBy}, {@link #resolveCompletedBy} and
     * {@link #resolveProcessors} never touch a lazy proxy.
     *
     * <p>Each of those resolvers used to initialise up to three lazy references
     * per department (addedBy, completedBy, the processors collection), and every
     * initialised {@code Worker} then dragged its two EAGER collections
     * ({@code roles}, {@code departments}). On a 20-visit page that was hundreds
     * of queries for a handful of distinct workers.
     *
     * <p>Costs exactly 2 queries regardless of how many departments are passed:
     * one for the processor IDs off the join table, one {@code findAllById} for
     * the workers themselves.
     *
     * @param visitDepartments departments on the page
     */
    public PreloadedWorkers loadPreloadedWorkers(java.util.Collection<VisitDepartment> visitDepartments) {
        return loadPreloadedWorkers(visitDepartments, List.of(), List.of(), List.of());
    }

    /**
     * Batch-load the worker DTOs referenced by a page of visit departments so
     * {@link #resolveAddedBy}, {@link #resolveCompletedBy},
     * {@link #resolveProcessors} and {@link #visitDepartmentProductToDto} never
     * touch a lazy proxy.
     *
     * <p>Each of those resolvers used to initialise up to three lazy references
     * per department (addedBy, completedBy, the processors collection), and every
     * initialised {@code Worker} then dragged its two EAGER collections
     * ({@code roles}, {@code departments}). On a 20-visit page that was hundreds
     * of queries for a handful of distinct workers.
     *
     * <p>Costs a fixed 2 queries regardless of page size: one for the processor
     * IDs off the join table, one {@code findAllById} for the workers themselves.
     *
     * @param visitDepartments departments on the page
     * @param products         department products on the page (4 worker refs each)
     * @param preInstructions  pre-instructions on the page (3 worker refs each)
     * @param extraWorkers     any other workers already loaded for the page
     */
    public PreloadedWorkers loadPreloadedWorkers(
            java.util.Collection<VisitDepartment> visitDepartments,
            java.util.Collection<VisitDepartmentProduct> products,
            java.util.Collection<VisitPreInstruction> preInstructions,
            java.util.Collection<Worker> extraWorkers) {
        java.util.Set<UUID> vdIds = visitDepartments.stream()
                .filter(java.util.Objects::nonNull)
                .map(VisitDepartment::getId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (vdIds.isEmpty() && products.isEmpty() && preInstructions.isEmpty() && extraWorkers.isEmpty()) {
            return PreloadedWorkers.EMPTY;
        }

        // 1 query: processor IDs straight off the join table. The processors
        // collection is a lazy collection, so reading it would initialise one
        // query per department.
        java.util.Map<UUID, List<UUID>> processorIdsByVdId = new java.util.HashMap<>();
        for (VisitDepartment vd : visitDepartments) {
            if (vd.getId() != null) {
                // Seed every requested department with a mutable list, so a
                // department with no processors maps to an empty list (not
                // absent) and does not fall through to the lazy path.
                processorIdsByVdId.putIfAbsent(vd.getId(), new java.util.ArrayList<>());
            }
        }
        if (!vdIds.isEmpty()) {
            for (Object[] row : visitDepartmentRepository
                    .findProcessorPairsByVisitDepartmentIdIn(vdIds)) {
                UUID vdId = (UUID) row[0];
                UUID workerId = (UUID) row[1];
                if (vdId != null && workerId != null) {
                    processorIdsByVdId.computeIfAbsent(vdId, k -> new java.util.ArrayList<>()).add(workerId);
                }
            }
        }

        // Collect every worker ID without initialising any ManyToOne proxy —
        // Hibernate serves getId() from the proxy's own state.
        java.util.Set<UUID> workerIds = new java.util.LinkedHashSet<>();
        for (VisitDepartment vd : visitDepartments) {
            if (vd == null) {
                continue;
            }
            if (vd.getAddedBy() != null && vd.getAddedBy().getId() != null) {
                workerIds.add(vd.getAddedBy().getId());
            }
            if (vd.getCompletedBy() != null && vd.getCompletedBy().getId() != null) {
                workerIds.add(vd.getCompletedBy().getId());
            }
        }
        processorIdsByVdId.values().forEach(workerIds::addAll);
        for (VisitDepartmentProduct p : products) {
            if (p == null) {
                continue;
            }
            addWorkerId(workerIds, p.getAddedBy());
            addWorkerId(workerIds, p.getBilledBy());
            addWorkerId(workerIds, p.getConfirmedBy());
            addWorkerId(workerIds, p.getProcessor());
        }
        for (VisitPreInstruction pre : preInstructions) {
            if (pre == null) {
                continue;
            }
            addWorkerId(workerIds, pre.getAddedBy());
            for (VisitPreInstructionProductRequest req : pre.getProducts()) {
                if (req != null) {
                    addWorkerId(workerIds, req.getRequestedBy());
                    addWorkerId(workerIds, req.getProcessedBy());
                }
            }
        }
        for (Worker w : extraWorkers) {
            addWorkerId(workerIds, w);
        }

        return new PreloadedWorkers(loadWorkerDtosById(workerIds), processorIdsByVdId);
    }

    private static void addWorkerId(java.util.Set<UUID> target, Worker worker) {
        if (worker != null && worker.getId() != null) {
            target.add(worker.getId());
        }
    }

    /**
     * Map a set of worker IDs to DTOs in a single {@code findAllById}, so the
     * same worker referenced from many products/departments is mapped once.
     */
    public java.util.Map<UUID, WorkerDto> loadWorkerDtosById(java.util.Collection<UUID> workerIds) {
        java.util.Set<UUID> ids = workerIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        if (ids.isEmpty()) {
            return java.util.Map.of();
        }
        java.util.Map<UUID, WorkerDto> result = new java.util.HashMap<>();
        for (Worker worker : workerRepository.findAllById(ids)) {
            result.put(worker.getId(), workerMapper.toDto(worker));
        }
        return result;
    }

    /**
     * Map a Department entity to DepartmentDto, including profiles.
     * The simple {@code departmentMapper.toDto()} ignores profiles (MapStruct
     * {@code @Mapping(ignore=true)}), which causes a GraphQL
     * NullValueInNonNullableField error because the schema declares
     * {@code profiles: [DepartmentProfile!]!}.
     */
    private DepartmentDto departmentToDtoWithProfiles(Department department) {
        if (department == null) {
            return null;
        }
        Department loadedDept = department.getId() != null
                ? departmentRepository.findById(department.getId()).orElse(department)
                : department;
        List<DepartmentProfileDto> profiles = departmentProfileRepository
                .findByDepartmentId(loadedDept.getId())
                .stream()
                .map(profile -> new DepartmentProfileDto(
                        profile.getId(),
                        profile.getName(),
                        profile.getEncounterType(),
                        profile.isDefault(),
                        departmentProfileProductRepository.findByProfileId(profile.getId())
                                .stream()
                                .map(link -> productMapper.toDto(link.getProduct()))
                                .toList(),
                        profile.getCreatedAt(),
                        profile.getUpdatedAt()
                ))
                .toList();
        return departmentMapper.toDtoWithDetails(
                loadedDept, List.of(), profiles, List.of(), null
        );
    }

    private boolean resolveHasFinalizedConsultationAnswers(VisitDepartment visitDepartment) {
        if (visitDepartment.getAnswerId() == null) {
            return false; // no answers submitted yet
        }
        Optional<ConsultationAnswer> answerOpt = consultationAnswerRepository.findById(visitDepartment.getAnswerId());
        if (answerOpt.isEmpty()) {
            return false;
        }
        AnswerStatus status = answerOpt.get().getStatus();
        return status == AnswerStatus.FINAL || status == AnswerStatus.SUBMITTED;
    }

    private boolean resolveHasBillableProducts(UUID visitDepartmentId) {
        if (visitDepartmentId == null) return false;
        List<VisitDepartmentProduct> products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartmentId);
        boolean direct = products.stream()
                .filter(p -> !p.isDeleted())
                .anyMatch(p -> p.getBillingState() != BillingState.BILLED);
        if (direct) return true;
        List<VisitDepartment> children = visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartmentId);
        return children.stream().anyMatch(child -> resolveHasBillableProducts(child.getId()));
    }

    public DepartmentProfileDto departmentProfileToDto(DepartmentProfile profile) {
        if (profile == null) {
            return null;
        }
        return new DepartmentProfileDto(
                profile.getId(),
                profile.getName(),
                profile.getEncounterType(),
                profile.isDefault(),
                departmentProfileProductRepository.findByProfileId(profile.getId())
                        .stream()
                        .map(link -> productMapper.toDto(link.getProduct()))
                        .toList(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    /**
     * @deprecated the {@code visitInsuranceProviderIds} argument was never read;
     * use {@link #visitDepartmentProductToDto(VisitDepartmentProduct, PreloadedWorkers)}
     */
    @Deprecated
    public VisitDepartmentProductDto visitDepartmentProductToDto(
            VisitDepartmentProduct item,
            Set<UUID> visitInsuranceProviderIds
    ) {
        return visitDepartmentProductToDto(item, PreloadedWorkers.EMPTY);
    }

    /**
     * @param preloaded workers already mapped for the current page. A product row
     *                 carries four worker references, so on a full page this mapper
     *                 runs hundreds of times over a handful of distinct workers;
     *                 without the map each call initialised a lazy proxy.
     */
    public VisitDepartmentProductDto visitDepartmentProductToDto(
            VisitDepartmentProduct item,
            PreloadedWorkers preloaded
    ) {
        return new VisitDepartmentProductDto(
                item.getId(),
                productMapper.toDto(item.getProduct()),
                item.getQuantity(),
                item.getStatus(),
                item.getBillingState(),
                item.getExemptionMode(),
                item.getSource(),
                workerDto(item.getAddedBy(), preloaded),
                workerDto(item.getBilledBy(), preloaded),
                workerDto(item.getConfirmedBy(), preloaded),
                item.getBillingConfirmationStatus(),
                workerDto(item.getProcessor(), preloaded),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    /**
     * Map a worker for a DTO slot: reuse the page's pre-mapped DTO when the
     * worker was pre-loaded, otherwise fall back to the lazy proxy. Returns null
     * for a null worker, matching the previous {@code workerMapper.toDto(null)}
     * behaviour.
     */
    private WorkerDto workerDto(Worker worker, PreloadedWorkers preloaded) {
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

    public VisitDepartmentDiagnosisDto visitDepartmentDiagnosisToDto(VisitDepartmentDiagnosis item) {
        return new VisitDepartmentDiagnosisDto(
                item.getId(),
                item.getDiagnosisName(),
                item.getIcd11Code(),
                item.getType(),
                item.getNotes(),
                item.getCreatedAt()
        );
    }

    public VisitDepartmentSymptomDto visitDepartmentSymptomToDto(VisitDepartmentSymptom item) {
        return new VisitDepartmentSymptomDto(
                item.getId(),
                item.getSymptomName(),
                item.getSonomedId(),
                item.getNotes(),
                item.getCreatedAt()
        );
    }

    public VisitDepartmentMedicationDto visitDepartmentMedicationToDto(VisitDepartmentMedication item) {
        return new VisitDepartmentMedicationDto(
                item.getId(),
                item.getMedicationName(),
                item.getInstructions(),
                item.getCreatedAt()
        );
    }

    public VisitPreInstructionDto visitPreInstructionToDto(VisitPreInstruction pre) {
        return visitPreInstructionToDto(pre, null);
    }

    /**
     * @param preloaded workers already mapped for the current page, see
     *                 {@link #visitDepartmentProductToDto(VisitDepartmentProduct, PreloadedWorkers)}
     */
    public VisitPreInstructionDto visitPreInstructionToDto(
            VisitPreInstruction pre,
            PreloadedWorkers preloaded) {
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
                        workerDto(item.getRequestedBy(), preloaded),
                        item.getStatus(),
                        workerDto(item.getProcessedBy(), preloaded),
                        item.getCreatedAt(),
                        item.getUpdatedAt()
                )).toList();

        return new VisitPreInstructionDto(
                pre.getId(),
                pre.getType(),
                pre.getNote(),
                workerDto(pre.getAddedBy(), preloaded),
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
                requestedProcessor = workerRepository.findById(requestedProcessorId).orElse(null);
            }
            if (requestedProcessor == null) {
                return ApiResponse.error("processorId must belong to the visit department processors.");
            }
            item.setProcessor(requestedProcessor);
            return null;
        }

        if (actingUser != null) {
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

        // Multiple processors and no explicit processorId: assign the first one
        // rather than blocking. The billing UI can reassign later via
        // updateVisitDepartmentProductProcessor if needed.
        item.setProcessor(processors.get(0));
        return null;
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

    public VisitDepartment deleteChildVisitDepartmentIfEmpty(UUID visitDepartmentId) {
        if (visitDepartmentId == null) {
            return null;
        }
        Optional<VisitDepartment> vdOpt = visitDepartmentRepository.findById(visitDepartmentId);
        if (vdOpt.isEmpty()) {
            return null;
        }
        return deleteChildVisitDepartmentIfEmpty(vdOpt.get());
    }

    public VisitDepartment deleteChildVisitDepartmentIfEmpty(VisitDepartment visitDepartment) {
        if (visitDepartment == null || visitDepartment.getId() == null) {
            return null;
        }

        Optional<VisitDepartment> freshOpt = visitDepartmentRepository.findById(visitDepartment.getId());
        if (freshOpt.isEmpty()) {
            return null;
        }
        VisitDepartment target = freshOpt.get();

        if (target.getParentVisitDepartment() == null) {
            return target;
        }

        // Use IncludingDeleted: a soft-deleted product still has a FK row,
        // so the department is not truly empty and must not be deleted.
        List<VisitDepartmentProduct> remainingProducts = visitDepartmentProductRepository
                .findByVisitDepartmentIdIncludingDeleted(target.getId());
        if (!remainingProducts.isEmpty()) {
            return target;
        }

        // FK guard: a child with clinical/financial dependents cannot be hard-deleted —
        // deleting it would throw DataIntegrityViolationException -> 500. Keep it and
        // let the caller decide (it will surface in the DTO as a still-present child).
        if (!visitDepartmentDiagnosisRepository.findByVisitDepartmentId(target.getId()).isEmpty()
                || !visitDepartmentMedicationRepository.findByVisitDepartmentId(target.getId()).isEmpty()
                || !visitPreInstructionRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(target.getId()).isEmpty()
                || visitDepartmentNoteServiceHasNotes(target.getId())
                || visitDepartmentBillingRepository.existsByVisitDepartmentId(target.getId())) {
            return target;
        }

        VisitDepartment parent = target.getParentVisitDepartment();
        visitDepartmentRepository.delete(target);
        visitDepartmentRepository.flush();
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

    private void reopenVisitIfCompleted(UUID visitId) {
        if (visitId == null) return;
        try {
            visitRepository.findById(visitId).ifPresent(this::reopenVisitIfCompleted);
        } catch (Exception ignored) {}
    }

    private void reopenVisitIfCompleted(Visit visit) {
        if (visit == null) return;
        try {
            if (visit.getStatus() == VisitStatus.COMPLETED) {
                visit.setStatus(VisitStatus.IN_PROGRESS);
                visitRepository.save(visit);
            }
        } catch (Exception e) {
            if (visit.getId() != null) {
                visitRepository.findById(visit.getId()).ifPresent(v -> {
                    if (v.getStatus() == VisitStatus.COMPLETED) {
                        v.setStatus(VisitStatus.IN_PROGRESS);
                        visitRepository.save(v);
                    }
                });
            }
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

    @Transactional
    public ApiResponse<VisitDepartmentDto> updateVisitDepartmentEncounterDate(
            com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentEncounterDateInput input,
            AuthenticatedUser authUser
    ) {
        if (input == null || input.visitDepartmentId() == null || input.encounterDate() == null) {
            return ApiResponse.error("visitDepartmentId and encounterDate are required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required.");
        }

        boolean authorised = actingUser.getRoles().contains(RoleName.ADMIN)
                || actingUser.getRoles().contains(RoleName.MANAGER);
        if (!authorised) {
            return ApiResponse.error("Only admin and manager can update the department encounter date.");
        }

        Optional<VisitDepartment> deptOpt = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (deptOpt.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment dept = deptOpt.get();
        if (dept.getStatus() == VisitDepartmentStatus.DEPARTMENT_EDITING) {
            return ApiResponse.error("Cannot change encounter date while department is in billing edit mode.");
        }
        Visit visit = dept.getVisit();
        if (visit != null && visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot change encounter date for a cancelled visit.");
        }

        java.time.LocalDateTime newEncounterDate = input.encounterDate();
        boolean visitDateAdjusted = false;
        if (visit != null && visit.getVisitDate() != null && newEncounterDate.isBefore(visit.getVisitDate())) {
            java.time.LocalDateTime adjustedVisitDate = newEncounterDate.minusMinutes(2);
            ApiResponse<?> visitDateResponse = visitService.changeVisitDate(
                new com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput(
                    visit.getId(), adjustedVisitDate
                ),
                authUser
            );
            if (visitDateResponse.status() != ResponseStatus.SUCCESS) {
                return ApiResponse.error(
                    "Encounter date was not changed because the visit date could not be adjusted: "
                        + visitDateResponse.message()
                );
            }
            visitDateAdjusted = true;
            dept = visitDepartmentRepository.findById(dept.getId()).orElseThrow();
            visit = dept.getVisit();
        }

        dept.setStartedAt(newEncounterDate);
        visitDepartmentRepository.save(dept);

        // If this department has billings, clamp any billing dates that would be before encounterDate + 5 mins
        List<DepartmentInsuranceBilling> billings = departmentInsuranceBillingRepository.findAllByVisitDepartmentId(dept.getId());
        if (!billings.isEmpty()) {
            java.time.LocalDateTime minBilling = newEncounterDate.plusMinutes(5);
            for (DepartmentInsuranceBilling b : billings) {
                if (b.getBillingDate() != null && b.getBillingDate().isBefore(minBilling)) {
                    b.setBillingDate(minBilling);
                }
                b.setInvoiceUrl(null);
                departmentInsuranceBillingRepository.save(b);
            }

            if (!visitDateAdjusted) {
                // Regenerate invoices after the transaction commits so the expensive
                // PDF render and upload do not hold the DB connection open. This mirrors
                // the invoice-regeneration path used by changeVisitDate.
                List<UUID> billingIds = billings.stream()
                    .map(DepartmentInsuranceBilling::getId)
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
                                                "Invoice regeneration after encounter date change for {} returned: {}",
                                                bid, result.message()
                                            );
                                        }
                                    } catch (Exception e) {
                                        log.error(
                                            "Failed to regenerate invoice after encounter date change for {}: {}",
                                            bid, e.getMessage(), e
                                        );
                                    }
                                }
                            }
                        }
                    );
            }
        }

        if (visit != null) {
            meilisearchIndexService.indexVisit(visit);
        }

        return ApiResponse.success("Department encounter date updated.", visitDepartmentToDto(dept));
    }

    public BigDecimal normalizeQuantity(BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ONE;
        }
        return value;
    }

}
