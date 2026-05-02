package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentFormVersion;
import com.nexxserve.nexxclinic.entity.FormStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentFormVersionRepository extends JpaRepository<DepartmentFormVersion, UUID> {
    List<DepartmentFormVersion> findByFormIdOrderByCreatedAtDesc(UUID formId);

    Optional<DepartmentFormVersion> findByFormIdAndVersionNumber(UUID formId, String versionNumber);

    @Query("SELECT fv FROM DepartmentFormVersion fv WHERE fv.form.id = :formId AND fv.status = :status ORDER BY fv.createdAt DESC LIMIT 1")
    Optional<DepartmentFormVersion> findLatestFinalVersionByFormId(
        @Param("formId") UUID formId,
        @Param("status") FormStatus status
    );

    @Query("SELECT fv FROM DepartmentFormVersion fv WHERE fv.form.id = :formId ORDER BY fv.createdAt DESC LIMIT 1")
    Optional<DepartmentFormVersion> findLatestVersionByFormId(@Param("formId") UUID formId);
}
