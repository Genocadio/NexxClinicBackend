package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Visit;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, UUID>, JpaSpecificationExecutor<Visit> {

	/**
	 * Whether the patient has any visit rows. Used as an FK guard before hard-deleting
	 * a patient (a patient with visits must never be deleted -> would throw
	 * DataIntegrityViolationException -> 500).
	 */
	boolean existsByPatientId(UUID patientId);

	/**
	 * Locks the visit row for the duration of the transaction. Used to serialize all
	 * billing operations (billVisit, editBillVisit, recordVisitBillingPayment) per visit
	 * so concurrent calls cannot race on the version counter, product billing, or payment
	 * paidAmount (round-2 review findings A1–A4).
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT v FROM Visit v WHERE v.id = :visitId")
	Optional<Visit> findByIdForUpdate(@Param("visitId") UUID visitId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Visit v set v.visitDate = :visitDate where v.id = :visitId")
	int updateVisitDate(@Param("visitId") UUID visitId, @Param("visitDate") LocalDateTime visitDate);

	@Query("SELECT v FROM Visit v WHERE v.patient.id = :patientId ORDER BY v.visitDate DESC, v.createdAt DESC")
	List<Visit> findLastVisitsByPatientId(@Param("patientId") UUID patientId, Pageable pageable);

	@Query("""
		SELECT v FROM Visit v
		WHERE v.patient.id = :patientId
		AND (
			v.visitDate < :referenceVisitDate
			OR (v.visitDate = :referenceVisitDate AND v.createdAt < :referenceCreatedAt)
			OR (v.visitDate = :referenceVisitDate AND v.createdAt = :referenceCreatedAt AND v.id <> :referenceVisitId)
		)
		ORDER BY v.visitDate DESC, v.createdAt DESC
		""")
	List<Visit> findPreviousVisitsByPatientId(
			@Param("patientId") UUID patientId,
			@Param("referenceVisitDate") LocalDateTime referenceVisitDate,
			@Param("referenceCreatedAt") LocalDateTime referenceCreatedAt,
			@Param("referenceVisitId") UUID referenceVisitId,
			Pageable pageable);
}
