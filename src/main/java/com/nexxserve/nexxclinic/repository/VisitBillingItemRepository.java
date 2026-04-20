package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitBillingItemRepository extends JpaRepository<VisitBillingItem, UUID> {

    List<VisitBillingItem> findByVisitBillingId(UUID visitBillingId);
}
