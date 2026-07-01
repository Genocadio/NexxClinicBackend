package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitBillingVersionRepository extends JpaRepository<VisitBillingVersion, UUID> {

    List<VisitBillingVersion> findByVisitIdOrderByVersionDesc(UUID visitId);

    Optional<VisitBillingVersion> findFirstByVisitIdOrderByVersionDesc(UUID visitId);
}
