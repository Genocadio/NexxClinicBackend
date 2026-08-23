package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.InsuranceCoverage;
import com.nexxserve.nexxclinic.model.EncounterType;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, UUID> {

    /**
     * Exact match: provider + department + encounter type.
     */
    Optional<InsuranceCoverage> findByInsuranceProviderIdAndDepartmentIdAndEncounterType(
        UUID insuranceProviderId,
        UUID departmentId,
        EncounterType encounterType
    );

    /**
     * Department-level fallback: provider + department (encounterType = null).
     */
    Optional<InsuranceCoverage> findByInsuranceProviderIdAndDepartmentIdAndEncounterTypeIsNull(
        UUID insuranceProviderId,
        UUID departmentId
    );

    /**
     * Provider-wide override: provider + null department + null encounterType.
     */
    Optional<InsuranceCoverage> findByInsuranceProviderIdAndDepartmentIdIsNullAndEncounterTypeIsNull(
        UUID insuranceProviderId
    );

    /**
     * Bulk prefetch: all rules for a set of insurance providers.
     */
    List<InsuranceCoverage> findByInsuranceProviderIdIn(Set<UUID> insuranceProviderIds);

    /**
     * All rules for a specific insurance provider.
     */
    List<InsuranceCoverage> findByInsuranceProviderIdOrderByDepartmentNameAscEncounterTypeAsc(
        UUID insuranceProviderId
    );

    /**
     * All rules for a specific insurance provider and department.
     */
    List<InsuranceCoverage> findByInsuranceProviderIdAndDepartmentIdOrderByEncounterTypeAsc(
        UUID insuranceProviderId,
        UUID departmentId
    );

    /**
     * Bulk prefetch filtered by provider and department sets.
     */
    @Query("SELECT r FROM InsuranceCoverage r WHERE r.insuranceProvider.id IN :providerIds AND (r.department.id IN :departmentIds OR r.department IS NULL)")
    List<InsuranceCoverage> findByProviderIdsAndDepartmentIds(
        @Param("providerIds") Set<UUID> providerIds,
        @Param("departmentIds") Set<UUID> departmentIds
    );

    long countByInsuranceProviderId(UUID insuranceProviderId);
}
