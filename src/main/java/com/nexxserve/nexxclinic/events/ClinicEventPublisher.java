package com.nexxserve.nexxclinic.events;

import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ClinicEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(ClinicEventPublisher.class);

    private final ClinicSseRegistry sseRegistry;
    private final VisitDepartmentRepository visitDepartmentRepository;

    public ClinicEventPublisher(ClinicSseRegistry sseRegistry, VisitDepartmentRepository visitDepartmentRepository) {
        this.sseRegistry = sseRegistry;
        this.visitDepartmentRepository = visitDepartmentRepository;
    }

    public void publishVisitCreated(Visit visit, Worker creator) {
        if (visit == null) return;
        try {
            Set<UUID> targetDeptIds = new HashSet<>();
            List<VisitDepartment> depts = visitDepartmentRepository.findByVisitId(visit.getId());
            for (VisitDepartment vd : depts) {
                if (vd.getDepartment() != null) {
                    targetDeptIds.add(vd.getDepartment().getId());
                }
            }

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_CREATED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(creator != null ? creator.getId() : null)
                    .actorName(creator != null ? creator.getFirstName() + " " + creator.getLastName() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetDepartmentIds(targetDeptIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_CREATED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishDepartmentStateChanged(VisitDepartment vd, Worker actor) {
        if (vd == null || vd.getVisit() == null) return;
        try {
            Set<RoleName> targetRoles = new HashSet<>(Set.of(RoleName.CLINICIAN, RoleName.NURSE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN));
            if (vd.getStatus() == VisitDepartmentStatus.BILLING || vd.getStatus() == VisitDepartmentStatus.COMPLETED) {
                targetRoles.add(RoleName.FINANCE);
            }

            Set<UUID> targetWorkerIds = new HashSet<>();
            if (vd.getProcessors() != null && !vd.getProcessors().isEmpty()) {
                vd.getProcessors().forEach(p -> targetWorkerIds.add(p.getId()));
            }

            Set<UUID> targetDeptIds = new HashSet<>();
            if (vd.getDepartment() != null) {
                targetDeptIds.add(vd.getDepartment().getId());
            }

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_DEPARTMENT_STATE_CHANGED")
                    .visitId(vd.getVisit().getId())
                    .patientId(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getId() : null)
                    .patientName(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getFirstName() + " " + vd.getVisit().getPatient().getLastName() : null)
                    .departmentId(vd.getDepartment() != null ? vd.getDepartment().getId() : null)
                    .departmentName(vd.getDepartment() != null ? vd.getDepartment().getName() : null)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(vd.getVisit().getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(targetRoles)
                    .targetDepartmentIds(targetDeptIds)
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_DEPARTMENT_STATE_CHANGED event for visit {}: {}", vd.getVisit().getId(), e.getMessage());
        }
    }

    public void publishProductChanged(VisitDepartment vd, String productName, Integer quantity, String action, Worker actor) {
        if (vd == null || vd.getVisit() == null) return;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("action", action);
            if (productName != null) metadata.put("productName", productName);
            if (quantity != null) metadata.put("quantity", quantity);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(vd.getVisit());

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_PRODUCT_CHANGED")
                    .visitId(vd.getVisit().getId())
                    .patientId(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getId() : null)
                    .patientName(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getFirstName() + " " + vd.getVisit().getPatient().getLastName() : null)
                    .departmentId(vd.getDepartment() != null ? vd.getDepartment().getId() : null)
                    .departmentName(vd.getDepartment() != null ? vd.getDepartment().getName() : null)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(vd.getVisit().getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.CLINICIAN, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_PRODUCT_CHANGED event for visit {}: {}", vd.getVisit().getId(), e.getMessage());
        }
    }

    public void publishNoteAdded(VisitDepartmentNote note, Worker actor) {
        if (note == null || note.getVisitDepartment() == null || note.getVisitDepartment().getVisit() == null) return;
        try {
            VisitDepartment vd = note.getVisitDepartment();
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("noteId", note.getId());
            metadata.put("noteType", note.getNoteType() != null ? note.getNoteType().name() : "NOTE");

            Set<UUID> targetWorkerIds = extractAttendingWorkers(vd.getVisit());

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_NOTE_ADDED")
                    .visitId(vd.getVisit().getId())
                    .patientId(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getId() : null)
                    .patientName(vd.getVisit().getPatient() != null ? vd.getVisit().getPatient().getFirstName() + " " + vd.getVisit().getPatient().getLastName() : null)
                    .departmentId(vd.getDepartment() != null ? vd.getDepartment().getId() : null)
                    .departmentName(vd.getDepartment() != null ? vd.getDepartment().getName() : null)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(vd.getVisit().getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_NOTE_ADDED event: {}", e.getMessage());
        }
    }

    public void publishInsuranceChanged(Visit visit, String action, String insuranceName, Worker actor) {
        if (visit == null) return;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("action", action);
            if (insuranceName != null) metadata.put("insuranceName", insuranceName);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_INSURANCE_CHANGED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.FINANCE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_INSURANCE_CHANGED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishVitalsChanged(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_VITALS_CHANGED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.NURSE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_VITALS_CHANGED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishBillingCompleted(Visit visit, Worker cashier) {
        if (visit == null) return;
        try {
            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("BILLING_COMPLETED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(cashier != null ? cashier.getId() : null)
                    .actorName(cashier != null ? cashier.getFirstName() + " " + cashier.getLastName() : null)
                    .targetRoles(Set.of(RoleName.FINANCE, RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting BILLING_COMPLETED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishVisitDischarged(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_DISCHARGED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_DISCHARGED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishVisitCancelled(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_CANCELLED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_CANCELLED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishVisitReopened(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            Set<UUID> targetWorkerIds = extractAttendingWorkers(visit);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_REOPENED")
                    .visitId(visit.getId())
                    .patientId(visit.getPatient() != null ? visit.getPatient().getId() : null)
                    .patientName(visit.getPatient() != null ? visit.getPatient().getFirstName() + " " + visit.getPatient().getLastName() : null)
                    .visitStatus(visit.getStatus())
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getFirstName() + " " + actor.getLastName() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_REOPENED event for visit {}: {}", visit.getId(), e.getMessage());
        }
    }

    public void publishPresenceChanged(UUID visitId, List<Map<String, Object>> activeViewers) {
        if (visitId == null) return;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("viewers", activeViewers);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_PRESENCE_CHANGED")
                    .visitId(visitId)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_PRESENCE_CHANGED event: {}", e.getMessage());
        }
    }

    private Set<UUID> extractAttendingWorkers(Visit visit) {
        Set<UUID> workerIds = new HashSet<>();
        if (visit == null || visit.getId() == null) return workerIds;
        List<VisitDepartment> depts = visitDepartmentRepository.findByVisitId(visit.getId());
        for (VisitDepartment vd : depts) {
            if (vd.getProcessors() != null) {
                vd.getProcessors().forEach(p -> workerIds.add(p.getId()));
            }
            if (vd.getAddedBy() != null) {
                workerIds.add(vd.getAddedBy().getId());
            }
            if (vd.getCompletedBy() != null) {
                workerIds.add(vd.getCompletedBy().getId());
            }
        }
        return workerIds;
    }
}
