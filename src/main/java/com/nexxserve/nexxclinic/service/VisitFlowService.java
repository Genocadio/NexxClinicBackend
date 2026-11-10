package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.events.ClinicEventPublisher;
import com.nexxserve.nexxclinic.model.DepartmentKind;
import com.nexxserve.nexxclinic.model.EncounterType;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.*;
import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class VisitFlowService {

    private static final Logger log = LoggerFactory.getLogger(VisitFlowService.class);

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final DepartmentRepository departmentRepository;
    private final VisitDepartmentTransitionRepository transitionRepository;
    private final WorkerRepository workerRepository;
    private final ClinicEventPublisher clinicEventPublisher;
    private final VisitService visitService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VisitFlowService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            DepartmentRepository departmentRepository,
            VisitDepartmentTransitionRepository transitionRepository,
            WorkerRepository workerRepository,
            ClinicEventPublisher clinicEventPublisher,
            @Lazy VisitService visitService
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.departmentRepository = departmentRepository;
        this.transitionRepository = transitionRepository;
        this.workerRepository = workerRepository;
        this.clinicEventPublisher = clinicEventPublisher;
        this.visitService = visitService;
    }

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) return null;
        return workerRepository.findById(authUser.userId()).orElse(null);
    }

    /**
     * Explicit Routing Action: Discharge patient from billing or clinic.
     * Marks open/billing departments as COMPLETED and sets visit status to COMPLETED.
     */
    @Transactional
    public ApiResponse<VisitDto> dischargeVisit(UUID visitId, UUID departmentId, String note, AuthenticatedUser authUser) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }

        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOpt.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot discharge a cancelled visit.");
        }

        Worker actor = resolveWorker(authUser);
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);

        VisitDepartment targetVd = null;
        if (departmentId != null) {
            targetVd = departments.stream()
                    .filter(vd -> vd.getId().equals(departmentId) || (vd.getDepartment() != null && vd.getDepartment().getId().equals(departmentId)))
                    .findFirst()
                    .orElse(null);
        }

        LocalDateTime now = LocalDateTime.now();

        // Complete any active or billing departments
        for (VisitDepartment vd : departments) {
            if (vd.getStatus() != VisitDepartmentStatus.CANCELLED && vd.getStatus() != VisitDepartmentStatus.FINALISED) {
                String prevStatus = vd.getStatus().name();
                vd.setStatus(VisitDepartmentStatus.COMPLETED);
                vd.setCompletedAt(now);
                vd.setCompletedBy(actor);
                vd.setAwaitingRole(null);
                vd.setLastRoutingAction("DISCHARGED");
                visitDepartmentRepository.save(vd);

                // Record transition for this department
                recordTransition(visit, vd, prevStatus, "COMPLETED", "DISCHARGE", null, null, actor, note);
            }
        }

        visit.setStatus(VisitStatus.COMPLETED);
        visit.setIsDischarged(true);
        visit.setLastFlowActivityAt(now);
        recomputeVisitFlowSummary(visit, departments);
        Visit saved = visitRepository.save(visit);

        clinicEventPublisher.publishVisitDischarged(saved, actor);
        if (targetVd != null) {
            clinicEventPublisher.publishVisitRouted(saved, targetVd, "DISCHARGE", null, null, actor);
        }

        return ApiResponse.success("Patient successfully discharged.", visitService.visitToDto(saved));
    }

    /**
     * Explicit Routing Action: Send back patient to previous department (e.g. from Billing back to Doctor).
     */
    @Transactional
    public ApiResponse<VisitDto> sendBackVisit(UUID visitId, UUID fromDepartmentId, UUID targetDepartmentId, String note, AuthenticatedUser authUser) {
        if (visitId == null || fromDepartmentId == null) {
            return ApiResponse.error("visitId and fromDepartmentId are required.");
        }

        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOpt.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot route a cancelled visit.");
        }

        Worker actor = resolveWorker(authUser);
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);

        VisitDepartment fromVd = departments.stream()
                .filter(vd -> vd.getId().equals(fromDepartmentId) || (vd.getDepartment() != null && vd.getDepartment().getId().equals(fromDepartmentId)))
                .findFirst()
                .orElse(null);

        if (fromVd == null) {
            return ApiResponse.error("Source department not found on visit.");
        }

        // Determine destination department
        VisitDepartment targetVd = null;
        if (targetDepartmentId != null) {
            targetVd = departments.stream()
                    .filter(vd -> vd.getId().equals(targetDepartmentId) || (vd.getDepartment() != null && vd.getDepartment().getId().equals(targetDepartmentId)))
                    .findFirst()
                    .orElse(null);
        }

        // Fallback: use previousDepartment if set
        if (targetVd == null && fromVd.getPreviousDepartment() != null) {
            targetVd = departments.stream()
                    .filter(vd -> vd.getDepartment() != null && vd.getDepartment().getId().equals(fromVd.getPreviousDepartment().getId()))
                    .findFirst()
                    .orElse(null);
        }

        // Fallback: use root parent or earliest consultation
        if (targetVd == null) {
            targetVd = departments.stream()
                    .filter(vd -> !vd.getId().equals(fromVd.getId()) && vd.getDepartment() != null && vd.getDepartment().getKind() == DepartmentKind.CONSULTATION)
                    .findFirst()
                    .orElse(null);
        }

        // Fallback: use parent department
        if (targetVd == null && fromVd.getParentVisitDepartment() != null) {
            targetVd = fromVd.getParentVisitDepartment();
        }

        if (targetVd == null) {
            return ApiResponse.error("Could not determine previous department to return patient to.");
        }

        LocalDateTime now = LocalDateTime.now();

        // Re-activate destination department
        String prevStatus = targetVd.getStatus().name();
        targetVd.setStatus(VisitDepartmentStatus.ACTIVE);
        targetVd.setAwaitingRole("CLINICIAN");
        targetVd.setAwaitingSince(now);
        targetVd.setLastRoutingAction("SENT_BACK");
        targetVd.setPreviousDepartment(fromVd.getDepartment());
        visitDepartmentRepository.save(targetVd);

        // Update visit flow state
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setIsDischarged(false);
        visit.setHasReturnedFromBilling(true);
        visit.setLastFlowActivityAt(now);
        recomputeVisitFlowSummary(visit, departments);
        Visit saved = visitRepository.save(visit);

        recordTransition(
                saved,
                targetVd,
                prevStatus,
                "ACTIVE",
                "SEND_BACK",
                targetVd.getDepartment(),
                fromVd.getDepartment(),
                actor,
                note
        );

        clinicEventPublisher.publishDepartmentStateChanged(targetVd, actor);
        clinicEventPublisher.publishVisitRouted(
                saved,
                targetVd,
                "SEND_BACK",
                targetVd.getDepartment(),
                fromVd.getDepartment(),
                actor
        );

        return ApiResponse.success("Patient successfully returned to previous service.", visitService.visitToDto(saved));
    }

    /**
     * Explicit Routing Action: Forward patient to next service (e.g. from Consultation/Billing to Lab, Imaging, Ward, or Pharmacy).
     */
    @Transactional
    public ApiResponse<VisitDto> sendVisitToNext(UUID visitId, UUID fromDepartmentId, UUID targetDepartmentId, String note, AuthenticatedUser authUser) {
        if (visitId == null || fromDepartmentId == null || targetDepartmentId == null) {
            return ApiResponse.error("visitId, fromDepartmentId and targetDepartmentId are required.");
        }

        Optional<Visit> visitOpt = visitRepository.findById(visitId);
        if (visitOpt.isEmpty()) {
            return ApiResponse.error("Visit not found.");
        }

        Visit visit = visitOpt.get();
        if (visit.getStatus() == VisitStatus.CANCELLED) {
            return ApiResponse.error("Cannot route a cancelled visit.");
        }

        Optional<Department> targetDeptOpt = departmentRepository.findById(targetDepartmentId);
        if (targetDeptOpt.isEmpty()) {
            return ApiResponse.error("Target department not found.");
        }
        Department targetDept = targetDeptOpt.get();

        Worker actor = resolveWorker(authUser);
        List<VisitDepartment> departments = visitDepartmentRepository.findByVisitId(visitId);

        VisitDepartment fromVd = departments.stream()
                .filter(vd -> vd.getId().equals(fromDepartmentId) || (vd.getDepartment() != null && vd.getDepartment().getId().equals(fromDepartmentId)))
                .findFirst()
                .orElse(null);

        if (fromVd == null) {
            return ApiResponse.error("Source department not found on visit.");
        }

        LocalDateTime now = LocalDateTime.now();

        // Determine destination awaiting role based on DepartmentKind
        String awaitingRole = resolveAwaitingRole(targetDept.getKind());

        // Find or create target VisitDepartment
        VisitDepartment targetVd = visitDepartmentRepository.findByVisitIdAndDepartmentId(visit.getId(), targetDept.getId())
                .orElse(null);

        String prevStatus = targetVd != null ? targetVd.getStatus().name() : "NONE";

        if (targetVd == null) {
            targetVd = new VisitDepartment();
            targetVd.setVisit(visit);
            targetVd.setDepartment(targetDept);
            targetVd.setStatus(VisitDepartmentStatus.ACTIVE);
            targetVd.setEncounterType(targetDept.getKind() == DepartmentKind.WARD ? EncounterType.INPATIENT_ADMISSION : EncounterType.OUTPATIENT);
            targetVd.setStartedAt(now);
            targetVd.setAddedBy(actor);
            targetVd.setAwaitingRole(awaitingRole);
            targetVd.setAwaitingSince(now);
            targetVd.setLastRoutingAction("SENT_TO_NEXT");
            targetVd.setPreviousDepartment(fromVd.getDepartment());

            VisitDepartment rootVd = visitDepartmentRepository.findByVisitIdAndParentVisitDepartmentIsNull(visit.getId())
                    .stream().findFirst().orElse(null);
            if (rootVd != null && !rootVd.getId().equals(targetVd.getId())) {
                targetVd.setParentVisitDepartment(rootVd);
            }

            targetVd = visitDepartmentRepository.save(targetVd);
            departments.add(targetVd);
        } else {
            targetVd.setStatus(VisitDepartmentStatus.ACTIVE);
            targetVd.setAwaitingRole(awaitingRole);
            targetVd.setAwaitingSince(now);
            targetVd.setLastRoutingAction("SENT_TO_NEXT");
            targetVd.setPreviousDepartment(fromVd.getDepartment());
            targetVd = visitDepartmentRepository.save(targetVd);
        }

        // If source department was in billing and all billing was handled, mark it as completed
        if (fromVd.getStatus() == VisitDepartmentStatus.BILLING) {
            fromVd.setStatus(VisitDepartmentStatus.COMPLETED);
            fromVd.setCompletedAt(now);
            fromVd.setCompletedBy(actor);
            visitDepartmentRepository.save(fromVd);
        }

        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setIsDischarged(false);
        visit.setLastFlowActivityAt(now);
        recomputeVisitFlowSummary(visit, departments);
        Visit saved = visitRepository.save(visit);

        recordTransition(
                saved,
                targetVd,
                prevStatus,
                "ACTIVE",
                "SEND_TO_NEXT",
                targetDept,
                fromVd.getDepartment(),
                actor,
                note
        );

        clinicEventPublisher.publishDepartmentStateChanged(targetVd, actor);
        clinicEventPublisher.publishVisitRouted(
                saved,
                targetVd,
                "SEND_TO_NEXT",
                targetDept,
                fromVd.getDepartment(),
                actor
        );

        return ApiResponse.success("Patient successfully routed to " + targetDept.getName() + ".", visitService.visitToDto(saved));
    }

    private String resolveAwaitingRole(DepartmentKind kind) {
        if (kind == null) return "CLINICIAN";
        return switch (kind) {
            case LABORATORY -> "LAB";
            case IMAGING -> "IMAGING";
            case WARD -> "WARD";
            case PHARMACY -> "PHARMACY";
            case TRIAGE -> "NURSE";
            case CONSULTATION -> "CLINICIAN";
            case PROCEDURE_ROOM -> "CLINICIAN";
            case OTHER -> "CLINICIAN";
        };
    }

    public void recomputeVisitFlowSummary(Visit visit, List<VisitDepartment> departments) {
        if (visit == null) return;
        if (departments == null) {
            departments = visitDepartmentRepository.findByVisitId(visit.getId());
        }

        boolean isDischarged = visit.getStatus() == VisitStatus.COMPLETED || visit.getStatus() == VisitStatus.FINALISED;
        visit.setIsDischarged(isDischarged);

        int activeEpisodes = 0;
        boolean hasReturned = false;
        boolean hasPendingResults = false;
        List<Map<String, Object>> locations = new ArrayList<>();

        for (VisitDepartment vd : departments) {
            if (vd.getStatus() != VisitDepartmentStatus.CANCELLED
                    && vd.getStatus() != VisitDepartmentStatus.COMPLETED
                    && vd.getStatus() != VisitDepartmentStatus.FINALISED) {
                activeEpisodes++;

                Department d = vd.getDepartment();
                Map<String, Object> loc = new HashMap<>();
                loc.put("id", vd.getId().toString());
                loc.put("name", d != null ? d.getName() : "Unknown");
                loc.put("kind", d != null && d.getKind() != null ? d.getKind().name() : "CONSULTATION");
                loc.put("status", vd.getStatus().name());
                loc.put("awaitingRole", vd.getAwaitingRole());
                locations.add(loc);

                if ("SENT_BACK".equals(vd.getLastRoutingAction())) {
                    hasReturned = true;
                }

                if (d != null && (d.getKind() == DepartmentKind.LABORATORY || d.getKind() == DepartmentKind.IMAGING)) {
                    hasPendingResults = true;
                }
            }
        }

        visit.setActiveEpisodeCount(activeEpisodes);
        visit.setHasReturnedFromBilling(hasReturned);
        visit.setHasPendingResults(hasPendingResults);

        try {
            visit.setCurrentLocations(objectMapper.writeValueAsString(locations));
        } catch (Exception e) {
            log.warn("Failed to serialize currentLocations for visit {}: {}", visit.getId(), e.getMessage());
            visit.setCurrentLocations("[]");
        }
    }

    private void recordTransition(
            Visit visit,
            VisitDepartment vd,
            String fromStatus,
            String toStatus,
            String actionType,
            Department targetDept,
            Department prevDept,
            Worker actor,
            String note
    ) {
        try {
            VisitDepartmentTransition transition = new VisitDepartmentTransition();
            transition.setVisit(visit);
            transition.setVisitDepartment(vd);
            transition.setFromStatus(fromStatus != null ? fromStatus : "NONE");
            transition.setToStatus(toStatus != null ? toStatus : "NONE");
            transition.setActionType(actionType);
            transition.setTargetDepartment(targetDept);
            transition.setPreviousDepartment(prevDept);
            transition.setActor(actor);
            transition.setOccurredAt(LocalDateTime.now());
            transition.setNote(note);
            transitionRepository.save(transition);
        } catch (Exception e) {
            log.warn("Failed to record visit department transition: {}", e.getMessage());
        }
    }
}
