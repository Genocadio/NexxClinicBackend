package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.DepartmentFormVersion;
import com.nexxserve.nexxclinic.model.FormStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentFormVersionRepository extends JpaRepository<DepartmentFormVersion, UUID> {

    List<DepartmentFormVersion> findByFormIdOrderByCreatedAtDesc(UUID formId);

    Optional<DepartmentFormVersion> findByFormIdAndVersionNumber(UUID formId, String versionNumber);

    Optional<DepartmentFormVersion> findTopByFormIdAndStatusOrderByCreatedAtDesc(UUID formId, FormStatus status);
}
