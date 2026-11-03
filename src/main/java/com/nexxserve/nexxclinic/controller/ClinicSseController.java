package com.nexxserve.nexxclinic.controller;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.events.ClinicEventPublisher;
import com.nexxserve.nexxclinic.events.ClinicSseRegistry;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/events")
public class ClinicSseController {

    private static final Logger log = LoggerFactory.getLogger(ClinicSseController.class);

    private final ClinicSseRegistry sseRegistry;
    private final WorkerRepository workerRepository;
    private final ClinicEventPublisher clinicEventPublisher;

    public ClinicSseController(ClinicSseRegistry sseRegistry, WorkerRepository workerRepository, ClinicEventPublisher clinicEventPublisher) {
        this.sseRegistry = sseRegistry;
        this.workerRepository = workerRepository;
        this.clinicEventPublisher = clinicEventPublisher;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Transactional(readOnly = true)
    public SseEmitter streamEvents(@AuthenticationPrincipal AuthenticatedUser user) {
        if (user == null || user.userId() == null) {
            throw new IllegalArgumentException("Authentication required to connect to SSE stream");
        }

        Set<UUID> departmentIds = new HashSet<>();
        Optional<Worker> workerOpt = workerRepository.findById(user.userId());
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            if (worker.getDepartment() != null) {
                departmentIds.add(worker.getDepartment().getId());
            }
            if (worker.getDepartments() != null) {
                worker.getDepartments().forEach(d -> departmentIds.add(d.getId()));
            }
        }

        log.debug("User {} (roles: {}, depts: {}) opened SSE event stream",
                user.userId(), user.roles(), departmentIds);

        return sseRegistry.register(user.userId(), user.roles(), departmentIds);
    }

    @PostMapping("/presence")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> updatePresence(
            @RequestParam UUID visitId,
            @RequestParam(defaultValue = "true") boolean active,
            @RequestParam(required = false) String activity,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        if (user == null || user.userId() == null || visitId == null) {
            return ApiResponse.error("Authentication and visitId are required.");
        }

        String workerName = user.principal();
        String primaryRole = user.roles() != null && !user.roles().isEmpty() ? user.roles().iterator().next().name() : "STAFF";

        Optional<Worker> workerOpt = workerRepository.findById(user.userId());
        if (workerOpt.isPresent()) {
            Worker worker = workerOpt.get();
            workerName = (worker.getFirstName() != null ? worker.getFirstName() : "") + " " + (worker.getLastName() != null ? worker.getLastName() : "");
            workerName = workerName.trim();
        }

        List<Map<String, Object>> activeViewers = sseRegistry.updatePresence(visitId, user.userId(), workerName, primaryRole, active, activity);
        clinicEventPublisher.publishPresenceChanged(visitId, activeViewers);

        return ApiResponse.success("Presence updated.", activeViewers);
    }

    @GetMapping("/presence")
    public ApiResponse<List<Map<String, Object>>> getPresence(@RequestParam UUID visitId) {
        if (visitId == null) {
            return ApiResponse.error("visitId is required.");
        }
        return ApiResponse.success("Presence fetched.", sseRegistry.getPresence(visitId));
    }
}
