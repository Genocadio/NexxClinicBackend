package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBillingPayment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBillingPaymentRepository extends JpaRepository<VisitBillingPayment, UUID> {

    /**
     * Returns every payment bucket-attributed to a visit department, eagerly
     * loading the attributed {@link DepartmentInsuranceBilling} (+ its insurance),
     * so callers can traverse the bucket outside a transaction.
     */
    @Query("""
            SELECT p FROM VisitBillingPayment p
            LEFT JOIN FETCH p.departmentInsuranceBilling dib
            LEFT JOIN FETCH dib.patientInsurance
            JOIN p.visitDepartmentBilling vdb
            JOIN vdb.visitDepartment vd
            WHERE vd.id = :visitDepartmentId
            """)
    List<VisitBillingPayment> findByVisitDepartmentIdWithBuckets(@Param("visitDepartmentId") UUID visitDepartmentId);
}