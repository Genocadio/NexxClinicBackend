package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.ClinicProfile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClinicProfileRepository extends JpaRepository<ClinicProfile, UUID> {
    Optional<ClinicProfile> findFirstByOrderByCreatedAtAsc();
}
