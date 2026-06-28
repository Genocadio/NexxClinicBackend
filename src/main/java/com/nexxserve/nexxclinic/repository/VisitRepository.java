package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Visit;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, UUID>, JpaSpecificationExecutor<Visit> {

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
