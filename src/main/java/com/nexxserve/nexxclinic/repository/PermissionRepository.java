package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByName(String name);

    boolean existsByName(String name);

    List<Permission> findByModule(String module);

    List<Permission> findByNameIn(Collection<String> names);

    List<Permission> findAllByOrderByNameAsc();
}
