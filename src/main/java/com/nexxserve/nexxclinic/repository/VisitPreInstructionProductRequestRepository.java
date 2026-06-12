package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitPreInstructionProductRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitPreInstructionProductRequestRepository extends JpaRepository<VisitPreInstructionProductRequest, UUID> {
    List<VisitPreInstructionProductRequest> findByPreInstructionId(UUID preInstructionId);
}
