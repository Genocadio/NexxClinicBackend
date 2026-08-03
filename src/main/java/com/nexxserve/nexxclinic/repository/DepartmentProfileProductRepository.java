package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentProfileProduct;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentProfileProductRepository extends JpaRepository<DepartmentProfileProduct, UUID> {

    List<DepartmentProfileProduct> findByProfileId(UUID profileId);

    void deleteByProfileId(UUID profileId);
}
