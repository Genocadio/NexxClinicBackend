package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.StandaloneFormVersion;
import com.nexxserve.nexxclinic.model.FormStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StandaloneFormVersionRepository extends JpaRepository<StandaloneFormVersion, UUID> {
    List<StandaloneFormVersion> findByFormIdOrderByCreatedAtDesc(UUID formId);
    Optional<StandaloneFormVersion> findTopByFormIdOrderByMajorVersionDescMinorVersionDesc(UUID formId);
    Optional<StandaloneFormVersion> findTopByFormIdAndStatusOrderByMajorVersionDescMinorVersionDesc(UUID formId, FormStatus status);
}
