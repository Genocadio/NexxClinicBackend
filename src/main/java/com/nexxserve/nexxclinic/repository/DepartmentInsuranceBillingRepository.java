package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DepartmentInsuranceBillingRepository extends JpaRepository<DepartmentInsuranceBilling, UUID> {

    @Query("""
            SELECT b FROM DepartmentInsuranceBilling b
            JOIN FETCH b.visitDepartmentBilling db
            JOIN FETCH db.visitDepartment vd
            JOIN FETCH db.visitBilling vb
            JOIN FETCH vb.visit v
            JOIN FETCH v.patient
            WHERE b.id = :id
            """)
    Optional<DepartmentInsuranceBilling> findByIdWithDepartmentBillingAndVisit(@Param("id") UUID id);

    @Query("""
            SELECT b FROM DepartmentInsuranceBilling b
            JOIN FETCH b.visitDepartmentBilling db
            JOIN FETCH db.visitDepartment vd
            JOIN FETCH vd.department
            JOIN FETCH db.visitBilling vb
            JOIN FETCH vb.visit v
            JOIN FETCH v.patient
            LEFT JOIN FETCH b.patientInsurance pi
            LEFT JOIN FETCH pi.insuranceProvider
            WHERE v.id = :visitId
            ORDER BY b.createdAt ASC
            """)
    List<DepartmentInsuranceBilling> findAllByVisitIdWithDetails(@Param("visitId") UUID visitId);
}
