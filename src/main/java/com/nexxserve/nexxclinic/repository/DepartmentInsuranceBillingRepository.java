package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    /**
     * Lightweight lookup of the owning visit's id for a {@link DepartmentInsuranceBilling}
     * row. Does NOT hydrate the billing entity, so it cannot poison the persistence
     * context with a stale snapshot before a per-visit lock is acquired (round-2 A3).
     */
    @Query("""
            SELECT vb.visit.id FROM DepartmentInsuranceBilling b
            JOIN b.visitDepartmentBilling db
            JOIN db.visitBilling vb
            WHERE b.id = :id
            """)
    Optional<UUID> findVisitIdById(@Param("id") UUID id);

    /**
     * Pessimistically locks the {@link DepartmentInsuranceBilling} row and re-reads its
     * CURRENT committed state (FOR UPDATE refreshes the persistence-context snapshot).
     * Used by {@code recordVisitBillingPayment} AFTER the per-visit lock so the
     * read-modify-write of paidAmount cannot lose a concurrent payment (round-2 A3).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT b FROM DepartmentInsuranceBilling b
            JOIN FETCH b.visitDepartmentBilling db
            JOIN FETCH db.visitDepartment vd
            JOIN FETCH db.visitBilling vb
            JOIN FETCH vb.visit v
            JOIN FETCH v.patient
            WHERE b.id = :id
            """)
    Optional<DepartmentInsuranceBilling> findByIdWithDepartmentBillingAndVisitForUpdate(@Param("id") UUID id);

    /**
     * Deep-fetches a {@link DepartmentInsuranceBilling} with every association the
     * invoice PDF renderer touches (visit department + its department, visit + patient,
     * patient insurance + provider). {@code generateInvoice} renders the PDF OUTSIDE any
     * transaction, so these lazy associations must be initialized here, inside a tx.
     */
    @Query("""
            SELECT b FROM DepartmentInsuranceBilling b
            JOIN FETCH b.visitDepartmentBilling db
            JOIN FETCH db.visitDepartment vd
            LEFT JOIN FETCH vd.department
            JOIN FETCH db.visitBilling vb
            JOIN FETCH vb.visit v
            JOIN FETCH v.patient
            LEFT JOIN FETCH b.patientInsurance pi
            LEFT JOIN FETCH pi.insuranceProvider
            WHERE b.id = :id
            """)
    Optional<DepartmentInsuranceBilling> findByIdWithInvoiceData(@Param("id") UUID id);

    /**
     * Returns every {@link DepartmentInsuranceBilling} row for a visit whose billing
     * version is NOT the supplied {@code versionId} (including rows without a version).
     * Used to invalidate invoices of previous billing versions when an edit creates a
     * new version, so stale PDFs can never be re-served.
     */
    @Query("""
            SELECT b FROM DepartmentInsuranceBilling b
            WHERE b.visitDepartmentBilling.visitBilling.visit.id = :visitId
              AND (b.billingVersion.id IS NULL OR b.billingVersion.id <> :versionId)
            """)
    List<DepartmentInsuranceBilling> findAllByVisitIdExcludingVersion(
            @Param("visitId") UUID visitId,
            @Param("versionId") UUID versionId
    );

    /**
     * Whether any billing bucket references the insurance and has a generated
     * invoice — used to decide if a patient insurance must be deactivated instead
     * of hard-deleted even when no visit link or billing item points at it.
     */
    boolean existsByPatientInsuranceIdAndInvoiceUrlIsNotNull(UUID patientInsuranceId);

    /**
     * Whether any billing bucket for the given visit references the specified patient insurance.
     */
    @Query("""
            SELECT COUNT(b) > 0 FROM DepartmentInsuranceBilling b
            WHERE b.visitDepartmentBilling.visitBilling.visit.id = :visitId
              AND b.patientInsurance.id = :patientInsuranceId
            """)
    boolean existsByVisitIdAndPatientInsuranceId(
            @Param("visitId") UUID visitId,
            @Param("patientInsuranceId") UUID patientInsuranceId
    );
}

