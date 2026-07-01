package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.Product;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);

    @Query("""
        SELECT p FROM Product p
        LEFT JOIN FETCH p.insuranceCoverages
        WHERE p.id = :id
        """)
    Optional<Product> findByIdWithCoverages(@Param("id") UUID id);

    @Override
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}
