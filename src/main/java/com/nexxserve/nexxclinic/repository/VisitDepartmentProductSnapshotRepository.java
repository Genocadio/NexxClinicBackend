package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentProductSnapshotRepository extends JpaRepository<VisitDepartmentProductSnapshot, UUID> {

    List<VisitDepartmentProductSnapshot> findByBillingVersionId(UUID billingVersionId);

    /**
     * Returns every snapshot for a visit department product id (stored as a plain
     * UUID, not an FK). Used by regression tests to assert that a failed bill
     * leaves no immutable-history writes behind.
     */
    List<VisitDepartmentProductSnapshot> findByVisitDepartmentProductId(UUID visitDepartmentProductId);
}
