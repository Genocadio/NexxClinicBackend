package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentProfileRepository extends JpaRepository<DepartmentProfile, UUID> {

    List<DepartmentProfile> findByDepartmentId(UUID departmentId);

    Optional<DepartmentProfile> findFirstByDepartmentIdAndIsDefaultTrue(UUID departmentId);
}
