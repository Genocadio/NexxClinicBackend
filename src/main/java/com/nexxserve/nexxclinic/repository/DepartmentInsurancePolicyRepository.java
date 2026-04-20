package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentInsurancePolicy;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentInsurancePolicyRepository extends JpaRepository<DepartmentInsurancePolicy, UUID> {

    List<DepartmentInsurancePolicy> findByDepartmentId(UUID departmentId);

    void deleteByDepartmentId(UUID departmentId);

    boolean existsByDepartmentIdAndInsuranceProviderId(UUID departmentId, UUID insuranceProviderId);

    boolean existsByDepartmentIdAndInsuranceProviderIdAndIdNot(UUID departmentId, UUID insuranceProviderId, UUID id);
}
