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

    /**
     * Delete all estimates for a visit.
     *
     * <p>Uses a bulk JPQL DELETE with {@code flushAutomatically = true} so that
     * any pending inserts (e.g. a VisitPriceEstimate created earlier in the same
     * transaction) are written to the DB before the DELETE runs. Without the
     * flush, Hibernate can leave those inserts in the write-behind queue and
     * attempt to re-insert them after the DELETE via {@code saveAll}, which
     * triggers the {@code uk_visit_price_estimate_product} unique constraint.
     * {@code clearAutomatically = true} then clears the L1 cache so the
     * subsequent {@code saveAll} starts with a clean slate.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM visit_price_estimates WHERE visit_id = :visitId", nativeQuery = true)
    void deleteByVisitId(@org.springframework.data.repository.query.Param("visitId") UUID visitId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM visit_price_estimates WHERE visit_department_product_id IN (SELECT id FROM visit_department_products WHERE visit_department_id = :visitDepartmentId)", nativeQuery = true)
    void deleteByVisitDepartmentId(@org.springframework.data.repository.query.Param("visitDepartmentId") UUID visitDepartmentId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = "DELETE FROM visit_price_estimates WHERE visit_department_product_id = :visitDepartmentProductId", nativeQuery = true)
    void deleteByVisitDepartmentProductId(@org.springframework.data.repository.query.Param("visitDepartmentProductId") UUID visitDepartmentProductId);

    @Query("SELECT COALESCE(SUM(e.lineTotal), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumLineTotalByVisitId(UUID visitId);

    @Query("SELECT COALESCE(SUM(e.insuranceCoveredAmount), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumInsuranceCoveredByVisitId(UUID visitId);

    @Query("SELECT COALESCE(SUM(e.patientPayableAmount), 0) FROM VisitPriceEstimate e WHERE e.visit.id = :visitId")
    java.math.BigDecimal sumPatientPayableByVisitId(UUID visitId);
}
