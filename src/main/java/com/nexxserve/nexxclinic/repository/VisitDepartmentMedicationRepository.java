package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentMedication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitDepartmentMedicationRepository extends JpaRepository<VisitDepartmentMedication, UUID> {
    List<VisitDepartmentMedication> findByVisitDepartmentId(UUID visitDepartmentId);
}
