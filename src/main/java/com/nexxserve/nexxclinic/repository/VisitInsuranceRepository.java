package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitInsurance;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitInsuranceRepository extends JpaRepository<VisitInsurance, UUID> {

    List<VisitInsurance> findByVisitId(UUID visitId);

    void deleteByVisitId(UUID visitId);

    boolean existsByVisitIdAndPatientInsuranceId(UUID visitId, UUID patientInsuranceId);

    /**
     * Whether any visit links this patient insurance. Used as an FK guard before
     * hard-deleting a PatientInsurance (a linked insurance must never be deleted ->
     * would throw DataIntegrityViolationException -> 500).
     */
    boolean existsByPatientInsuranceId(UUID patientInsuranceId);
}
