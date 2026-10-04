package com.nexxserve.nexxclinic.events;

import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import java.util.Collections;
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
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetDeptIds = new HashSet<>();
            try {
                List<VisitDepartment> depts = visitDepartmentRepository.findByVisitId(visitId);
                for (VisitDepartment vd : depts) {
                    if (vd != null && vd.getDepartment() != null) {
                        targetDeptIds.add(vd.getDepartment().getId());
                    }
                }
            } catch (Exception ignored) {}

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_CREATED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(creator != null ? creator.getId() : null)
                    .actorName(creator != null ? (creator.getFirstName() + " " + creator.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.NURSE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetDepartmentIds(targetDeptIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_CREATED event: {}", e.getMessage());
        }
    }

    public void publishDepartmentStateChanged(VisitDepartment vd, Worker actor) {
        if (vd == null) return;
        try {
            Visit visit = vd.getVisit();
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<RoleName> targetRoles = new HashSet<>(Set.of(RoleName.CLINICIAN, RoleName.NURSE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN));
            if (vd.getStatus() == VisitDepartmentStatus.BILLING || vd.getStatus() == VisitDepartmentStatus.COMPLETED) {
                targetRoles.add(RoleName.FINANCE);
            }

            Set<UUID> targetWorkerIds = new HashSet<>();
            try {
                if (vd.getProcessors() != null && !vd.getProcessors().isEmpty()) {
                    vd.getProcessors().forEach(p -> {
                        if (p != null && p.getId() != null) targetWorkerIds.add(p.getId());
                    });
                }
            } catch (Exception ignored) {}

            Set<UUID> targetDeptIds = new HashSet<>();
            UUID departmentId = null;
            String departmentName = null;
            try {
                if (vd.getDepartment() != null) {
                    departmentId = vd.getDepartment().getId();
                    departmentName = vd.getDepartment().getName();
                    targetDeptIds.add(departmentId);
                }
            } catch (Exception ignored) {}

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_DEPARTMENT_STATE_CHANGED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .departmentId(departmentId)
                    .departmentName(departmentName)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(targetRoles)
                    .targetDepartmentIds(targetDeptIds)
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_DEPARTMENT_STATE_CHANGED event: {}", e.getMessage());
        }
    }

    public void publishProductChanged(VisitDepartment vd, String productName, Integer quantity, String action, Worker actor) {
        if (vd == null) return;
        try {
            Visit visit = vd.getVisit();
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("action", action);
            if (productName != null) metadata.put("productName", productName);
            if (quantity != null) metadata.put("quantity", quantity);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            UUID departmentId = null;
            String departmentName = null;
            try {
                if (vd.getDepartment() != null) {
                    departmentId = vd.getDepartment().getId();
                    departmentName = vd.getDepartment().getName();
                }
            } catch (Exception ignored) {}

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_PRODUCT_CHANGED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .departmentId(departmentId)
                    .departmentName(departmentName)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.CLINICIAN, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_PRODUCT_CHANGED event: {}", e.getMessage());
        }
    }

    public void publishNoteAdded(VisitDepartmentNote note, Worker actor) {
        if (note == null || note.getVisitDepartment() == null) return;
        try {
            VisitDepartment vd = note.getVisitDepartment();
            Visit visit = vd.getVisit();
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("noteId", note.getId());
            metadata.put("noteType", note.getNoteType() != null ? note.getNoteType().name() : "NOTE");

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            UUID departmentId = null;
            String departmentName = null;
            try {
                if (vd.getDepartment() != null) {
                    departmentId = vd.getDepartment().getId();
                    departmentName = vd.getDepartment().getName();
                }
            } catch (Exception ignored) {}

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_NOTE_ADDED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .departmentId(departmentId)
                    .departmentName(departmentName)
                    .departmentStatus(vd.getStatus())
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
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
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("action", action);
            if (insuranceName != null) metadata.put("insuranceName", insuranceName);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_INSURANCE_CHANGED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.FINANCE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .metadata(metadata)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_INSURANCE_CHANGED event: {}", e.getMessage());
        }
    }

    public void publishVitalsChanged(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_VITALS_CHANGED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.NURSE, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_VITALS_CHANGED event: {}", e.getMessage());
        }
    }

    public void publishBillingCompleted(Visit visit, Worker cashier) {
        if (visit == null) return;
        try {
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("BILLING_COMPLETED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(cashier != null ? cashier.getId() : null)
                    .actorName(cashier != null ? (cashier.getFirstName() + " " + cashier.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.FINANCE, RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting BILLING_COMPLETED event: {}", e.getMessage());
        }
    }

    public void publishVisitDischarged(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_DISCHARGED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_DISCHARGED event: {}", e.getMessage());
        }
    }

    public void publishVisitCancelled(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_CANCELLED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_CANCELLED event: {}", e.getMessage());
        }
    }

    public void publishVisitReopened(Visit visit, Worker actor) {
        if (visit == null) return;
        try {
            UUID visitId = safeGetVisitId(visit);
            if (visitId == null) return;

            UUID patientId = safeGetPatientId(visit);
            String patientName = safeGetPatientName(visit);
            VisitStatus visitStatus = safeGetVisitStatus(visit);

            Set<UUID> targetWorkerIds = extractAttendingWorkers(visitId);

            ClinicSseEvent event = ClinicSseEvent.builder()
                    .type("VISIT_REOPENED")
                    .visitId(visitId)
                    .patientId(patientId)
                    .patientName(patientName)
                    .visitStatus(visitStatus)
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null)
                    .targetRoles(Set.of(RoleName.RECEPTION, RoleName.CLINICIAN, RoleName.NURSE, RoleName.FINANCE, RoleName.ADMIN, RoleName.MANAGER, RoleName.CLINIC_ADMIN))
                    .targetWorkerIds(targetWorkerIds)
                    .build();

            sseRegistry.broadcast(event);
        } catch (Exception e) {
            log.warn("Error broadcasting VISIT_REOPENED event: {}", e.getMessage());
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

    private Set<UUID> extractAttendingWorkers(UUID visitId) {
        Set<UUID> workerIds = new HashSet<>();
        if (visitId == null) return workerIds;
        try {
            List<VisitDepartment> depts = visitDepartmentRepository.findByVisitId(visitId);
            for (VisitDepartment vd : depts) {
                if (vd == null) continue;
                try {
                    if (vd.getProcessors() != null) {
                        vd.getProcessors().forEach(p -> {
                            if (p != null && p.getId() != null) workerIds.add(p.getId());
                        });
                    }
                } catch (Exception ignored) {}
                try {
                    if (vd.getAddedBy() != null && vd.getAddedBy().getId() != null) {
                        workerIds.add(vd.getAddedBy().getId());
                    }
                } catch (Exception ignored) {}
                try {
                    if (vd.getCompletedBy() != null && vd.getCompletedBy().getId() != null) {
                        workerIds.add(vd.getCompletedBy().getId());
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return workerIds;
    }

    private UUID safeGetVisitId(Visit visit) {
        if (visit == null) return null;
        try {
            return visit.getId();
        } catch (Exception e) {
            return null;
        }
    }

    private UUID safeGetPatientId(Visit visit) {
        if (visit == null) return null;
        try {
            return visit.getPatient() != null ? visit.getPatient().getId() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String safeGetPatientName(Visit visit) {
        if (visit == null) return null;
        try {
            if (visit.getPatient() != null) {
                String first = visit.getPatient().getFirstName() != null ? visit.getPatient().getFirstName() : "";
                String last = visit.getPatient().getLastName() != null ? visit.getPatient().getLastName() : "";
                String full = (first + " " + last).trim();
                return full.isEmpty() ? null : full;
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    private VisitStatus safeGetVisitStatus(Visit visit) {
        if (visit == null) return null;
        try {
            return visit.getStatus();
        } catch (Exception e) {
            return null;
        }
    }
}
