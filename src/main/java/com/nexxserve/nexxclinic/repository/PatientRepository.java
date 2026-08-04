package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Patient;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    /**
     * Locks the patient row for the duration of the transaction. Used by createVisit
     * to serialize concurrent visit creations for the same patient, so two racing
     * requests cannot both pass the duplicate-open-visit check and end up with two
     * active visits for one patient.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Patient p WHERE p.id = :patientId")
    Optional<Patient> findByIdForUpdate(@Param("patientId") UUID patientId);

    boolean existsByPatientIdentifier(String patientIdentifier);

    boolean existsByNationalIdNumber(String nationalIdNumber);

    boolean existsByPassportNumber(String passportNumber);

    boolean existsByNationalIdNumberAndIdNot(String nationalIdNumber, UUID id);

    boolean existsByPassportNumberAndIdNot(String passportNumber, UUID id);
}
