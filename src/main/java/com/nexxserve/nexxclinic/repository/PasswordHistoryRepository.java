package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.PasswordHistoryEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordHistoryRepository extends JpaRepository<PasswordHistoryEntry, UUID> {

    List<PasswordHistoryEntry> findTop10ByWorkerIdOrderByCreatedAtDesc(UUID workerId);
}
