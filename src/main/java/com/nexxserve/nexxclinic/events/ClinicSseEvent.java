package com.nexxserve.nexxclinic.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ClinicSseEvent(
        UUID eventId,
        String type,
        Instant timestamp,
        UUID visitId,
        UUID patientId,
        String patientName,
        UUID departmentId,
        String departmentName,
        VisitDepartmentStatus departmentStatus,
        VisitStatus visitStatus,
        UUID actorId,
        String actorName,
        Set<RoleName> targetRoles,
        Set<UUID> targetDepartmentIds,
        Set<UUID> targetWorkerIds,
        Map<String, Object> metadata
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID eventId = UUID.randomUUID();
        private String type;
        private Instant timestamp = Instant.now();
        private UUID visitId;
        private UUID patientId;
        private String patientName;
        private UUID departmentId;
        private String departmentName;
        private VisitDepartmentStatus departmentStatus;
        private VisitStatus visitStatus;
        private UUID actorId;
        private String actorName;
        private Set<RoleName> targetRoles = Set.of();
        private Set<UUID> targetDepartmentIds = Set.of();
        private Set<UUID> targetWorkerIds = Set.of();
        private Map<String, Object> metadata;

        public Builder eventId(UUID eventId) { this.eventId = eventId; return this; }
        public Builder type(String type) { this.type = type; return this; }
        public Builder timestamp(Instant timestamp) { this.timestamp = timestamp; return this; }
        public Builder visitId(UUID visitId) { this.visitId = visitId; return this; }
        public Builder patientId(UUID patientId) { this.patientId = patientId; return this; }
        public Builder patientName(String patientName) { this.patientName = patientName; return this; }
        public Builder departmentId(UUID departmentId) { this.departmentId = departmentId; return this; }
        public Builder departmentName(String departmentName) { this.departmentName = departmentName; return this; }
        public Builder departmentStatus(VisitDepartmentStatus departmentStatus) { this.departmentStatus = departmentStatus; return this; }
        public Builder visitStatus(VisitStatus visitStatus) { this.visitStatus = visitStatus; return this; }
        public Builder actorId(UUID actorId) { this.actorId = actorId; return this; }
        public Builder actorName(String actorName) { this.actorName = actorName; return this; }
        public Builder targetRoles(Set<RoleName> targetRoles) { this.targetRoles = targetRoles != null ? targetRoles : Set.of(); return this; }
        public Builder targetDepartmentIds(Set<UUID> targetDepartmentIds) { this.targetDepartmentIds = targetDepartmentIds != null ? targetDepartmentIds : Set.of(); return this; }
        public Builder targetWorkerIds(Set<UUID> targetWorkerIds) { this.targetWorkerIds = targetWorkerIds != null ? targetWorkerIds : Set.of(); return this; }
        public Builder metadata(Map<String, Object> metadata) { this.metadata = metadata; return this; }

        public ClinicSseEvent build() {
            return new ClinicSseEvent(
                    eventId,
                    type,
                    timestamp,
                    visitId,
                    patientId,
                    patientName,
                    departmentId,
                    departmentName,
                    departmentStatus,
                    visitStatus,
                    actorId,
                    actorName,
                    targetRoles,
                    targetDepartmentIds,
                    targetWorkerIds,
                    metadata
            );
        }
    }
}
