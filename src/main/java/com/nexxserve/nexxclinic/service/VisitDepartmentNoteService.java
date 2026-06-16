package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNoteViewer;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.AddVisitDepartmentNoteInput;
import com.nexxserve.nexxclinic.dto.out.*;
import com.nexxserve.nexxclinic.mappers.out.WorkerMapper;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteViewerRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles all operations on {@link VisitDepartmentNote} entities:
 *
 * <ul>
 *   <li>Querying notes for a visit or a specific visit department</li>
 *   <li>Adding notes – supports {@code noteType} and {@code targetUsers}</li>
 *   <li>Marking individual or bulk notes as viewed</li>
 *   <li>Building the unread-notes summary used in the department DTO</li>
 * </ul>
 */
@Service
public class VisitDepartmentNoteService {

    private final VisitRepository visitRepository;
    private final VisitDepartmentRepository visitDepartmentRepository;
    private final VisitDepartmentNoteRepository visitDepartmentNoteRepository;
    private final VisitDepartmentNoteViewerRepository visitDepartmentNoteViewerRepository;
    private final WorkerRepository workerRepository;
    private final WorkerMapper workerMapper;

    public VisitDepartmentNoteService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentNoteRepository visitDepartmentNoteRepository,
            VisitDepartmentNoteViewerRepository visitDepartmentNoteViewerRepository,
            WorkerRepository workerRepository,
            WorkerMapper workerMapper
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.visitDepartmentNoteViewerRepository = visitDepartmentNoteViewerRepository;
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
    }

    // ─────────────────────────────────────────────────────────────
    //  QUERIES
    // ─────────────────────────────────────────────────────────────

    /**
     * Returns all notes for a visit, optionally filtered to a single department,
     * a specific {@link NoteType}, and/or notes targeted at a specific worker.
     *
     * @param visitId            required
     * @param visitDepartmentId  optional – narrows results to one department
     * @param noteType           optional – filter by {@link NoteType} enum value
     * @param authUser           used to compute the {@code isNew} flag per note
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDepartmentNoteDto>> visitDepartmentNotes(
            UUID visitId,
            UUID visitDepartmentId,
            NoteType noteType,
            AuthenticatedUser authUser
    ) {
        if (visitId == null) { return ApiResponse.error("visitId is required."); }
        if (!visitRepository.existsById(visitId)) { return ApiResponse.error("Visit not found."); }

        List<VisitDepartmentNote> notes;
        if (visitDepartmentId != null) {
            VisitDepartment visitDepartment = visitDepartmentRepository.findById(visitDepartmentId)
                    .filter(vd -> visitId.equals(vd.getVisit().getId()))
                    .orElseThrow(() -> new RuntimeException("Visit department not found or does not belong to visit."));
            notes = visitDepartmentNoteRepository
                    .findByVisitDepartment_Visit_IdAndVisitDepartment_IdOrderByCreatedAtAsc(
                            visitId, visitDepartmentId);
        } else {
            notes = visitDepartmentNoteRepository.findByVisitDepartment_Visit_IdOrderByCreatedAtAsc(visitId);
        }

        // Optional enum filter for noteType
        if (noteType != null) {
            notes = notes.stream()
                    .filter(n -> noteType.equals(n.getNoteType()))
                    .toList();
        }

        UUID viewerId = authUser == null ? null : authUser.userId();

        // Visibility filtering: only include notes that are either public (no targetUsers)
        // or the current user is creator / a member of the target list.
        if (viewerId != null) {
            notes = notes.stream()
                    .filter(n -> {
                        List<Worker> targets = n.getTargetUsers();
                        if (targets == null || targets.isEmpty()) return true;          // public note
                        boolean creatorMatch = n.getCreatedBy() != null &&
                                viewerId.equals(n.getCreatedBy().getId());
                        boolean inTargets = targets.stream()
                                .anyMatch(w -> viewerId.equals(w.getId()));
                        return creatorMatch || inTargets;
                    })
                    .toList();
        }

        List<VisitDepartmentNoteDto> noteDtos = notes.stream()
                .map(note -> visitDepartmentNoteToDto(note, authUser))
                .toList();

        return ApiResponse.success("Visit department notes fetched.", noteDtos);
    }

    /**
     * Convenience overload without noteType / targetUserId filters –
     * preserves backward compatibility with existing callers.
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDepartmentNoteDto>> visitDepartmentNotes(
            UUID visitId,
            UUID visitDepartmentId,
            AuthenticatedUser authUser
    ) {
        return visitDepartmentNotes(visitId, visitDepartmentId, null, authUser);
    }

    // ─────────────────────────────────────────────────────────────
    //  MUTATIONS
    // ─────────────────────────────────────────────────────────────

    /**
     * Adds a note to a visit department.
     *
     * <p>Supported fields on {@link AddVisitDepartmentNoteInput}:
     * <ul>
     *   <li>{@code noteType}     – {@link NoteType} enum value. Optional; stored as-is.</li>
     *   <li>{@code targetUserId} – list of worker IDs this note is directed at.
     *                              Optional; all supplied IDs must exist or the call fails.</li>
     * </ul>
     */
    @Transactional
    public ApiResponse<VisitDepartmentNoteDto> addVisitDepartmentNote(
            AddVisitDepartmentNoteInput input,
            AuthenticatedUser authUser
    ) {
        if (input == null || input.visitDepartmentId() == null || input.content() == null || input.content().isBlank()) {
            return ApiResponse.error("visitDepartmentId and content are required.");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findById(input.visitDepartmentId());
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        VisitDepartment visitDepartment = visitDepartmentOptional.get();
        if (visitDepartment.getStatus() == VisitDepartmentStatus.CANCELLED) {
            return ApiResponse.error("Cannot add notes to a cancelled department.");
        }

        // Resolve target users if any are supplied
        List<Worker> targets = Collections.emptyList();
        if (input.targetUserId() != null && !input.targetUserId().isEmpty()) {
            Iterable<Worker> found = workerRepository.findAllById(input.targetUserId());

            Set<UUID> foundIds = new HashSet<>();
            List<Worker> list = new ArrayList<>();
            for (Worker w : found) {
                foundIds.add(w.getId());
                list.add(w);
            }

            List<UUID> missing = input.targetUserId().stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            if (!missing.isEmpty()) {
                return ApiResponse.error("The following target user IDs were not found: " + missing);
            }

            targets = list;
        }

        Worker actingUser = resolveWorker(authUser);
        VisitDepartmentNote note = new VisitDepartmentNote();
        note.setVisitDepartment(visitDepartment);
        note.setContent(input.content().trim());
        note.setCreatedBy(actingUser);
        note.setTargetUsers(targets);

        if (input.noteType() != null) {
            note.setNoteType(input.noteType());
        }

        VisitDepartmentNote saved = visitDepartmentNoteRepository.save(note);
        return ApiResponse.success("Note added successfully.", visitDepartmentNoteToDto(saved, authUser));
    }

    /**
     * Marks a single note as viewed by the authenticated worker.
     * No-ops silently when the note was created by the same worker.
     */
    @Transactional
    public ApiResponse<VisitDepartmentNoteDto> markVisitDepartmentNoteViewed(UUID noteId, AuthenticatedUser authUser) {
        if (noteId == null) {
            return ApiResponse.error("noteId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required.");
        }

        Optional<VisitDepartmentNote> noteOptional = visitDepartmentNoteRepository.findById(noteId);
        if (noteOptional.isEmpty()) {
            return ApiResponse.error("Note not found.");
        }

        VisitDepartmentNote note = noteOptional.get();
        if (note.getCreatedBy() != null && actingUser.getId().equals(note.getCreatedBy().getId())) {
            return ApiResponse.success("Note marked as viewed.", visitDepartmentNoteToDto(note, authUser));
        }

        if (!visitDepartmentNoteViewerRepository.existsByNoteIdAndViewerId(noteId, actingUser.getId())) {
            VisitDepartmentNoteViewer viewer = new VisitDepartmentNoteViewer();
            viewer.setNote(note);
            viewer.setViewer(actingUser);
            note.getViewers().add(viewer);
            visitDepartmentNoteRepository.save(note);
        }

        return ApiResponse.success("Note marked as viewed.", visitDepartmentNoteToDto(note, authUser));
    }

    /**
     * Bulk-marks all notes in a visit department as viewed for the authenticated worker.
     * Supports an optional {@link NoteType} filter so only notes of that type are marked.
     *
     * @param visitDepartmentId required
     * @param noteType          optional – if supplied, only notes whose type matches are marked
     * @param authUser          the calling user
     */
    @Transactional
    public ApiResponse<VisitDepartmentNotesSummaryDto> markVisitDepartmentNotesViewed(
            UUID visitDepartmentId,
            NoteType noteType,
            AuthenticatedUser authUser
    ) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }

        Worker actingUser = resolveWorker(authUser);
        if (actingUser == null) {
            return ApiResponse.error("Authentication is required.");
        }

        Optional<VisitDepartment> visitDepartmentOptional = visitDepartmentRepository.findById(visitDepartmentId);
        if (visitDepartmentOptional.isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository.findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartmentId);

        // Apply optional NoteType enum filter
        if (noteType != null) {
            notes = notes.stream()
                    .filter(n -> noteType.equals(n.getNoteType()))
                    .toList();
        }

        for (VisitDepartmentNote note : notes) {
            if (note.getCreatedBy() != null && actingUser.getId().equals(note.getCreatedBy().getId())) {
                continue;
            }
            if (!visitDepartmentNoteViewerRepository.existsByNoteIdAndViewerId(note.getId(), actingUser.getId())) {
                VisitDepartmentNoteViewer viewer = new VisitDepartmentNoteViewer();
                viewer.setNote(note);
                viewer.setViewer(actingUser);
                note.getViewers().add(viewer);
            }
        }
        visitDepartmentNoteRepository.saveAll(notes);

        VisitDepartmentNotesSummaryDto summary = buildNotesSummary(visitDepartmentId, authUser);
        return ApiResponse.success("Visit department notes marked as viewed.", summary);
    }

    /**
     * Convenience overload without noteType – preserves backward compatibility.
     */
    @Transactional
    public ApiResponse<VisitDepartmentNotesSummaryDto> markVisitDepartmentNotesViewed(
            UUID visitDepartmentId,
            AuthenticatedUser authUser
    ) {
        return markVisitDepartmentNotesViewed(visitDepartmentId, null, authUser);
    }

    /**
     * Returns notes for a visit department targeted at a specific worker.
     * Useful for "inbox"-style views where a clinician wants notes addressed to them.
     *
     * @param visitDepartmentId required
     * @param targetUserId      required – worker whose targeted notes to return
     * @param noteType          optional – further filter by {@link NoteType}
     * @param authUser          the calling user
     */
    @Transactional(readOnly = true)
    public ApiResponse<List<VisitDepartmentNoteDto>> visitDepartmentNotesForTarget(
            UUID visitDepartmentId,
            UUID targetUserId,
            NoteType noteType,
            AuthenticatedUser authUser
    ) {
        if (visitDepartmentId == null) {
            return ApiResponse.error("visitDepartmentId is required.");
        }
        if (targetUserId == null) {
            return ApiResponse.error("targetUserId is required.");
        }

        if (visitDepartmentRepository.findById(visitDepartmentId).isEmpty()) {
            return ApiResponse.error("Visit department not found.");
        }

        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository
                .findByVisitDepartmentIdOrderByCreatedAtAsc(visitDepartmentId)
                .stream()
                .filter(n -> n.getTargetUsers() != null &&
                        n.getTargetUsers().stream().anyMatch(w -> targetUserId.equals(w.getId())))
                .toList();

        if (noteType != null) {
            notes = notes.stream()
                    .filter(n -> noteType.equals(n.getNoteType()))
                    .toList();
        }

        List<VisitDepartmentNoteDto> noteDtos = notes.stream()
                .map(note -> visitDepartmentNoteToDto(note, authUser))
                .toList();

        return ApiResponse.success("Targeted notes fetched.", noteDtos);
    }

    // ─────────────────────────────────────────────────────────────
    //  SUMMARY (called by VisitDepartmentService when building DTOs)
    // ─────────────────────────────────────────────────────────────

    public VisitDepartmentNotesSummaryDto buildNotesSummary(UUID visitDepartmentId, AuthenticatedUser authUser) {
        long totalNotes = visitDepartmentNoteRepository.countByVisitDepartmentId(visitDepartmentId);
        int newNotes = 0;
        if (authUser != null && authUser.userId() != null) {
            newNotes = (int) visitDepartmentNoteRepository.countNewNotesForViewer(visitDepartmentId, authUser.userId());
        }
        return new VisitDepartmentNotesSummaryDto((int) totalNotes, newNotes);
    }

    // ─────────────────────────────────────────────────────────────
    //  DTO MAPPING
    // ─────────────────────────────────────────────────────────────

    private VisitDepartmentNoteDto visitDepartmentNoteToDto(
            VisitDepartmentNote note,
            AuthenticatedUser authUser) {

        UUID viewerId = (authUser == null) ? null : authUser.userId();

        // 1️⃣ Was the note explicitly viewed?
        boolean hasViewed = false;
        if (viewerId != null && !visitDepartmentNoteViewerRepository
                .existsByNoteIdAndViewerId(note.getId(), viewerId)) {
            // not found in the viewers table
        } else if (viewerId != null) {
            hasViewed = true;
        }

        // 2️⃣ OR – is the current user the creator?
        if (!hasViewed && viewerId != null
                && note.getCreatedBy() != null
                && viewerId.equals(note.getCreatedBy().getId())) {
            hasViewed = true;          // creator automatically sees it as viewed
        }

        return new VisitDepartmentNoteDto(
                note.getId(),
                note.getVisitDepartment().getId(),
                note.getContent(),
                workerMapper.toDto(note.getCreatedBy()),
                note.getNoteType(),
                hasViewed,               // <-- final viewed flag
                note.getCreatedAt());
    }




    // ─────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private Worker resolveWorker(AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null) {
            return null;
        }
        return workerRepository.findById(authUser.userId()).orElse(null);
    }
}