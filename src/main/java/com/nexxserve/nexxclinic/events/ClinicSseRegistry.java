package com.nexxserve.nexxclinic.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.model.RoleName;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
public class ClinicSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(ClinicSseRegistry.class);
    private static final Long EMITTER_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    private final ObjectMapper objectMapper;
    private final Map<String, ClientSession> sessions = new ConcurrentHashMap<>();

    public record ViewerInfo(
            UUID workerId,
            String workerName,
            String role,
            String activity,
            long lastActiveAt
    ) {}

    private final Map<UUID, Map<UUID, ViewerInfo>> visitPresence = new ConcurrentHashMap<>();

    public ClinicSseRegistry() {
        this(createDefaultObjectMapper());
    }

    public ClinicSseRegistry(ObjectMapper objectMapper) {
        ObjectMapper mapper = objectMapper != null ? objectMapper.copy() : createDefaultObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.objectMapper = mapper;
    }

    private static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.findAndRegisterModules();
        return mapper;
    }

    public record ClientSession(
            String sessionId,
            SseEmitter emitter,
            UUID workerId,
            Set<RoleName> roles,
            Set<UUID> departmentIds
    ) {}

    public SseEmitter register(UUID workerId, Set<RoleName> roles, Set<UUID> departmentIds) {
        String sessionId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);

        ClientSession session = new ClientSession(
                sessionId,
                emitter,
                workerId,
                roles != null ? Set.copyOf(roles) : Set.of(),
                departmentIds != null ? Set.copyOf(departmentIds) : Set.of()
        );

        sessions.put(sessionId, session);
        log.debug("Registered SSE client session {} for worker {} (roles: {}, depts: {})",
                sessionId, workerId, session.roles(), session.departmentIds());

        emitter.onCompletion(() -> {
            sessions.remove(sessionId);
            log.debug("SSE session {} completed", sessionId);
        });
        emitter.onTimeout(() -> {
            sessions.remove(sessionId);
            log.debug("SSE session {} timed out", sessionId);
        });
        emitter.onError(e -> {
            sessions.remove(sessionId);
            log.debug("SSE session {} encountered error: {}", sessionId, e.getMessage());
        });

        // Send initial connected confirmation ping
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECTED")
                    .data("{\"status\":\"CONNECTED\"}"));
        } catch (IOException e) {
            sessions.remove(sessionId);
        }

        return emitter;
    }

    public List<Map<String, Object>> updatePresence(UUID visitId, UUID workerId, String workerName, String role, boolean active) {
        return updatePresence(visitId, workerId, workerName, role, active, null);
    }

    public List<Map<String, Object>> updatePresence(UUID visitId, UUID workerId, String workerName, String role, boolean active, String activity) {
        if (visitId == null || workerId == null) {
            return List.of();
        }

        Map<UUID, ViewerInfo> viewers = visitPresence.computeIfAbsent(visitId, k -> new ConcurrentHashMap<>());
        if (active) {
            String act = (activity != null && !activity.isBlank()) ? activity.trim() : "viewing";
            viewers.put(workerId, new ViewerInfo(workerId, workerName, role, act, System.currentTimeMillis()));
        } else {
            viewers.remove(workerId);
        }

        // Clean stale viewers (> 3 mins inactivity)
        long cutoff = System.currentTimeMillis() - 180000;
        viewers.entrySet().removeIf(e -> e.getValue().lastActiveAt() < cutoff);

        return formatViewers(viewers);
    }

    public List<Map<String, Object>> getPresence(UUID visitId) {
        if (visitId == null) return List.of();
        Map<UUID, ViewerInfo> viewers = visitPresence.get(visitId);
        if (viewers == null) return List.of();
        return formatViewers(viewers);
    }

    private List<Map<String, Object>> formatViewers(Map<UUID, ViewerInfo> viewers) {
        return viewers.values().stream()
                .map(v -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("workerId", v.workerId().toString());
                    map.put("workerName", v.workerName());
                    map.put("role", v.role());
                    map.put("activity", v.activity());
                    return map;
                })
                .toList();
    }

    public void broadcast(ClinicSseEvent event) {
        if (sessions.isEmpty()) {
            log.debug("Dropping SSE event {} because there are no connected clients", event.type());
            return;
        }

        int connectedCount = sessions.size();
        AtomicInteger deliveredCount = new AtomicInteger();
        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize SSE event {}", event.type(), e);
            return;
        }

        sessions.forEach((sessionId, session) -> {
            if (shouldDeliver(session, event)) {
                try {
                    session.emitter().send(SseEmitter.event()
                            .id(event.eventId().toString())
                            .name(event.type())
                            .data(jsonPayload));
                    deliveredCount.incrementAndGet();
                } catch (Exception e) {
                    log.debug("Failed to send SSE event to session {}, removing", sessionId);
                    session.emitter().complete();
                    sessions.remove(sessionId);
                }
            }
        });
        log.debug("SSE event {} delivered to {} of {} connected session(s)",
                event.type(), deliveredCount.get(), connectedCount);
    }

    private boolean shouldDeliver(ClientSession session, ClinicSseEvent event) {
        // Presence events are broadcast to all relevant connected clients for that visit
        if ("VISIT_PRESENCE_CHANGED".equals(event.type())) {
            return true;
        }

        // 1. If worker is explicitly targeted (e.g. dedicated processor or creator of discharged visit)
        if (event.targetWorkerIds() != null && !event.targetWorkerIds().isEmpty()) {
            if (session.workerId() != null && event.targetWorkerIds().contains(session.workerId())) {
                return true;
            }
        }

        // 2. Admins and managers receive all clinic events
        if (session.roles().contains(RoleName.ADMIN) || session.roles().contains(RoleName.MANAGER)) {
            return true;
        }

        // 3. If target roles is empty or contains client role
        boolean matchesRole = false;
        if (event.targetRoles() == null || event.targetRoles().isEmpty()) {
            matchesRole = true;
        } else {
            for (RoleName targetRole : event.targetRoles()) {
                if (session.roles().contains(targetRole)) {
                    matchesRole = true;
                    break;
                }
            }
        }

        if (!matchesRole) {
            return false;
        }

        // 4. Department filter: if event specifies target departments, client must belong to one of them
        // (unless client is Finance, Reception, or Admin who monitor clinic-wide)
        if (event.targetDepartmentIds() != null && !event.targetDepartmentIds().isEmpty()) {
            if (session.roles().contains(RoleName.FINANCE)
                    || session.roles().contains(RoleName.RECEPTION)) {
                return true;
            }
            return event.targetDepartmentIds().stream().anyMatch(session.departmentIds()::contains);
        }

        return true;
    }

    @Scheduled(fixedDelay = 25000)
    public void sendHeartbeat() {
        if (sessions.isEmpty()) {
            return;
        }
        sessions.forEach((sessionId, session) -> {
            try {
                session.emitter().send(SseEmitter.event()
                        .name("PING")
                        .data("{\"timestamp\":" + System.currentTimeMillis() + "}"));
            } catch (Exception e) {
                sessions.remove(sessionId);
            }
        });
    }
}
