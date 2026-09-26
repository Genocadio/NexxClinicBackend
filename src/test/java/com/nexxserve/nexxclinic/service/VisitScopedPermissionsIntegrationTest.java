package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDto;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentDiagnosis;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.SearchVisitsInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentDiagnosisRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
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
class VisitScopedPermissionsIntegrationTest {

    @Autowired private VisitService visitService;
    @Autowired private PatientRepository patientRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;
    @Autowired private WorkerRepository workerRepository;

    private Department createDepartment(String name) {
        Department d = new Department();
        d.setName(name + " " + UUID.randomUUID().toString().substring(0, 6));
        return departmentRepository.save(d);
    }

    private Worker createWorker(String firstName, RoleName role, Department dept) {
        Worker w = new Worker();
        w.setFirstName(firstName);
        w.setLastName("Test");
        w.setUsername("w_" + UUID.randomUUID().toString().substring(0, 8));
        w.setEmail("w_" + UUID.randomUUID().toString().substring(0, 8) + "@test.com");
        w.setRoles(Set.of(role));
        w.setAccountStatus(AccountStatus.ACTIVE);
        w.setActive(true);
        if (dept != null) {
            w.setDepartments(Set.of(dept));
        }
        return workerRepository.save(w);
    }

    private Patient createPatient() {
        Patient p = new Patient();
        p.setFirstName("Jane");
        p.setLastName("ScopeTest");
        p.setFullName("Jane ScopeTest");
        p.setPatientIdentifier("P-SC-" + UUID.randomUUID().toString().substring(0, 8));
        p.setPrimaryPhoneNumber("+250788" + (100000 + (int)(Math.random() * 899999)));
        p.setGender(Gender.FEMALE);
        p.setDateOfBirth(LocalDate.of(1995, 5, 15));
        return patientRepository.save(p);
    }

    private Visit createVisitWithDepartment(Patient patient, Department dept) {
        Visit v = new Visit();
        v.setPatient(patient);
        v.setStatus(VisitStatus.IN_PROGRESS);
        v.setVisitDate(LocalDateTime.now());
        Visit savedVisit = visitRepository.save(v);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(savedVisit);
        vd.setDepartment(dept);
        vd.setStatus(VisitDepartmentStatus.ACTIVE);
        vd.setAnswerId(UUID.randomUUID());
        VisitDepartment savedVd = visitDepartmentRepository.save(vd);

        // Add a diagnosis
        VisitDepartmentDiagnosis diag = new VisitDepartmentDiagnosis();
        diag.setVisitDepartment(savedVd);
        diag.setDiagnosisName("Acute Bronchitis");
        visitDepartmentDiagnosisRepository.save(diag);

        return savedVisit;
    }

    @Test
    @Transactional
    void receptionist_seesAllVisits_butClinicalDiagnosticsAreMasked() {
        Department dental = createDepartment("Dental");
        Department opd = createDepartment("OPD");
        Patient patient = createPatient();

        createVisitWithDepartment(patient, dental);
        createVisitWithDepartment(patient, opd);

        Worker receptionist = createWorker("Alice", RoleName.RECEPTION, null);
        AuthenticatedUser authUser = new AuthenticatedUser(
                receptionist.getId(),
                receptionist.getUsername(),
                Set.of(RoleName.RECEPTION),
                "token-rec",
                Instant.now().plus(Duration.ofHours(1))
        );

        SearchVisitsInput input = new SearchVisitsInput(
                null, null, null, null, null, null, patient.getFullName(), 0, 10
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(input, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        assertEquals(2, response.data().size(), "Receptionist should see all 2 visits");

        // Verify clinical data is masked for receptionist
        for (VisitDto v : response.data()) {
            for (VisitDepartmentDto vd : v.departments()) {
                assertNull(vd.answerId(), "Consultation answerId should be masked for reception");
                assertTrue(vd.diagnostics().isEmpty(), "Diagnostics should be masked (empty) for reception");
                assertTrue(vd.medications().isEmpty(), "Medications should be masked (empty) for reception");
            }
        }
    }

    @Test
    @Transactional
    void clinician_withAssignedDepartment_onlySeesTheirDepartmentVisits_andHasFullClinicalData() {
        Department dental = createDepartment("Dental");
        Department pediatrics = createDepartment("Pediatrics");
        Patient patient = createPatient();

        Visit dentalVisit = createVisitWithDepartment(patient, dental);
        Visit pedsVisit = createVisitWithDepartment(patient, pediatrics);

        // Clinician assigned only to Dental
        Worker dentist = createWorker("Dr. Bob", RoleName.CLINICIAN, dental);
        AuthenticatedUser authUser = new AuthenticatedUser(
                dentist.getId(),
                dentist.getUsername(),
                Set.of(RoleName.CLINICIAN),
                "token-dentist",
                Instant.now().plus(Duration.ofHours(1))
        );

        SearchVisitsInput input = new SearchVisitsInput(
                null, null, null, null, null, null, patient.getFullName(), 0, 10
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(input, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        assertEquals(1, response.data().size(), "Scoped dentist should only see Dental visit");
        assertEquals(dentalVisit.getId(), response.data().get(0).id());

        // Verify clinical data IS visible for clinician
        VisitDepartmentDto dentalVd = response.data().get(0).departments().get(0);
        assertNotNull(dentalVd.answerId(), "Answer ID should be visible for clinician");
        assertFalse(dentalVd.diagnostics().isEmpty(), "Diagnostics should be visible for clinician");
        assertEquals("Acute Bronchitis", dentalVd.diagnostics().get(0).diagnosisName());
    }

    @Test
    @Transactional
    void floatingClinician_withNoDepartment_seesGeneralClinicVisits() {
        Department dental = createDepartment("Dental");
        Department opd = createDepartment("OPD");
        Patient patient = createPatient();

        createVisitWithDepartment(patient, dental);
        createVisitWithDepartment(patient, opd);

        // Floating Doctor (no assigned department)
        Worker floatingDoctor = createWorker("Dr. Floater", RoleName.CLINICIAN, null);
        AuthenticatedUser authUser = new AuthenticatedUser(
                floatingDoctor.getId(),
                floatingDoctor.getUsername(),
                Set.of(RoleName.CLINICIAN),
                "token-floater",
                Instant.now().plus(Duration.ofHours(1))
        );

        SearchVisitsInput input = new SearchVisitsInput(
                null, null, null, null, null, null, patient.getFullName(), 0, 10
        );
        ApiResponse<List<VisitDto>> response = visitService.visits(input, authUser);

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());
        assertEquals(2, response.data().size(), "Floating clinician should see all visits");
    }

    @Test
    @Transactional
    void patientHistory_accessibleByClinician_blockedForUnauthorizedUser() {
        Department opd = createDepartment("OPD");
        Patient patient = createPatient();
        createVisitWithDepartment(patient, opd);

        // 1. Clinician can access patient history
        Worker doctor = createWorker("Dr. Smith", RoleName.CLINICIAN, null);
        AuthenticatedUser doctorAuth = new AuthenticatedUser(
                doctor.getId(), doctor.getUsername(), Set.of(RoleName.CLINICIAN), "tok", Instant.now().plus(Duration.ofHours(1))
        );
        ApiResponse<List<VisitDto>> docResponse = visitService.getPatientHistory(patient.getId(), new SearchPatientHistoryInput(null, null, null, null, null, null, null, null, null, null, 0, 10), doctorAuth);
        assertEquals(ResponseStatus.SUCCESS, docResponse.status());

        // 2. Staff user without history permission is blocked
        Worker staff = createWorker("Sam", RoleName.STAFF, null);
        AuthenticatedUser staffAuth = new AuthenticatedUser(
                staff.getId(), staff.getUsername(), Set.of(RoleName.STAFF), "tok", Instant.now().plus(Duration.ofHours(1))
        );
        ApiResponse<List<VisitDto>> staffResponse = visitService.getPatientHistory(patient.getId(), new SearchPatientHistoryInput(null, null, null, null, null, null, null, null, null, null, 0, 10), staffAuth);
        assertEquals(ResponseStatus.ERROR, staffResponse.status());
        assertEquals("You do not have permission to view patient history.", staffResponse.message());
    }
}
