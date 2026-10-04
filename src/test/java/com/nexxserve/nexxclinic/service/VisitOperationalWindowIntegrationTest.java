package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class VisitOperationalWindowIntegrationTest {

    @Autowired private VisitService visitService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private jakarta.persistence.EntityManager entityManager;

    private AuthenticatedUser createAuthUser() {
        return new AuthenticatedUser(
                UUID.randomUUID(),
                "admin_op_test",
                Set.of(RoleName.ADMIN),
                "test-token-id",
                Instant.now().plus(Duration.ofHours(1))
        );
    }

    private Patient createPatient() {
        Patient p = new Patient();
        p.setFirstName("John");
        p.setLastName("Doe");
        p.setFullName("John Doe");
        p.setPatientIdentifier("P-OP-" + UUID.randomUUID().toString().substring(0, 8));
        p.setPrimaryPhoneNumber("+250788" + (100000 + (int)(Math.random() * 899999)));
        p.setGender(Gender.MALE);
        p.setDateOfBirth(LocalDate.of(1990, 1, 1));
        return patientRepository.save(p);
    }

    private Visit createVisit(Patient patient, VisitStatus status, LocalDateTime visitDate, LocalDateTime updatedAt) {
        Visit v = new Visit();
        v.setPatient(patient);
        v.setStatus(status);
        v.setVisitDate(visitDate);
        Visit saved = visitRepository.save(v);
        entityManager.flush();
        LocalDateTime stamp = updatedAt != null ? updatedAt : visitDate;
        entityManager.createNativeQuery("UPDATE visits SET created_at = :dt, updated_at = :dt WHERE id = :id")
                .setParameter("dt", stamp)
                .setParameter("id", saved.getId())
                .executeUpdate();
        entityManager.refresh(saved);
        return saved;
    }

    @Test
    @Transactional
    void defaultRollingWindow_includesActiveOldVisitsAndRecentCompleted_excludesOldCompleted() {
        AuthenticatedUser authUser = createAuthUser();
        Patient patient = createPatient();

        // 1. Old open visit (created 10 days ago, still IN_PROGRESS)
        Visit oldOpenVisit = createVisit(patient, VisitStatus.IN_PROGRESS, LocalDateTime.now().minusDays(10), null);

        // 2. Recent completed visit (completed yesterday)
        Visit recentCompletedVisit = createVisit(patient, VisitStatus.COMPLETED, LocalDateTime.now().minusDays(1), null);

        // 3. Old completed visit (completed 15 days ago, not updated since)
        Visit oldCompletedVisit = createVisit(patient, VisitStatus.COMPLETED, LocalDateTime.now().minusDays(15), null);

        // Run default query (no date filters -> 3-day operational window)
        SearchVisitsInput defaultInput = new SearchVisitsInput(
                null, null, null, null, null, null, patient.getFullName(), 0, 50
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(defaultInput, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        List<UUID> returnedIds = response.data().stream().map(VisitDto::id).toList();

        // Must contain old open visit and recent completed visit
        assertTrue(returnedIds.contains(oldOpenVisit.getId()), "Old open visit should be present in active window");
        assertTrue(returnedIds.contains(recentCompletedVisit.getId()), "Recent completed visit should be present");
        assertFalse(returnedIds.contains(oldCompletedVisit.getId()), "Old completed visit should NOT be in 3-day window");
    }

    @Test
    @Transactional
    void explicitDateRange_returnsHistoricalVisits() {
        AuthenticatedUser authUser = createAuthUser();
        Patient patient = createPatient();

        Visit oldCompletedVisit = createVisit(patient, VisitStatus.COMPLETED, LocalDateTime.now().minusDays(15), null);

        // Query with fromDate covering 20 days ago to 10 days ago
        SearchVisitsInput historyInput = new SearchVisitsInput(
                null,
                LocalDate.now().minusDays(20).toString(),
                LocalDate.now().minusDays(10).toString(),
                null, null, null, patient.getFullName(), 0, 50
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(historyInput, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        List<UUID> returnedIds = response.data().stream().map(VisitDto::id).toList();
        assertTrue(returnedIds.contains(oldCompletedVisit.getId()), "Historical query must find old completed visit");
    }

    @Test
    @Transactional
    void activeOnly_filtersStrictlyOpenVisits() {
        AuthenticatedUser authUser = createAuthUser();
        Patient patient = createPatient();

        Visit openVisit = createVisit(patient, VisitStatus.CREATED, LocalDateTime.now().minusDays(1), null);
        Visit completedVisit = createVisit(patient, VisitStatus.COMPLETED, LocalDateTime.now().minusDays(1), null);
        Visit cancelledVisit = createVisit(patient, VisitStatus.CANCELLED, LocalDateTime.now().minusDays(1), null);

        SearchVisitsInput activeInput = new SearchVisitsInput(
                null, null, null, null, true, null, patient.getFullName(), 0, 50
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(activeInput, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        List<UUID> returnedIds = response.data().stream().map(VisitDto::id).toList();
        assertTrue(returnedIds.contains(openVisit.getId()));
        assertFalse(returnedIds.contains(completedVisit.getId()));
        assertFalse(returnedIds.contains(cancelledVisit.getId()));
    }
}
