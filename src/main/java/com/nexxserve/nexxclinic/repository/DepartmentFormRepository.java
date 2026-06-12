package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentForm;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentFormRepository extends JpaRepository<DepartmentForm, UUID> {

    List<DepartmentForm> findByDepartmentIdOrderByUpdatedAtDesc(UUID departmentId);

    Optional<DepartmentForm> findByIdAndDepartmentId(UUID id, UUID departmentId);

    Optional<DepartmentForm> findTopByDepartmentIdOrderByUpdatedAtDesc(UUID departmentId);
}
