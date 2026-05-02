package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentForm;
import com.nexxserve.nexxclinic.entity.FormStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentFormRepository extends JpaRepository<DepartmentForm, UUID> {
    List<DepartmentForm> findByDepartmentIdOrderByCreatedAtDesc(UUID departmentId);

    Optional<DepartmentForm> findByIdAndDepartmentId(UUID formId, UUID departmentId);

    boolean existsByIdAndDepartmentId(UUID formId, UUID departmentId);

    @Query("SELECT f FROM DepartmentForm f WHERE f.department.id = :departmentId ORDER BY f.createdAt DESC")
    List<DepartmentForm> findLatestFormsByDepartment(@Param("departmentId") UUID departmentId);
}
