package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.AdminAuditLog;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.StandaloneForm;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.graphql.input.StandaloneFormInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateInsuranceProviderInput;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.repository.AdminAuditLogRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.StandaloneFormRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the S8 error-handling fixes. Each test drives the exact
 * input that used to throw a {@code RuntimeException} (-> 500) or NPE, and
 * asserts the service now returns a clean {@link ApiResponse} with
 * {@link ResponseStatus#ERROR} instead of throwing.
 */
@SpringBootTest
@Transactional
class ErrorHandlingRegressionTest {

    @Autowired
    private InsuranceProviderService insuranceProviderService;

    @Autowired
    private StandaloneFormService standaloneFormService;

    @Autowired
    private StandaloneFormRepository standaloneFormRepository;

    @Autowired
    private VisitDepartmentNoteService visitDepartmentNoteService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private AdminAuditService adminAuditService;

    @Autowired
    private AdminAuditLogRepository adminAuditLogRepository;

    // ─────────────────────────────────────────────────────────────
    // InsuranceProviderService.updateInsuranceProvider
    // ─────────────────────────────────────────────────────────────

    @Test
    void updateInsuranceProviderWithUnknownIdReturnsCleanErrorInsteadOfThrowing() {
        ApiResponse<?> response = insuranceProviderService.updateInsuranceProvider(
                UUID.randomUUID(),
                new UpdateInsuranceProviderInput("Renamed", null, null, null, null)
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("Insurance provider not found", response.message());
    }

    // ─────────────────────────────────────────────────────────────
    // StandaloneFormService.updateForm
    // ─────────────────────────────────────────────────────────────

    @Test
    void updateFormWithUnknownIdReturnsCleanErrorInsteadOfThrowing() {
        ApiResponse<?> response = standaloneFormService.updateForm(
                UUID.randomUUID(),
                new StandaloneFormInput("Renamed", null, "consultation", null, false, Map.of(), null),
                false
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("Form not found", response.message());
    }

    @Test
    void updateFormWithNoVersionRowsReturnsCleanErrorInsteadOfThrowing() {
        // A form that exists but has zero version rows (legacy/edge data).
        StandaloneForm orphan = new StandaloneForm();
        orphan.setName("orphan-form");
        orphan.setType("consultation");
        orphan = standaloneFormRepository.save(orphan);

        ApiResponse<?> response = standaloneFormService.updateForm(
                orphan.getId(),
                new StandaloneFormInput("Renamed", null, "consultation", null, false, Map.of(), null),
                false
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("No version found for form", response.message());
    }

    // ─────────────────────────────────────────────────────────────
    // VisitDepartmentNoteService.visitDepartmentNotes
    // ─────────────────────────────────────────────────────────────

    @Test
    void visitDepartmentNotesWithForeignDepartmentReturnsCleanErrorInsteadOfThrowing() {
        Patient patient = new Patient();
        patient.setFirstName("Regression");
        patient.setLastName("Patient");
        patient.setFullName("Regression Patient");
        patient.setDateOfBirth(LocalDate.of(2000, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit = visitRepository.save(visit);

        // A department id that exists but belongs to another visit (or none at all)
        // used to throw RuntimeException -> 500.
        // (AuthenticatedUser) null picks the 3-arg convenience overload; without the
        // cast the call is ambiguous against the 4-arg (NoteType, AuthenticatedUser) one.
        ApiResponse<?> response = visitDepartmentNoteService.visitDepartmentNotes(
                visit.getId(),
                UUID.randomUUID(),
                (AuthenticatedUser) null
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("Visit department not found or does not belong to visit.", response.message());
    }

    @Test
    void visitDepartmentNotesWithUnknownVisitReturnsCleanError() {
        ApiResponse<?> response = visitDepartmentNoteService.visitDepartmentNotes(
                UUID.randomUUID(),
                null,
                (AuthenticatedUser) null
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("Visit not found.", response.message());
    }

    // ─────────────────────────────────────────────────────────────
    // AdminAuditService.latestAuditLogs (Map.of -> LinkedHashMap)
    // ─────────────────────────────────────────────────────────────

    @Test
    void auditLogsWithNullDetailsDoNotThrow() {
        AdminAuditLog log = new AdminAuditLog();
        log.setActionType("REGRESSION_TEST");
        log.setAdminUserId(UUID.randomUUID());
        log.setTargetUserId(UUID.randomUUID());
        // details is a nullable column — the Map.of used to NPE here.
        log.setDetails(null);
        adminAuditLogRepository.save(log);

        List<Map<String, Object>> logs = assertDoesNotThrow(
                adminAuditService::latestAuditLogs
        );

        assertNotNull(logs);
        assertFalse(logs.isEmpty());
        // The regression was the Map.of NPE on the null details value — pin that the
        // null-details row survives mapping (the "details" key must be present).
        assertTrue(
                logs.stream().anyMatch(m -> m.containsKey("details") && m.get("details") == null),
                "null-details audit row must be mapped without throwing"
        );
    }
}
