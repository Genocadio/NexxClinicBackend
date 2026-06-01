package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentRepository extends JpaRepository<VisitDepartment, UUID> {

    List<VisitDepartment> findByVisitId(UUID visitId);

    List<VisitDepartment> findByVisitIdAndParentVisitDepartmentIsNull(UUID visitId);

    List<VisitDepartment> findByParentVisitDepartmentId(UUID parentVisitDepartmentId);

    List<VisitDepartment> findByDepartmentIdIn(Collection<UUID> departmentIds);

    Optional<VisitDepartment> findByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    Optional<VisitDepartment> findByVisitIdAndDepartmentIdAndParentVisitDepartmentId(UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);

    boolean existsByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    boolean existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);
}
