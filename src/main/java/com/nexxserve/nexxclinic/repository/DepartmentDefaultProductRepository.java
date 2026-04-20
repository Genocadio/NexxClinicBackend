package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentDefaultProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentDefaultProductRepository extends JpaRepository<DepartmentDefaultProduct, UUID> {

    List<DepartmentDefaultProduct> findByDepartmentId(UUID departmentId);

    void deleteByDepartmentId(UUID departmentId);

    boolean existsByDepartmentIdAndProductId(UUID departmentId, UUID productId);

    boolean existsByDepartmentIdAndProductIdAndIdNot(UUID departmentId, UUID productId, UUID id);
}
