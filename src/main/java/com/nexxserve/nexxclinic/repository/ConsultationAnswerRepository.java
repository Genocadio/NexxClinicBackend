package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.ConsultationAnswer;
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
}
