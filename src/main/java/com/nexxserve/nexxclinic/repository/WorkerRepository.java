package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Worker;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    Optional<Worker> findByEmailIgnoreCase(String email);

    Optional<Worker> findByPhoneNumber(String phoneNumber);

    Optional<Worker> findByUsernameIgnoreCase(String username);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, UUID id);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, UUID id);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, UUID id);
}
