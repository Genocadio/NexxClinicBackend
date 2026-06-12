package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InsuranceProviderRepository extends JpaRepository<InsuranceProvider, UUID>, JpaSpecificationExecutor<InsuranceProvider> {

    boolean existsByInsuranceNameIgnoreCase(String insuranceName);

    boolean existsByInsuranceNameIgnoreCaseAndIdNot(String insuranceName, UUID id);

    boolean existsByAcronymIgnoreCase(String acronym);

    boolean existsByAcronymIgnoreCaseAndIdNot(String acronym, UUID id);
}
