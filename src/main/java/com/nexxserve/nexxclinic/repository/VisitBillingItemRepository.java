package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBillingItemRepository extends JpaRepository<VisitBillingItem, UUID> {

    List<VisitBillingItem> findByVisitBillingId(UUID visitBillingId);

    @Query("""
            SELECT i FROM VisitBillingItem i
            JOIN FETCH i.visitDepartmentProduct vdp
            JOIN FETCH vdp.product
            WHERE i.visitBilling.id = :billingId
            """)
    List<VisitBillingItem> findByVisitBillingIdWithProduct(@Param("billingId") UUID billingId);
}
