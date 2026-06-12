package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.AdminAuditLog;
import com.nexxserve.nexxclinic.repository.AdminAuditLogRepository;
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
        return Map.of(
                "id", log.getId(),
                "action", log.getActionType(),
                "details", log.getDetails(),
                "timestamp", log.getCreatedAt()
        );
    }
}
