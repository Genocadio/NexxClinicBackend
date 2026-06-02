package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentBillingRepository extends JpaRepository<VisitDepartmentBilling, UUID> {
}
