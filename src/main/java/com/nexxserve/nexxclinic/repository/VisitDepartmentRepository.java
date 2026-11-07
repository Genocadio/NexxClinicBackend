package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartment;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface VisitDepartmentRepository extends JpaRepository<VisitDepartment, UUID> {

    @Query("SELECT vd FROM VisitDepartment vd "
            + "LEFT JOIN FETCH vd.visit v "
            + "LEFT JOIN FETCH v.patient "
            + "LEFT JOIN FETCH vd.department "
            + "LEFT JOIN FETCH vd.profile "
            + "WHERE vd.id = :id")
    Optional<VisitDepartment> findByIdWithVisitAndDepartment(@Param("id") UUID id);

    List<VisitDepartment> findByVisitId(UUID visitId);

    List<VisitDepartment> findByAnswerId(UUID answerId);

    List<VisitDepartment> findByVisitIdIn(Collection<UUID> visitIds);

    List<VisitDepartment> findByVisitIdAndParentVisitDepartmentIsNull(UUID visitId);

    List<VisitDepartment> findByParentVisitDepartmentId(UUID parentVisitDepartmentId);

    List<VisitDepartment> findByParentVisitDepartmentIdIn(Collection<UUID> parentVisitDepartmentIds);

    /**
     * Resolve the processor worker IDs for many visit departments in one query,
     * straight off the {@code visit_department_processors} join table.
     *
     * <p>Reading {@code VisitDepartment.getProcessors()} instead would initialise a
     * lazy collection per department; this keeps the visit-read path at a fixed
     * query count regardless of how many departments a page contains.
     *
     * @return rows of {@code [visitDepartmentId, processorWorkerId]}, in the
     *         join table's natural order
     */
    @Query("SELECT vd.id, p.id FROM VisitDepartment vd JOIN vd.processors p "
            + "WHERE vd.id IN :visitDepartmentIds")
    List<Object[]> findProcessorPairsByVisitDepartmentIdIn(
            @Param("visitDepartmentIds") Collection<UUID> visitDepartmentIds);

    List<VisitDepartment> findByDepartmentIdIn(Collection<UUID> departmentIds);

    Optional<VisitDepartment> findByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    Optional<VisitDepartment> findByVisitIdAndDepartmentIdAndParentVisitDepartmentId(
            UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);

    boolean existsByVisitIdAndDepartmentId(UUID visitId, UUID departmentId);

    boolean existsByVisitIdAndDepartmentIdAndParentVisitDepartmentId(
            UUID visitId, UUID departmentId, UUID parentVisitDepartmentId);

    boolean existsByProfileId(UUID profileId);

    @Query("SELECT vd FROM VisitDepartment vd " +
            "WHERE vd.visit.patient.id = :patientId " +
            "AND vd.department.id = :departmentId " +
            "AND vd.parentVisitDepartment IS NULL " +
            "ORDER BY vd.visit.visitDate DESC, vd.visit.createdAt DESC")
    List<VisitDepartment> findLastByPatientIdAndDepartmentId(
            @Param("patientId") UUID patientId,
            @Param("departmentId") UUID departmentId,
            Pageable pageable);

    @Query("""
            SELECT vd FROM VisitDepartment vd
            WHERE vd.visit.patient.id = :patientId
            AND vd.department.id = :departmentId
            AND vd.parentVisitDepartment IS NULL
            AND (
                vd.visit.visitDate < :referenceVisitDate
                OR (vd.visit.visitDate = :referenceVisitDate AND vd.visit.createdAt < :referenceCreatedAt)
                OR (vd.visit.visitDate = :referenceVisitDate AND vd.visit.createdAt = :referenceCreatedAt AND vd.visit.id <> :referenceVisitId)
            )
            ORDER BY vd.visit.visitDate DESC, vd.visit.createdAt DESC
            """)
    List<VisitDepartment> findPreviousByPatientIdAndDepartmentId(
            @Param("patientId") UUID patientId,
            @Param("departmentId") UUID departmentId,
            @Param("referenceVisitDate") java.time.LocalDateTime referenceVisitDate,
            @Param("referenceCreatedAt") java.time.LocalDateTime referenceCreatedAt,
            @Param("referenceVisitId") UUID referenceVisitId,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT vd FROM VisitDepartment vd WHERE vd.id = :id")
    Optional<VisitDepartment> findByIdForUpdate(@Param("id") UUID id);

    @Query("SELECT DISTINCT vd.visit.id FROM VisitDepartment vd WHERE vd.status = com.nexxserve.nexxclinic.model.VisitDepartmentStatus.BILLING AND vd.visit IS NOT NULL")
    List<UUID> findVisitIdsWithBillingDepartment();
}
