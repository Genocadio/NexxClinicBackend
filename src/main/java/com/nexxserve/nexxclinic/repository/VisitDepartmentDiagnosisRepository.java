package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitDepartmentDiagnosisRepository extends JpaRepository<VisitDepartmentDiagnosis, UUID> {
    List<VisitDepartmentDiagnosis> findByVisitDepartmentId(UUID visitDepartmentId);
}
