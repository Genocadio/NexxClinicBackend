package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitPriceEstimate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitPriceEstimateRepository extends JpaRepository<VisitPriceEstimate, UUID> {

    List<VisitPriceEstimate> findByVisitId(UUID visitId);

    Optional<VisitPriceEstimate> findByVisitDepartmentProductId(UUID visitDepartmentProductId);

    /** Delete all estimates for a visit (used when billing is created). */
    @Modifying
    void deleteByVisitId(UUID visitId);

    /** Delete a specific estimate by its visit department product ID. */
    void deleteByVisitDepartmentProductId(UUID visitDepartmentProductId);

    @Query("SELECT COALESCE(SUM(e.lineTotal), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumLineTotalByVisitId(UUID visitId);

    @Query("SELECT COALESCE(SUM(e.insuranceCoveredAmount), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumInsuranceCoveredByVisitId(UUID visitId);

    @Query("SELECT COALESCE(SUM(e.patientPayableAmount), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumPatientPayableByVisitId(UUID visitId);
}
