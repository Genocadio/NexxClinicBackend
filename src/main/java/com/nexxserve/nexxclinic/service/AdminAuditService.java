package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.AdminAuditLog;
import com.nexxserve.nexxclinic.repository.AdminAuditLogRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminAuditService(AdminAuditLogRepository adminAuditLogRepository) {
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @Transactional
    public void logAdminAction(AuthenticatedUser admin, UUID targetUserId, String actionType, String details) {
        if (admin == null) {
            return;
        }

        AdminAuditLog log = new AdminAuditLog();
        log.setAdminUserId(admin.userId());
        log.setTargetUserId(targetUserId);
        log.setActionType(actionType);
        log.setDetails(details);
        adminAuditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> latestAuditLogs() {
        return adminAuditLogRepository.findTop100ByOrderByCreatedAtDesc().stream()
                .map(this::toAuditLogView)
                .toList();
    }

    private Map<String, Object> toAuditLogView(AdminAuditLog log) {
        // S8 fix: Map.of throws NullPointerException on any null value. `details` is a
        // nullable column, so one legacy/edge row with null details would 500 the whole
        // adminAuditLogs query. Build the map null-safely instead.
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", log.getId());
        view.put("action", log.getActionType());
        view.put("details", log.getDetails());
        view.put("timestamp", log.getCreatedAt());
        return view;
    }
}
