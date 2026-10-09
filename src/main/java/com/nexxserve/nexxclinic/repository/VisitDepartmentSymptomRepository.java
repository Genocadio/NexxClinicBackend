package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentSymptom;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitDepartmentSymptomRepository extends JpaRepository<VisitDepartmentSymptom, UUID> {
    List<VisitDepartmentSymptom> findByVisitDepartmentId(UUID visitDepartmentId);
    List<VisitDepartmentSymptom> findByVisitDepartmentIdIn(Collection<UUID> visitDepartmentIds);
}
