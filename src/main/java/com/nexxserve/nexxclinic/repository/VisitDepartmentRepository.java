package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitDepartmentRepository extends JpaRepository<VisitDepartment, UUID> {

    List<VisitDepartment> findByVisitId(UUID visitId);

    List<VisitDepartment> findByVisitIdAndParentVisitDepartmentIsNull(UUID visitId);

    List<VisitDepartment> findByParentVisitDepartmentId(UUID parentVisitDepartmentId);

    List<VisitDepartment> findByDepartmentIdIn(Collection<UUID> departmentIds);

    Optional<VisitDepartment> findByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    Optional<VisitDepartment> findByVisitIdAndDepartmentIdAndParentVisitDepartmentId(
            UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);

    boolean existsByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    boolean existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(
            UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);

    @Query("SELECT vd FROM VisitDepartment vd " +
            "WHERE vd.visit.patient.id = :patientId " +
            "AND vd.department.id = :departmentId " +
            "AND vd.parentVisitDepartment IS NULL " +
            "ORDER BY vd.visit.visitDate DESC, vd.visit.createdAt DESC")
    List<VisitDepartment> findLastByPatientIdAndDepartmentId(
            @Param("patientId") UUID patientId,
            @Param("departmentId") UUID departmentId,
            Pageable pageable);
}
