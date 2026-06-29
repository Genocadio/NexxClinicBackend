package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Worker;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Query("""
            SELECT DISTINCT w FROM Worker w
            LEFT JOIN w.departments d
            LEFT JOIN w.roles r
            WHERE (:name IS NULL OR (
                LOWER(w.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(COALESCE(w.lastName, '')) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(CONCAT(w.firstName, ' ', COALESCE(w.lastName, ''))) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(COALESCE(w.username, '')) LIKE LOWER(CONCAT('%', :name, '%'))
                OR LOWER(COALESCE(w.email, '')) LIKE LOWER(CONCAT('%', :name, '%'))
            ))
            AND (:activeOnly IS NULL OR w.active = :activeOnly)
            AND (:role IS NULL OR r = :role)
            AND (:departmentId IS NULL OR d.id = :departmentId OR w.department.id = :departmentId)
            ORDER BY w.firstName ASC, w.lastName ASC, w.createdAt DESC
            """)
    List<Worker> searchWorkers(
            @Param("name") String name,
            @Param("role") com.nexxserve.nexxclinic.model.RoleName role,
            @Param("activeOnly") Boolean activeOnly,
            @Param("departmentId") UUID departmentId);
}
