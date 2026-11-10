package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentNoteViewer;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitDepartmentNoteViewerRepository extends JpaRepository<VisitDepartmentNoteViewer, UUID> {

    Optional<VisitDepartmentNoteViewer> findByNoteIdAndViewerId(UUID noteId, UUID viewerId);

    boolean existsByNoteIdAndViewerId(UUID noteId, UUID viewerId);

    @Query("SELECT v.note.id FROM VisitDepartmentNoteViewer v WHERE v.viewer.id = :viewerId AND v.note.id IN :noteIds")
    Set<UUID> findViewedNoteIds(@Param("viewerId") UUID viewerId, @Param("noteIds") Collection<UUID> noteIds);

    void deleteByNote(com.nexxserve.nexxclinic.entity.VisitDepartmentNote note);
}
