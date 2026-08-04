package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentBillingRepository extends JpaRepository<VisitDepartmentBilling, UUID> {

    /**
     * Whether this visit department has ever been included in a billing container
     * (any version). Used by {@code updateVisitDepartmentStatus} to distinguish a
     * pre-billing BILLING-status department (can still be reopened) from one that has
     * actual billing history (frozen — editBillVisit only).
     */
    boolean existsByVisitDepartmentId(UUID visitDepartmentId);

    java.util.List<VisitDepartmentBilling> findByVisitDepartmentId(UUID visitDepartmentId);
}
