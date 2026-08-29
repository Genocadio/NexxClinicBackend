package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitDepartmentProductRepository extends JpaRepository<VisitDepartmentProduct, UUID> {

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.id = :visitDepartmentId AND v.deleted = false")
    List<VisitDepartmentProduct> findByVisitDepartmentId(@Param("visitDepartmentId") UUID visitDepartmentId);

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.id = :visitDepartmentId AND v.product.id = :productId AND v.deleted = false")
    Optional<VisitDepartmentProduct> findByVisitDepartmentIdAndProductId(@Param("visitDepartmentId") UUID visitDepartmentId, @Param("productId") UUID productId);

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.visit.id = :visitId AND v.deleted = false")
    List<VisitDepartmentProduct> findByVisitDepartmentVisitId(@Param("visitId") UUID visitId);

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.id = :visitDepartmentId")
    List<VisitDepartmentProduct> findByVisitDepartmentIdIncludingDeleted(@Param("visitDepartmentId") UUID visitDepartmentId);

    // Returns ALL rows (active + soft-deleted) for the given pair, ordered newest first.
    // An Optional here is unsafe: a product that was removed multiple times produces
    // multiple soft-deleted rows, which would throw NonUniqueResultException.
    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.id = :visitDepartmentId AND v.product.id = :productId ORDER BY v.createdAt DESC")
    List<VisitDepartmentProduct> findAllByVisitDepartmentIdAndProductIdIncludingDeleted(@Param("visitDepartmentId") UUID visitDepartmentId, @Param("productId") UUID productId);

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.visit.id = :visitId")
    List<VisitDepartmentProduct> findByVisitDepartmentVisitIdIncludingDeleted(@Param("visitId") UUID visitId);

    @Query("SELECT v FROM VisitDepartmentProduct v WHERE v.visitDepartment.visit.id = :visitId AND v.deleted = true")
    List<VisitDepartmentProduct> findSoftDeletedByVisitId(@Param("visitId") UUID visitId);
}
