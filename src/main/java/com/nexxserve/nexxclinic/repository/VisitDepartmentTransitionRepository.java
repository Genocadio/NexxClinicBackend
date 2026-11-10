package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentTransition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VisitDepartmentTransitionRepository extends JpaRepository<VisitDepartmentTransition, UUID> {
    List<VisitDepartmentTransition> findByVisitIdOrderByOccurredAtAsc(UUID visitId);
    List<VisitDepartmentTransition> findByVisitDepartmentIdOrderByOccurredAtAsc(UUID visitDepartmentId);
}
