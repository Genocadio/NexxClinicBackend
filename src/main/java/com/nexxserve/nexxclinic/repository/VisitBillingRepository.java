package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitBilling;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitBillingRepository extends JpaRepository<VisitBilling, UUID> {

    List<VisitBilling> findByVisitIdOrderByCreatedAtDesc(UUID visitId);

    @Query("""
            SELECT b FROM VisitBilling b
            JOIN FETCH b.visit v
            JOIN FETCH v.patient
            WHERE b.id = :id
            """)
    Optional<VisitBilling> findByIdWithVisitAndPatient(@Param("id") UUID id);
}
