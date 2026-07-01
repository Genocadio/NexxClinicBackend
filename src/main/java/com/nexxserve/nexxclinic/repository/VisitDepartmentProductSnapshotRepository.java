package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentProductSnapshotRepository extends JpaRepository<VisitDepartmentProductSnapshot, UUID> {

    List<VisitDepartmentProductSnapshot> findByBillingVersionId(UUID billingVersionId);
}
