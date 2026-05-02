package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentFormAnswer;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentFormAnswerRepository extends JpaRepository<DepartmentFormAnswer, UUID> {
    Optional<DepartmentFormAnswer> findByConsultationIdAndFormId(String consultationId, UUID formId);

    List<DepartmentFormAnswer> findByConsultationIdOrderByCreatedAtDesc(String consultationId);

    List<DepartmentFormAnswer> findByVisitIdOrderByCreatedAtDesc(String visitId);

    List<DepartmentFormAnswer> findByPatientIdOrderByCreatedAtDesc(String patientId);

    List<DepartmentFormAnswer> findByFormIdOrderByCreatedAtDesc(UUID formId);

    @Query("SELECT a FROM DepartmentFormAnswer a WHERE a.consultationId = :consultationId AND a.form.department.id = :departmentId")
    List<DepartmentFormAnswer> findByConsultationIdAndDepartmentId(
        @Param("consultationId") String consultationId,
        @Param("departmentId") UUID departmentId
    );
}
