package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.AdminAuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, UUID> {

    List<AdminAuditLog> findTop100ByOrderByCreatedAtDesc();
}
