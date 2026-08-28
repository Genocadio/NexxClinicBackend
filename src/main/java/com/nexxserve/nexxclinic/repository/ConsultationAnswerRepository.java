package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.ConsultationAnswer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultationAnswerRepository extends JpaRepository<ConsultationAnswer, UUID> {

        Optional<ConsultationAnswer> findByConsultationIdAndFormIdAndFormVersion(
            UUID consultationId,
            UUID formId,
            String formVersion
    );

    Optional<ConsultationAnswer> findTopByConsultationIdAndFormIdOrderByUpdatedAtDesc(UUID consultationId, UUID formId);

    List<ConsultationAnswer> findByConsultationIdAndDepartmentIdAndFormIdOrderByUpdatedAtDesc(
            UUID consultationId,
            UUID departmentId,
            UUID formId
    );

    List<ConsultationAnswer> findByVisitIdAndDepartmentIdOrderByUpdatedAtDesc(
            UUID visitId,
            UUID departmentId
    );

    /**
     * Find the latest (most recently updated) consultation answer for a given
     * visit + department combination.
     */
    Optional<ConsultationAnswer> findTopByVisitIdAndDepartmentIdOrderByUpdatedAtDesc(
            UUID visitId,
            UUID departmentId
    );

    /**
     * Find the latest consultation answer for a given visit (across all departments).
     */
    List<ConsultationAnswer> findByVisitIdOrderByUpdatedAtDesc(UUID visitId);
}
