package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Patient;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PatientRepository extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {

    boolean existsByPatientIdentifier(String patientIdentifier);

    boolean existsByNationalIdNumber(String nationalIdNumber);

    boolean existsByPassportNumber(String passportNumber);

    boolean existsByNationalIdNumberAndIdNot(String nationalIdNumber, UUID id);

    boolean existsByPassportNumberAndIdNot(String passportNumber, UUID id);
}
