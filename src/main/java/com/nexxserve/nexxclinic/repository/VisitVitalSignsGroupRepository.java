package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitVitalSignsGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitVitalSignsGroupRepository extends JpaRepository<VisitVitalSignsGroup, UUID> {
    List<VisitVitalSignsGroup> findByVisitIdOrderByCreatedAtAsc(UUID visitId);
}