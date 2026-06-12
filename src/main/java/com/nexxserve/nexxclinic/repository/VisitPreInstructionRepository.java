package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitPreInstruction;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitPreInstructionRepository extends JpaRepository<VisitPreInstruction, UUID> {
    List<VisitPreInstruction> findByVisitIdOrderByCreatedAtAsc(UUID visitId);
    List<VisitPreInstruction> findByVisitDepartmentIdOrderByCreatedAtAsc(UUID visitDepartmentId);
}
