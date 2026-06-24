package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentStandaloneForm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentStandaloneFormRepository extends JpaRepository<DepartmentStandaloneForm, UUID> {
    List<DepartmentStandaloneForm> findByDepartmentId(UUID departmentId);
    Optional<DepartmentStandaloneForm> findByDepartmentIdAndStandaloneFormId(UUID departmentId, UUID standaloneFormId);
    void deleteByDepartmentIdAndStandaloneFormId(UUID departmentId, UUID standaloneFormId);

    @Modifying
    @Query("UPDATE DepartmentStandaloneForm dsf SET dsf.isDefault = false WHERE dsf.department.id = :departmentId")
    void clearDefaultByDepartmentId(@Param("departmentId") UUID departmentId);

    Optional<DepartmentStandaloneForm> findByDepartmentIdAndIsDefaultTrue(UUID departmentId);
}
