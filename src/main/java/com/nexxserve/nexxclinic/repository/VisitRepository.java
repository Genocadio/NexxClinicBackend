package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Visit;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, UUID>, JpaSpecificationExecutor<Visit> {

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("update Visit v set v.visitDate = :visitDate where v.id = :visitId")
	int updateVisitDate(@Param("visitId") UUID visitId, @Param("visitDate") LocalDate visitDate);

	@Query("SELECT v FROM Visit v WHERE v.patient.id = :patientId ORDER BY v.visitDate DESC, v.createdAt DESC LIMIT 1")
	Optional<Visit> findLatestVisitByPatientId(@Param("patientId") UUID patientId);
}
