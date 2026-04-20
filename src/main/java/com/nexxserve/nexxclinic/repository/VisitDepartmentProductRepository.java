package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentProductRepository extends JpaRepository<VisitDepartmentProduct, UUID> {

    List<VisitDepartmentProduct> findByVisitDepartmentId(UUID visitDepartmentId);

    Optional<VisitDepartmentProduct> findByVisitDepartmentIdAndProductId(UUID visitDepartmentId, UUID productId);
}
