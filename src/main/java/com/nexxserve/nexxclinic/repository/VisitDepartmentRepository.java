package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentRepository extends JpaRepository<VisitDepartment, UUID> {

    List<VisitDepartment> findByVisitIdOrderByCreatedAtAsc(UUID visitId);

    Optional<VisitDepartment> findFirstByVisitIdAndStatusOrderByCreatedAtAsc(UUID visitId, VisitDepartmentStatus status);

    Optional<VisitDepartment> findByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    boolean existsByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    boolean existsByVisitIdAndStatus(UUID visitId, VisitDepartmentStatus status);
}
