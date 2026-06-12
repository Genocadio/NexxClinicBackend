package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitDepartmentNoteRepository extends JpaRepository<VisitDepartmentNote, UUID> {

    long countByVisitDepartmentId(UUID visitDepartmentId);

    List<VisitDepartmentNote> findByVisitDepartmentIdOrderByCreatedAtAsc(UUID visitDepartmentId);

    List<VisitDepartmentNote> findByVisitDepartment_Visit_IdOrderByCreatedAtAsc(UUID visitId);

    List<VisitDepartmentNote> findByVisitDepartment_Visit_IdAndVisitDepartment_IdOrderByCreatedAtAsc(
            UUID visitId,
            UUID visitDepartmentId
    );

    @Query("""
            SELECT COUNT(n) FROM VisitDepartmentNote n
            WHERE n.visitDepartment.id = :visitDepartmentId
            AND (n.createdBy IS NULL OR n.createdBy.id <> :viewerId)
            AND NOT EXISTS (
                SELECT v FROM VisitDepartmentNoteViewer v
                WHERE v.note = n AND v.viewer.id = :viewerId
            )
            """)
    long countNewNotesForViewer(
            @Param("visitDepartmentId") UUID visitDepartmentId,
            @Param("viewerId") UUID viewerId
    );
}
