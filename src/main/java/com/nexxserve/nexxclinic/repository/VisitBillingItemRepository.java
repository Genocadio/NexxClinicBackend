package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBillingItemRepository extends JpaRepository<VisitBillingItem, UUID> {

    List<VisitBillingItem> findByDepartmentInsuranceBillingId(UUID departmentInsuranceBillingId);

    List<VisitBillingItem> findByDepartmentInsuranceBillingVisitDepartmentBillingVisitBillingId(UUID visitBillingId);

    @Query("""
            SELECT i FROM VisitBillingItem i
            JOIN FETCH i.visitDepartmentProduct vdp
            JOIN FETCH vdp.product
            WHERE i.departmentInsuranceBilling.id = :billingId
            """)
    List<VisitBillingItem> findByDepartmentInsuranceBillingIdWithProduct(@Param("billingId") UUID billingId);

    @Query("""
            SELECT i FROM VisitBillingItem i
            JOIN FETCH i.visitDepartmentProduct vdp
            JOIN FETCH vdp.product
            WHERE i.departmentInsuranceBilling.visitDepartmentBilling.visitBilling.id = :billingId
            """)
    List<VisitBillingItem> findByVisitBillingIdWithProduct(@Param("billingId") UUID billingId);
}
