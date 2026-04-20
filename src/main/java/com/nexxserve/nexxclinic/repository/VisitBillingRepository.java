package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBilling;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBillingRepository extends JpaRepository<VisitBilling, UUID> {

    List<VisitBilling> findByVisitIdOrderByCreatedAtDesc(UUID visitId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update VisitBilling vb set vb.billingDate = :billingDate where vb.visit.id = :visitId")
    int updateBillingDateByVisitId(@Param("visitId") UUID visitId, @Param("billingDate") LocalDate billingDate);
}
