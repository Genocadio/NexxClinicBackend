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
import com.nexxserve.nexxclinic.model.RoleName;
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
    private final com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher;

    public VisitDepartmentNoteService(
            VisitRepository visitRepository,
            VisitDepartmentRepository visitDepartmentRepository,
            VisitDepartmentNoteRepository visitDepartmentNoteRepository,
            VisitDepartmentNoteViewerRepository visitDepartmentNoteViewerRepository,
            WorkerRepository workerRepository,
            WorkerMapper workerMapper,
            com.nexxserve.nexxclinic.events.ClinicEventPublisher clinicEventPublisher
    ) {
        this.visitRepository = visitRepository;
        this.visitDepartmentRepository = visitDepartmentRepository;
        this.visitDepartmentNoteRepository = visitDepartmentNoteRepository;
        this.visitDepartmentNoteViewerRepository = visitDepartmentNoteViewerRepository;
        this.workerRepository = workerRepository;
        this.workerMapper = workerMapper;
        this.clinicEventPublisher = clinicEventPublisher;
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
            // S8 fix: a missing/foreign visitDepartmentId must be a clean error, not a
            // RuntimeException -> 500. The filter already proves it belongs to this visit.
            if (visitDepartmentRepository.findById(visitDepartmentId)
                    .filter(vd -> visitId.equals(vd.getVisit().getId()))
                    .isEmpty()) {
                return ApiResponse.error("Visit department not found or does not belong to visit.");
            }
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

        // Visibility filtering:
        // - Creators can always see their notes
        // - Admin/managers can fetch and see all notes
        // - Billing notes: only users in billing (FINANCE, ADMIN, MANAGER, CLINIC_ADMIN) or creator
        // - Targeted notes: only creator, admin/manager, or target user
        notes = notes.stream()
                .filter(n -> canUserViewNote(n, authUser))
                .toList();

        Set<UUID> viewedNoteIds = Collections.emptySet();
        if (authUser != null && authUser.userId() != null && !notes.isEmpty()) {
            List<UUID> noteIds = notes.stream().map(VisitDepartmentNote::getId).toList();
            viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(authUser.userId(), noteIds);
        }

        final Set<UUID> finalViewedNoteIds = viewedNoteIds;
        List<VisitDepartmentNoteDto> noteDtos = notes.stream()
                .map(note -> visitDepartmentNoteToDto(note, authUser, finalViewedNoteIds))
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
     * Supports both top-level visit departments and child visit departments,
     * because child departments are also stored as {@link VisitDepartment} records.
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

        // Notes are only blocked when the department is FINALISED.
        if (visitDepartment.getStatus() == VisitDepartmentStatus.FINALISED) {
            return ApiResponse.error("Cannot add notes to a finalised department.");
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
        clinicEventPublisher.publishNoteAdded(saved, actingUser);
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

    /**
     * Whether a visit department has any notes at all. Used as an FK guard before
     * hard-deleting a child department (a child with notes must never be deleted ->
     * would throw DataIntegrityViolationException -> 500).
     */
    @Transactional(readOnly = true)
    public boolean hasNotes(UUID visitDepartmentId) {
        return visitDepartmentNoteRepository.countByVisitDepartmentId(visitDepartmentId) > 0;
    }

    @Transactional
    public void deleteAllNotesForDepartment(UUID visitDepartmentId) {
        List<com.nexxserve.nexxclinic.entity.VisitDepartmentNote> notes =
                visitDepartmentNoteRepository.findByVisitDepartmentId(visitDepartmentId);
        for (com.nexxserve.nexxclinic.entity.VisitDepartmentNote note : notes) {
            // Delete viewers first (FK constraint)
            visitDepartmentNoteViewerRepository.deleteByNote(note);
        }
        visitDepartmentNoteRepository.deleteByVisitDepartmentId(visitDepartmentId);
    }

    public VisitDepartmentNotesSummaryDto buildNotesSummary(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (visitDepartmentId == null) {
            return new VisitDepartmentNotesSummaryDto(0, 0);
        }
        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository.findByVisitDepartmentId(visitDepartmentId);
        if (notes.isEmpty()) {
            return new VisitDepartmentNotesSummaryDto(0, 0);
        }
        List<VisitDepartmentNote> visibleNotes = notes.stream()
                .filter(n -> canUserViewNote(n, authUser))
                .toList();
        int totalNotes = visibleNotes.size();
        int newNotes = 0;
        if (authUser != null && authUser.userId() != null && !visibleNotes.isEmpty()) {
            List<UUID> noteIds = visibleNotes.stream().map(VisitDepartmentNote::getId).toList();
            Set<UUID> viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(authUser.userId(), noteIds);
            newNotes = (int) visibleNotes.stream()
                    .filter(n -> isNoteUnreadForUser(n, authUser, viewedNoteIds))
                    .count();
        }
        return new VisitDepartmentNotesSummaryDto(totalNotes, newNotes);
    }

    public record NotesDepartmentSummaryMaps(
            Map<UUID, Long> totalNotesByVdId,
            Map<UUID, Long> newNotesByVdId
    ) {}

    /**
     * Batch summary for departments, applying role visibility and unread attention rules.
     */
    public NotesDepartmentSummaryMaps getNotesSummariesForDepartments(
            Collection<UUID> visitDepartmentIds,
            AuthenticatedUser authUser
    ) {
        if (visitDepartmentIds == null || visitDepartmentIds.isEmpty()) {
            return new NotesDepartmentSummaryMaps(Map.of(), Map.of());
        }

        List<VisitDepartmentNote> allNotes = visitDepartmentNoteRepository.findByVisitDepartmentIdIn(visitDepartmentIds);
        if (allNotes.isEmpty()) {
            return new NotesDepartmentSummaryMaps(Map.of(), Map.of());
        }

        List<VisitDepartmentNote> visibleNotes = allNotes.stream()
                .filter(n -> canUserViewNote(n, authUser))
                .toList();

        Map<UUID, Long> totalNotesByVdId = visibleNotes.stream()
                .filter(n -> n.getVisitDepartment() != null && n.getVisitDepartment().getId() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        n -> n.getVisitDepartment().getId(),
                        java.util.stream.Collectors.counting()
                ));

        Map<UUID, Long> newNotesByVdId = new HashMap<>();
        if (authUser != null && authUser.userId() != null && !visibleNotes.isEmpty()) {
            List<UUID> noteIds = visibleNotes.stream().map(VisitDepartmentNote::getId).toList();
            Set<UUID> viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(authUser.userId(), noteIds);

            for (VisitDepartmentNote note : visibleNotes) {
                if (isNoteUnreadForUser(note, authUser, viewedNoteIds)) {
                    UUID vdId = note.getVisitDepartment() != null ? note.getVisitDepartment().getId() : null;
                    if (vdId != null) {
                        newNotesByVdId.merge(vdId, 1L, Long::sum);
                    }
                }
            }
        }

        return new NotesDepartmentSummaryMaps(totalNotesByVdId, newNotesByVdId);
    }

    /**
     * Batch count unread notes for visits, respecting visibility, admin unread scope,
     * and completion status.
     */
    public Map<UUID, Long> countUnreadNotesForVisits(
            Collection<UUID> visitIds,
            AuthenticatedUser authUser
    ) {
        if (visitIds == null || visitIds.isEmpty() || authUser == null || authUser.userId() == null) {
            return Map.of();
        }

        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository.findByVisitDepartment_Visit_IdIn(visitIds);
        if (notes.isEmpty()) {
            return Map.of();
        }

        List<VisitDepartmentNote> visibleNotes = notes.stream()
                .filter(n -> canUserViewNote(n, authUser))
                .toList();

        if (visibleNotes.isEmpty()) {
            return Map.of();
        }

        List<UUID> noteIds = visibleNotes.stream().map(VisitDepartmentNote::getId).toList();
        Set<UUID> viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(authUser.userId(), noteIds);

        Map<UUID, Long> unreadByVisitId = new HashMap<>();
        for (VisitDepartmentNote note : visibleNotes) {
            if (isNoteUnreadForUser(note, authUser, viewedNoteIds)) {
                if (note.getVisitDepartment() != null && note.getVisitDepartment().getVisit() != null) {
                    UUID vId = note.getVisitDepartment().getVisit().getId();
                    if (vId != null) {
                        unreadByVisitId.merge(vId, 1L, Long::sum);
                    }
                }
            }
        }
        return unreadByVisitId;
    }

    public long countUnreadNotesForVisit(UUID visitId, AuthenticatedUser authUser) {
        if (visitId == null || authUser == null || authUser.userId() == null) {
            return 0;
        }
        Map<UUID, Long> map = countUnreadNotesForVisits(List.of(visitId), authUser);
        return map.getOrDefault(visitId, 0L);
    }

    /**
     * Whether the calling user has billing privileges (FINANCE, ADMIN, MANAGER, CLINIC_ADMIN).
     */
    public boolean canViewBillingNotes(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(RoleName.FINANCE)
                || authUser.roles().contains(RoleName.ADMIN)
                || authUser.roles().contains(RoleName.MANAGER)
                || authUser.roles().contains(RoleName.CLINIC_ADMIN);
    }

    public boolean isAdminOrManager(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(RoleName.ADMIN)
                || authUser.roles().contains(RoleName.MANAGER)
                || authUser.roles().contains(RoleName.CLINIC_ADMIN);
    }

    /**
     * Rule 1 & Rule 2: Note visibility determination.
     * - Creator can always view their own note.
     * - Admin/managers can fetch and see all notes.
     * - BILLING notes: only users with billing privilege can see.
     * - Targeted notes: user must be creator, admin/manager, or listed in targets.
     */
    public boolean canUserViewNote(VisitDepartmentNote note, AuthenticatedUser authUser) {
        if (note == null) {
            return false;
        }
        UUID viewerId = authUser == null ? null : authUser.userId();

        // Creator can always view
        if (viewerId != null && note.getCreatedBy() != null && viewerId.equals(note.getCreatedBy().getId())) {
            return true;
        }

        // Admin and managers can view all notes
        if (isAdminOrManager(authUser)) {
            return true;
        }

        // Billing notes: only users in billing (FINANCE, ADMIN, MANAGER, CLINIC_ADMIN) or creator
        if (note.getNoteType() == NoteType.BILLING) {
            if (!canViewBillingNotes(authUser)) {
                return false;
            }
        }

        // Targeted notes: if targets specified, viewer must be in target list
        List<Worker> targets = note.getTargetUsers();
        if (targets != null && !targets.isEmpty()) {
            if (viewerId == null) {
                return false;
            }
            return targets.stream().anyMatch(w -> viewerId.equals(w.getId()));
        }

        return true;
    }

    /**
     * Attention & Unread rules:
     * - User must be able to view the note.
     * - Creator's own note is never unread.
     * - For Admin/Managers: ONLY ADMIN and PUBLIC notes trigger unread dot/badge/gate.
     * - For other users: If visit department is COMPLETED or FINALISED, notes created at
     *   or before completion are no longer enforced (not unread). Notes created after
     *   completion still require attention.
     */
    public boolean isNoteUnreadForUser(VisitDepartmentNote note, AuthenticatedUser authUser, Set<UUID> viewedNoteIds) {
        if (note == null || authUser == null || authUser.userId() == null) {
            return false;
        }
        if (!canUserViewNote(note, authUser)) {
            return false;
        }
        UUID viewerId = authUser.userId();
        if (note.getCreatedBy() != null && viewerId.equals(note.getCreatedBy().getId())) {
            return false;
        }

        // Admin/manager unread scope: only ADMIN or PUBLIC notes
        if (isAdminOrManager(authUser)) {
            if (note.getNoteType() != NoteType.ADMIN && note.getNoteType() != NoteType.PUBLIC) {
                return false;
            }
        } else {
            // Completed department rule for all other users:
            VisitDepartment vd = note.getVisitDepartment();
            if (vd != null && (vd.getStatus() == VisitDepartmentStatus.COMPLETED || vd.getStatus() == VisitDepartmentStatus.FINALISED)) {
                java.time.LocalDateTime completedAt = vd.getCompletedAt();
                if (completedAt == null) {
                    completedAt = vd.getUpdatedAt() != null ? vd.getUpdatedAt() : vd.getCreatedAt();
                }
                if (completedAt != null && note.getCreatedAt() != null && !note.getCreatedAt().isAfter(completedAt)) {
                    return false;
                }
            }
        }

        if (viewedNoteIds != null) {
            return !viewedNoteIds.contains(note.getId());
        }
        return !visitDepartmentNoteViewerRepository.existsByNoteIdAndViewerId(note.getId(), viewerId);
    }

    public boolean isNoteUnreadForUser(VisitDepartmentNote note, AuthenticatedUser authUser) {
        return isNoteUnreadForUser(note, authUser, null);
    }

    /**
     * Admin, Manager and Clinic Admin roles are never blocked by unread notes.
     * Finance is handled separately at the billing boundary (see
     * {@link BillingValidation#countUnreadNotesForVisit} and
     * {@link #hasUnreadBillingNotes}) so finance can bill without being forced
     * through the clinician notes workflow.
     */
    public boolean isExemptFromNotesBlock(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(RoleName.ADMIN)
                || authUser.roles().contains(RoleName.MANAGER)
                || authUser.roles().contains(RoleName.CLINIC_ADMIN);
    }

    /**
     * Finance users can bypass the unread BILLING notes gate. ADMIN / MANAGER /
     * CLINIC_ADMIN are already covered by {@link #isExemptFromNotesBlock}.
     */
    public boolean canBypassBillingNotesGate(AuthenticatedUser authUser) {
        if (authUser == null || authUser.roles() == null) {
            return false;
        }
        return authUser.roles().contains(RoleName.FINANCE);
    }

    /**
     * Checks whether there are any unread BILLING notes for the current user on the given visit.
     *
     * <p>Finance users bypass this gate intentionally so the billing screen does not force finance
     * staff to hunt down clinician notes for completed/finalised visits. ADMIN / MANAGER / CLINIC_ADMIN
     * are already exempt via {@link #isExemptFromNotesBlock}.
     */
    @Transactional(readOnly = true)
    public boolean hasUnreadBillingNotes(UUID visitId, AuthenticatedUser authUser) {
        if (authUser == null || authUser.userId() == null || visitId == null) {
            return false;
        }
        if (isExemptFromNotesBlock(authUser) || canBypassBillingNotesGate(authUser)) {
            return false;
        }
        if (!canViewBillingNotes(authUser)) {
            return false;
        }
        UUID viewerId = authUser.userId();
        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository.findByVisitDepartment_Visit_IdOrderByCreatedAtAsc(visitId);
        if (notes.isEmpty()) {
            return false;
        }
        Set<UUID> viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(
                viewerId,
                notes.stream().map(VisitDepartmentNote::getId).toList()
        );
        return notes.stream()
                .filter(n -> n.getVisitDepartment() == null || n.getVisitDepartment().getStatus() != VisitDepartmentStatus.CANCELLED)
                .filter(n -> n.getNoteType() == NoteType.BILLING)
                .anyMatch(n -> isNoteUnreadForUser(n, authUser, viewedNoteIds));
    }

    /**
     * Checks whether there are any unread department notes for the current user on the given visit department.
     */
    @Transactional(readOnly = true)
    public boolean hasUnreadDepartmentNotes(UUID visitDepartmentId, AuthenticatedUser authUser) {
        if (isExemptFromNotesBlock(authUser) || authUser == null || authUser.userId() == null || visitDepartmentId == null) {
            return false;
        }
        List<VisitDepartmentNote> notes = visitDepartmentNoteRepository.findByVisitDepartmentId(visitDepartmentId);
        if (notes.isEmpty()) {
            return false;
        }
        Set<UUID> viewedNoteIds = visitDepartmentNoteViewerRepository.findViewedNoteIds(
                authUser.userId(),
                notes.stream().map(VisitDepartmentNote::getId).toList()
        );
        return notes.stream()
                .filter(n -> n.getVisitDepartment() == null || n.getVisitDepartment().getStatus() != VisitDepartmentStatus.CANCELLED)
                .anyMatch(n -> isNoteUnreadForUser(n, authUser, viewedNoteIds));
    }

    // ─────────────────────────────────────────────────────────────
    //  DTO MAPPING
    // ─────────────────────────────────────────────────────────────

    private VisitDepartmentNoteDto visitDepartmentNoteToDto(
            VisitDepartmentNote note,
            AuthenticatedUser authUser,
            Set<UUID> viewedNoteIds) {

        boolean hasViewed = !isNoteUnreadForUser(note, authUser, viewedNoteIds);

        return new VisitDepartmentNoteDto(
                note.getId(),
                note.getVisitDepartment() != null ? note.getVisitDepartment().getId() : null,
                note.getContent(),
                workerMapper.toDto(note.getCreatedBy()),
                note.getNoteType(),
                hasViewed,               // <-- final viewed flag
                note.getCreatedAt());
    }

    private VisitDepartmentNoteDto visitDepartmentNoteToDto(
            VisitDepartmentNote note,
            AuthenticatedUser authUser) {
        return visitDepartmentNoteToDto(note, authUser, null);
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
