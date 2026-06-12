package com.nexxserve.nexxclinic.repository;

import com.nexxserve.nexxclinic.entity.VisitDepartmentNoteViewer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VisitDepartmentNoteViewerRepository extends JpaRepository<VisitDepartmentNoteViewer, UUID> {

    Optional<VisitDepartmentNoteViewer> findByNoteIdAndViewerId(UUID noteId, UUID viewerId);

    boolean existsByNoteIdAndViewerId(UUID noteId, UUID viewerId);
}
