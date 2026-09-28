package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentNoteDto;
import com.nexxserve.nexxclinic.dto.out.VisitDto;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitDepartmentNoteInput;
import com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.model.*;
import com.nexxserve.nexxclinic.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VisitDepartmentLifecycleLockingIntegrationTest {

    @Autowired
    private VisitDepartmentService visitDepartmentService;

    @Autowired
    private VisitDepartmentNoteService visitDepartmentNoteService;

    @Autowired
    private DepartmentFormService departmentFormService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private DepartmentFormRepository departmentFormRepository;

    @Autowired
    private DepartmentFormVersionRepository departmentFormVersionRepository;

    @Autowired
    private VisitBillingRepository visitBillingRepository;

    @Autowired
    private VisitDepartmentBillingRepository visitDepartmentBillingRepository;

    @Autowired
    private VisitService visitService;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;

    @Autowired
    private VisitBillingItemRepository visitBillingItemRepository;

    @Autowired
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

    @Autowired
    private VisitDepartmentDiagnosisRepository visitDepartmentDiagnosisRepository;

    @Autowired
    private VisitDepartmentMedicationRepository visitDepartmentMedicationRepository;

    @Autowired
    private ConsultationAnswerRepository consultationAnswerRepository;

    @Autowired
    private VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository visitBillingVersionRepository;

    @Autowired
    private VisitBillingPaymentRepository visitBillingPaymentRepository;

    private Patient patient;
    private Department department;
    private Product product;
    private DepartmentForm form;
    private DepartmentFormVersion formVersion;
    private Worker clinicianWorker;
    private AuthenticatedUser authUser;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setFullName("Jane Doe");
        patient.setDateOfBirth(LocalDate.of(1995, 5, 15));
        patient.setGender(Gender.FEMALE);
        patient = patientRepository.save(patient);

        department = new Department();
        department.setName("Clinical Dept " + UUID.randomUUID());
        department = departmentRepository.save(department);

        product = new Product();
        product.setName("Paracetamol " + UUID.randomUUID());
        product.setCode("MED-" + UUID.randomUUID().toString().substring(0, 8));
        product.setType(ProductType.DRUG);
        product.setUnit(ProductUnit.PIECE);
        product.setMetadata("{}");
        product.setClinicPrice(BigDecimal.valueOf(10.0));
        product = productRepository.save(product);

        form = new DepartmentForm();
        form.setDepartment(department);
        form.setTitle("Consultation Form " + UUID.randomUUID());
        form.setStatus(FormStatus.FINAL);
        form.setCurrentVersionNumber("1.0.0");
        form.setFormData("{}");
        form = departmentFormRepository.save(form);

        formVersion = new DepartmentFormVersion();
        formVersion.setForm(form);
        formVersion.setVersionNumber("1.0.0");
        formVersion.setTitle(form.getTitle());
        formVersion.setFormData("{}");
        formVersion.setStatus(FormStatus.FINAL);
        formVersion = departmentFormVersionRepository.save(formVersion);

        clinicianWorker = new Worker();
        clinicianWorker.setFirstName("Dr.");
        clinicianWorker.setLastName("Clinician");
        clinicianWorker.setUsername("dr_clinician_" + UUID.randomUUID());
        clinicianWorker.setEmail("dr_" + UUID.randomUUID() + "@clinic.test");
        clinicianWorker.setPasswordHash("hash");
        clinicianWorker.setAccountStatus(AccountStatus.ACTIVE);
        clinicianWorker.setRoles(Set.of(RoleName.CLINICIAN));
        clinicianWorker = workerRepository.save(clinicianWorker);

        authUser = new AuthenticatedUser(
                clinicianWorker.getId(),
                clinicianWorker.getUsername(),
                Set.of(RoleName.CLINICIAN),
                "mock-token",
                Instant.now()
        );
    }

    @Test
    void testCompletedDepartmentAllowsNotesConsultationMedsDiagnostics_ButBlocksProducts() {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.CREATED);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment visitDept = new VisitDepartment();
        visitDept.setVisit(visit);
        visitDept.setDepartment(department);
        visitDept.setStatus(VisitDepartmentStatus.COMPLETED);
        visitDept = visitDepartmentRepository.save(visitDept);

        // Add dummy billing container and department billing
        VisitBilling billing = new VisitBilling();
        billing.setVisit(visit);
        billing = visitBillingRepository.save(billing);

        VisitDepartmentBilling deptBilling = new VisitDepartmentBilling();
        deptBilling.setVisitBilling(billing);
        deptBilling.setVisitDepartment(visitDept);
        deptBilling.setStatus(VisitBillingStatus.PAID);
        deptBilling.setTotalAmount(BigDecimal.TEN);
        deptBilling.setInsuranceCoveredAmount(BigDecimal.ZERO);
        deptBilling.setPatientPayableAmount(BigDecimal.TEN);
        deptBilling.setPaidAmount(BigDecimal.TEN);
        deptBilling.setOutstandingAmount(BigDecimal.ZERO);
        visitDepartmentBillingRepository.save(deptBilling);

        // 1. Adding Note in COMPLETED status should succeed
        AddVisitDepartmentNoteInput noteInput = new AddVisitDepartmentNoteInput(
                visitDept.getId(),
                null,
                NoteType.CONSULTATION,
                "Follow up clinical note after visit completion"
        );
        ApiResponse<VisitDepartmentNoteDto> noteResponse = visitDepartmentNoteService.addVisitDepartmentNote(noteInput, authUser);
        assertEquals(ResponseStatus.SUCCESS, noteResponse.status(), "Notes must be allowed when department is COMPLETED");

        // 2. Submitting Consultation Answer in COMPLETED status should succeed
        ConsultationAnswersInput formInput = new ConsultationAnswersInput(
                UUID.randomUUID(),
                visit.getId(),
                patient.getId(),
                department.getId(),
                form.getId(),
                "1.0.0",
                AnswerStatus.DRAFT,
                "{\"symptoms\":\"resolved\"}"
        );
        ApiResponse formResponse = departmentFormService.upsertConsultationAnswers(formInput, authUser);
        assertEquals(ResponseStatus.SUCCESS, formResponse.status(), "Form answers must be allowed when department is COMPLETED");

        // 3. Adding Diagnosis in COMPLETED status should succeed
        AddDiagnosisInput diagInput = new AddDiagnosisInput(
                visitDept.getId().toString(),
                "Common Cold",
                "1A00"
        );
        ApiResponse<VisitDepartmentDto> diagResponse = visitDepartmentService.addDiagnosisToVisitDepartment(diagInput);
        assertEquals(ResponseStatus.SUCCESS, diagResponse.status(), "Diagnostics must be allowed when department is COMPLETED");

        // 4. Adding Medication in COMPLETED status should succeed
        AddMedicationInput medInput = new AddMedicationInput(
                visitDept.getId().toString(),
                "Amoxicillin 500mg",
                "Take 1 tablet 3 times a day"
        );
        ApiResponse<VisitDepartmentDto> medResponse = visitDepartmentService.addMedicationToVisitDepartment(medInput);
        assertEquals(ResponseStatus.SUCCESS, medResponse.status(), "Medications must be allowed when department is COMPLETED");

        // 5. Adding Product in COMPLETED status should fail (locked unless in DEPARTMENT_EDITING)
        CreateVisitDepartmentProductInput prodInput = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                product.getId(),
                null,
                BigDecimal.ONE,
                null
        );
        ApiResponse<VisitDepartmentDto> prodResponse = visitDepartmentService.addVisitDepartmentProduct(prodInput, authUser);
        assertEquals(ResponseStatus.ERROR, prodResponse.status(), "Products must be locked when department is COMPLETED");
    }

    @Test
    void testFinalisedDepartmentBlocksEverything() {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.CREATED);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment visitDept = new VisitDepartment();
        visitDept.setVisit(visit);
        visitDept.setDepartment(department);
        visitDept.setStatus(VisitDepartmentStatus.FINALISED);
        visitDept = visitDepartmentRepository.save(visitDept);

        AuthenticatedUser authUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "doctor",
                Set.of(RoleName.CLINICIAN),
                "mock-token",
                Instant.now()
        );

        // 1. Note should be blocked
        AddVisitDepartmentNoteInput noteInput = new AddVisitDepartmentNoteInput(
                visitDept.getId(),
                null,
                NoteType.CONSULTATION,
                "Note on finalised"
        );
        ApiResponse<VisitDepartmentNoteDto> noteResponse = visitDepartmentNoteService.addVisitDepartmentNote(noteInput, authUser);
        assertEquals(ResponseStatus.ERROR, noteResponse.status(), "Notes must be blocked when FINALISED");

        // 2. Form answers should be blocked
        ConsultationAnswersInput formInput = new ConsultationAnswersInput(
                UUID.randomUUID(),
                visit.getId(),
                patient.getId(),
                department.getId(),
                form.getId(),
                "1.0.0",
                AnswerStatus.DRAFT,
                "{\"symptoms\":\"test\"}"
        );
        ApiResponse formResponse = departmentFormService.upsertConsultationAnswers(formInput, authUser);
        assertEquals(ResponseStatus.ERROR, formResponse.status(), "Form answers must be blocked when FINALISED");

        // 3. Diagnosis should be blocked
        AddDiagnosisInput diagInput = new AddDiagnosisInput(
                visitDept.getId().toString(),
                "Flu",
                null
        );
        ApiResponse<VisitDepartmentDto> diagResponse = visitDepartmentService.addDiagnosisToVisitDepartment(diagInput);
        assertEquals(ResponseStatus.ERROR, diagResponse.status(), "Diagnostics must be blocked when FINALISED");

        // 4. Medication should be blocked
        AddMedicationInput medInput = new AddMedicationInput(
                visitDept.getId().toString(),
                "Ibuprofen",
                "As needed"
        );
        ApiResponse<VisitDepartmentDto> medResponse = visitDepartmentService.addMedicationToVisitDepartment(medInput);
        assertEquals(ResponseStatus.ERROR, medResponse.status(), "Medications must be blocked when FINALISED");
    }

    @Test
    void testVisitStatusOnlyLimitsAddingNewDepartments() {
        Department newDept = new Department();
        newDept.setName("Another Dept " + UUID.randomUUID());
        newDept = departmentRepository.save(newDept);

        AuthenticatedUser authUser = new AuthenticatedUser(
                UUID.randomUUID(),
                "doctor",
                Set.of(RoleName.CLINICIAN),
                "mock-token",
                Instant.now()
        );

        // 1. Visit in CREATED -> adding department succeeds
        Visit visitCreated = new Visit();
        visitCreated.setPatient(patient);
        visitCreated.setStatus(VisitStatus.CREATED);
        visitCreated.setVisitDate(LocalDateTime.now());
        visitCreated = visitRepository.save(visitCreated);

        ApiResponse<VisitDto> addDeptRes1 = visitDepartmentService.addVisitDepartment(
                visitCreated.getId(),
                newDept.getId(),
                null,
                EncounterType.OUTPATIENT,
                null,
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addDeptRes1.status(), "Adding department to CREATED visit must succeed");

        // 2. Visit in IN_PROGRESS -> adding department succeeds
        Department dept3 = new Department();
        dept3.setName("Third Dept " + UUID.randomUUID());
        dept3 = departmentRepository.save(dept3);

        Visit visitInProgress = new Visit();
        visitInProgress.setPatient(patient);
        visitInProgress.setStatus(VisitStatus.IN_PROGRESS);
        visitInProgress.setVisitDate(LocalDateTime.now());
        visitInProgress = visitRepository.save(visitInProgress);

        ApiResponse<VisitDto> addDeptRes2 = visitDepartmentService.addVisitDepartment(
                visitInProgress.getId(),
                dept3.getId(),
                null,
                EncounterType.OUTPATIENT,
                null,
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addDeptRes2.status(), "Adding department to IN_PROGRESS visit must succeed");

        // 3. Visit in COMPLETED -> adding department fails
        Department dept4 = new Department();
        dept4.setName("Fourth Dept " + UUID.randomUUID());
        dept4 = departmentRepository.save(dept4);

        Visit visitCompleted = new Visit();
        visitCompleted.setPatient(patient);
        visitCompleted.setStatus(VisitStatus.COMPLETED);
        visitCompleted.setVisitDate(LocalDateTime.now());
        visitCompleted = visitRepository.save(visitCompleted);

        ApiResponse<VisitDto> addDeptRes3 = visitDepartmentService.addVisitDepartment(
                visitCompleted.getId(),
                dept4.getId(),
                null,
                EncounterType.OUTPATIENT,
                null,
                authUser
        );
        assertEquals(ResponseStatus.ERROR, addDeptRes3.status(), "Adding department to COMPLETED visit must fail");

        // 4. Visit in CANCELLED -> adding department fails
        Department dept5 = new Department();
        dept5.setName("Fifth Dept " + UUID.randomUUID());
        dept5 = departmentRepository.save(dept5);

        Visit visitCancelled = new Visit();
        visitCancelled.setPatient(patient);
        visitCancelled.setStatus(VisitStatus.CANCELLED);
        visitCancelled.setVisitDate(LocalDateTime.now());
        visitCancelled = visitRepository.save(visitCancelled);

        ApiResponse<VisitDto> addDeptRes4 = visitDepartmentService.addVisitDepartment(
                visitCancelled.getId(),
                dept5.getId(),
                null,
                EncounterType.OUTPATIENT,
                null,
                authUser
        );
        assertEquals(ResponseStatus.ERROR, addDeptRes4.status(), "Adding department to CANCELLED visit must fail");
    }

    @Test
    void testCancelAndRemoveVisitDepartmentPermissions() {
        Worker nurseWorker = new Worker();
        nurseWorker.setFirstName("Nurse");
        nurseWorker.setLastName("Joy");
        nurseWorker.setUsername("nurse_" + UUID.randomUUID());
        nurseWorker.setEmail("nurse_" + UUID.randomUUID() + "@clinic.test");
        nurseWorker.setPasswordHash("hash");
        nurseWorker.setAccountStatus(AccountStatus.ACTIVE);
        nurseWorker.setRoles(Set.of(RoleName.NURSE));
        nurseWorker = workerRepository.save(nurseWorker);

        AuthenticatedUser nurseAuth = new AuthenticatedUser(
                nurseWorker.getId(),
                nurseWorker.getUsername(),
                Set.of(RoleName.NURSE),
                "mock-token",
                Instant.now()
        );

        Worker managerWorker = new Worker();
        managerWorker.setFirstName("Clinic");
        managerWorker.setLastName("Manager");
        managerWorker.setUsername("mgr_" + UUID.randomUUID());
        managerWorker.setEmail("mgr_" + UUID.randomUUID() + "@clinic.test");
        managerWorker.setPasswordHash("hash");
        managerWorker.setAccountStatus(AccountStatus.ACTIVE);
        managerWorker.setRoles(Set.of(RoleName.MANAGER));
        managerWorker = workerRepository.save(managerWorker);

        AuthenticatedUser managerAuth = new AuthenticatedUser(
                managerWorker.getId(),
                managerWorker.getUsername(),
                Set.of(RoleName.MANAGER),
                "mock-token",
                Instant.now()
        );

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        Department dept = new Department();
        dept.setName("Test Dept " + UUID.randomUUID());
        dept = departmentRepository.save(dept);

        ApiResponse<VisitDto> addDeptRes = visitDepartmentService.addVisitDepartment(
                visit.getId(),
                dept.getId(),
                null,
                EncounterType.OUTPATIENT,
                null,
                nurseAuth
        );
        assertEquals(ResponseStatus.SUCCESS, addDeptRes.status());
        UUID visitDeptId = addDeptRes.data().departments().get(0).id();

        // 1. Nurse cannot delete department
        ApiResponse<VisitDto> deleteRes = visitDepartmentService.removeVisitDepartment(visitDeptId, nurseAuth);
        assertEquals(ResponseStatus.ERROR, deleteRes.status(), "Nurse cannot delete department");

        // 2. Nurse can cancel pending department with no products
        ApiResponse updateStatusRes = visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(visitDeptId, VisitDepartmentStatus.CANCELLED),
                nurseAuth
        );
        assertEquals(ResponseStatus.SUCCESS, updateStatusRes.status(), "Nurse can cancel pending department with no products");

        // 3. Manager can delete cancelled department
        ApiResponse<VisitDto> mgrDeleteRes = visitDepartmentService.removeVisitDepartment(visitDeptId, managerAuth);
        assertEquals(ResponseStatus.SUCCESS, mgrDeleteRes.status(), "Manager can delete department");

        // 4. Test active department permissions
        Department dept2 = new Department();
        dept2.setName("Active Dept " + UUID.randomUUID());
        dept2 = departmentRepository.save(dept2);
        final UUID dept2Id = dept2.getId();

        ApiResponse<VisitDto> addDept2Res = visitDepartmentService.addVisitDepartment(
                visit.getId(),
                dept2.getId(),
                null,
                EncounterType.OUTPATIENT,
                clinicianWorker.getId(),
                nurseAuth
        );
        assertEquals(ResponseStatus.SUCCESS, addDept2Res.status());
        UUID activeDeptId = addDept2Res.data().departments().stream()
                .filter(d -> d.department().id().equals(dept2Id))
                .findFirst().get().id();

        // Set status to ACTIVE
        visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(activeDeptId, VisitDepartmentStatus.ACTIVE),
                authUser
        );

        // Add product to active department
        visitDepartmentService.addVisitDepartmentProduct(
                new com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput(
                        visit.getId(),
                        dept2Id,
                        product.getId(),
                        null,
                        BigDecimal.ONE,
                        null
                ),
                authUser
        );

        // Nurse (not assigned processor, not manager) cannot cancel active department with products
        ApiResponse nurseCancelActiveRes = visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(activeDeptId, VisitDepartmentStatus.CANCELLED),
                nurseAuth
        );
        assertEquals(ResponseStatus.ERROR, nurseCancelActiveRes.status(), "Unassigned nurse cannot cancel active department");

        // Assigned processor (clinician) CAN cancel active department
        ApiResponse processorCancelActiveRes = visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(activeDeptId, VisitDepartmentStatus.CANCELLED),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, processorCancelActiveRes.status(), "Assigned processor can cancel active department");
    }

    @Test
    void testManagerCanRemoveBilledDepartmentWithAllDependentsCleanly() {
        // Setup workers
        Worker managerWorker = new Worker();
        managerWorker.setFirstName("Manager");
        managerWorker.setLastName("User");
        managerWorker.setUsername("mgr_" + UUID.randomUUID());
        managerWorker.setEmail("mgr_" + UUID.randomUUID() + "@clinic.test");
        managerWorker.setPasswordHash("hash");
        managerWorker.setAccountStatus(AccountStatus.ACTIVE);
        managerWorker.setRoles(Set.of(RoleName.MANAGER));
        managerWorker = workerRepository.save(managerWorker);

        AuthenticatedUser managerAuth = new AuthenticatedUser(
                managerWorker.getId(),
                managerWorker.getUsername(),
                Set.of(RoleName.MANAGER),
                "mock-token",
                Instant.now()
        );

        Worker nurseWorker = new Worker();
        nurseWorker.setFirstName("Nurse");
        nurseWorker.setLastName("User");
        nurseWorker.setUsername("nurse_" + UUID.randomUUID());
        nurseWorker.setEmail("nurse_" + UUID.randomUUID() + "@clinic.test");
        nurseWorker.setPasswordHash("hash");
        nurseWorker.setAccountStatus(AccountStatus.ACTIVE);
        nurseWorker.setRoles(Set.of(RoleName.NURSE));
        nurseWorker = workerRepository.save(nurseWorker);

        AuthenticatedUser nurseAuth = new AuthenticatedUser(
                nurseWorker.getId(),
                nurseWorker.getUsername(),
                Set.of(RoleName.NURSE),
                "mock-token",
                Instant.now()
        );

        // 1. Create a Visit and add two departments
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now().minusHours(2));
        visit = visitRepository.save(visit);

        // Department 1: Billed department to delete
        Department dept1 = new Department();
        dept1.setName("Dept To Delete " + UUID.randomUUID());
        dept1 = departmentRepository.save(dept1);

        final UUID targetDept1Id = dept1.getId();
        ApiResponse<VisitDto> addDept1Res = visitDepartmentService.addVisitDepartment(
                visit.getId(),
                targetDept1Id,
                null,
                EncounterType.OUTPATIENT,
                clinicianWorker.getId(),
                managerAuth
        );
        assertEquals(ResponseStatus.SUCCESS, addDept1Res.status());
        UUID dept1Id = addDept1Res.data().departments().stream()
                .filter(d -> d.department().id().equals(targetDept1Id))
                .findFirst().get().id();

        // Department 2: Remaining department to keep
        Department dept2 = new Department();
        dept2.setName("Dept To Keep " + UUID.randomUUID());
        dept2 = departmentRepository.save(dept2);
        final UUID targetDept2Id = dept2.getId();

        ApiResponse<VisitDto> addDept2Res = visitDepartmentService.addVisitDepartment(
                visit.getId(),
                targetDept2Id,
                null,
                EncounterType.OUTPATIENT,
                clinicianWorker.getId(),
                managerAuth
        );
        assertEquals(ResponseStatus.SUCCESS, addDept2Res.status());
        UUID dept2Id = addDept2Res.data().departments().stream()
                .filter(d -> d.department().id().equals(targetDept2Id))
                .findFirst().get().id();

        // 2. Add products, diagnoses, medications, and notes to Dept 1
        ApiResponse addProdRes = visitDepartmentService.addVisitDepartmentProduct(
                new CreateVisitDepartmentProductInput(visit.getId(), targetDept1Id, product.getId(), null, BigDecimal.valueOf(2.0), VisitProductStatus.PENDING),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addProdRes.status(), "Product should be added: " + addProdRes.message());

        ApiResponse addDiagRes = visitDepartmentService.addDiagnosisToVisitDepartment(
                new AddDiagnosisInput(dept1Id.toString(), "Test Diagnosis", "1A00")
        );
        assertEquals(ResponseStatus.SUCCESS, addDiagRes.status());

        ApiResponse addMedRes = visitDepartmentService.addMedicationToVisitDepartment(
                new AddMedicationInput(dept1Id.toString(), "Test Medication", "1 tab daily")
        );
        assertEquals(ResponseStatus.SUCCESS, addMedRes.status());

        ApiResponse addNoteRes = visitDepartmentNoteService.addVisitDepartmentNote(
                new AddVisitDepartmentNoteInput(dept1Id, null, NoteType.CONSULTATION, "Clinical note for department"),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addNoteRes.status());

        // 3. Add Consultation Answers for Dept 1
        DepartmentForm form1 = new DepartmentForm();
        form1.setDepartment(dept1);
        form1.setTitle("Consultation Form " + UUID.randomUUID());
        form1.setStatus(FormStatus.FINAL);
        form1.setCurrentVersionNumber("1.0.0");
        form1.setFormData("{}");
        form1 = departmentFormRepository.save(form1);

        DepartmentFormVersion form1Version = new DepartmentFormVersion();
        form1Version.setForm(form1);
        form1Version.setVersionNumber("1.0.0");
        form1Version.setTitle(form1.getTitle());
        form1Version.setFormData("{}");
        form1Version.setStatus(FormStatus.FINAL);
        form1Version = departmentFormVersionRepository.save(form1Version);

        ApiResponse answerRes = departmentFormService.upsertConsultationAnswers(
                new ConsultationAnswersInput(
                        UUID.randomUUID(),
                        visit.getId(),
                        patient.getId(),
                        dept1.getId(),
                        form1.getId(),
                        form1Version.getVersionNumber(),
                        AnswerStatus.FINAL,
                        "{\"symptoms\":\"headache\"}"
                ),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, answerRes.status(), "Consultation answer should be saved: " + answerRes.message());

        // 4. Create full billing container, department billing, insurance billing, and billing items for Dept 1
        VisitDepartment vd1 = visitDepartmentRepository.findById(dept1Id).orElseThrow();
        VisitDepartmentProduct vdp1 = visitDepartmentProductRepository.findByVisitDepartmentId(dept1Id).get(0);

        VisitBilling vb = new VisitBilling();
        vb.setVisit(visit);
        vb = visitBillingRepository.save(vb);

        VisitDepartmentBilling vdb = new VisitDepartmentBilling();
        vdb.setVisitBilling(vb);
        vdb.setVisitDepartment(vd1);
        vdb.setStatus(VisitBillingStatus.PAID);
        vdb.setTotalAmount(BigDecimal.valueOf(20.0));
        vdb.setPatientPayableAmount(BigDecimal.valueOf(20.0));
        vdb.setPaidAmount(BigDecimal.valueOf(20.0));
        vdb.setOutstandingAmount(BigDecimal.ZERO);
        vdb.setInsuranceCoveredAmount(BigDecimal.ZERO);
        vdb = visitDepartmentBillingRepository.save(vdb);

        DepartmentInsuranceBilling dib = new DepartmentInsuranceBilling();
        dib.setVisitDepartmentBilling(vdb);
        dib.setStatus(VisitBillingStatus.PAID);
        dib.setTotalAmount(BigDecimal.valueOf(20.0));
        dib.setPatientPayableAmount(BigDecimal.valueOf(20.0));
        dib.setPaidAmount(BigDecimal.valueOf(20.0));
        dib.setOutstandingAmount(BigDecimal.ZERO);
        dib.setInsuranceCoveredAmount(BigDecimal.ZERO);
        dib = departmentInsuranceBillingRepository.save(dib);

        VisitBillingItem item = new VisitBillingItem();
        item.setDepartmentInsuranceBilling(dib);
        item.setVisitDepartmentProduct(vdp1);
        item.setQuantitySnapshot(BigDecimal.valueOf(2.0));
        item.setUnitPriceSnapshot(BigDecimal.valueOf(10.0));
        item.setLineTotal(BigDecimal.valueOf(20.0));
        item.setPatientPayableAmount(BigDecimal.valueOf(20.0));
        item.setInsuranceCoveredAmount(BigDecimal.ZERO);
        visitBillingItemRepository.save(item);

        VisitBillingPayment payment = new VisitBillingPayment();
        payment.setVisitDepartmentBilling(vdb);
        payment.setDepartmentInsuranceBilling(dib);
        payment.setAmount(BigDecimal.valueOf(20.0));
        payment.setPaymentMethod(PaymentMethod.CASH);
        visitBillingPaymentRepository.save(payment);

        com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion billingVersion =
                new com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion();
        billingVersion.setVisit(visit);
        billingVersion.setVersion(1);
        billingVersion = visitBillingVersionRepository.save(billingVersion);

        com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot snapshot =
                new com.nexxserve.nexxclinic.entity.billing.VisitDepartmentProductSnapshot();
        snapshot.setVisitDepartment(vd1);
        snapshot.setVisitDepartmentProductId(vdp1.getId());
        snapshot.setProduct(product);
        snapshot.setQuantity(BigDecimal.valueOf(2.0));
        snapshot.setUnitPrice(BigDecimal.valueOf(10.0));
        snapshot.setStatus(VisitProductStatus.BILLED);
        snapshot.setBillingVersion(billingVersion);
        visitDepartmentProductSnapshotRepository.save(snapshot);


        // 5. Verify non-admin/manager cannot remove the department
        ApiResponse<VisitDto> nurseDeleteRes = visitDepartmentService.removeVisitDepartment(dept1Id, nurseAuth);
        assertEquals(ResponseStatus.ERROR, nurseDeleteRes.status(), "Nurse should be rejected");
        assertTrue(nurseDeleteRes.message().contains("Only administrators and managers"));

        // 6. Manager removes the billed department with all its dependents
        ApiResponse<VisitDto> mgrDeleteRes = visitDepartmentService.removeVisitDepartment(dept1Id, managerAuth);
        assertEquals(ResponseStatus.SUCCESS, mgrDeleteRes.status(), "Manager should successfully remove department");

        // 7. Verify Dept 1 is completely gone from the returned VisitDto
        VisitDto updatedVisitDto = mgrDeleteRes.data();
        assertNotNull(updatedVisitDto);
        assertEquals(1, updatedVisitDto.departments().size());
        assertEquals(dept2Id, updatedVisitDto.departments().get(0).id());

        // 8. Verify all database records for Dept 1 were permanently and cleanly cascaded
        assertFalse(visitDepartmentRepository.existsById(dept1Id), "VisitDepartment should be deleted");
        assertTrue(visitDepartmentProductRepository.findByVisitDepartmentId(dept1Id).isEmpty(), "Products should be deleted");
        assertTrue(visitDepartmentDiagnosisRepository.findByVisitDepartmentId(dept1Id).isEmpty(), "Diagnoses should be deleted");
        assertTrue(visitDepartmentMedicationRepository.findByVisitDepartmentId(dept1Id).isEmpty(), "Medications should be deleted");
        assertTrue(visitDepartmentNoteService.buildNotesSummary(dept1Id, managerAuth).totalNotes() == 0, "Notes should be deleted");
        assertTrue(visitDepartmentBillingRepository.findByVisitDepartmentId(dept1Id).isEmpty(), "Department billing should be deleted");
        assertTrue(departmentInsuranceBillingRepository.findByVisitDepartmentBillingId(vdb.getId()).isEmpty(), "Insurance billing should be deleted");
        assertTrue(visitBillingItemRepository.findByDepartmentInsuranceBillingId(dib.getId()).isEmpty(), "Billing items should be deleted");
        assertTrue(visitBillingItemRepository.findByVisitDepartmentProductId(vdp1.getId()).isEmpty(), "Product billing items should be deleted");
        assertTrue(visitDepartmentProductSnapshotRepository.findByVisitDepartmentId(dept1Id).isEmpty(), "Snapshots should be deleted");
        assertTrue(consultationAnswerRepository.findByVisitIdAndDepartmentIdOrderByUpdatedAtDesc(visit.getId(), dept1.getId()).isEmpty(), "Consultation answers should be deleted");
    }

    @Test
    void testDoctorCannotSetDepartmentStatusToCompleted() {
        Visit testVisit = new Visit();
        testVisit.setPatient(patient);
        testVisit.setStatus(VisitStatus.IN_PROGRESS);
        testVisit.setVisitDate(LocalDateTime.now());
        testVisit = visitRepository.save(testVisit);

        Department testDept = new Department();
        testDept.setName("Clinician Complete Test " + UUID.randomUUID());
        testDept = departmentRepository.save(testDept);

        ApiResponse<VisitDto> addDeptRes = visitDepartmentService.addVisitDepartment(
                testVisit.getId(),
                testDept.getId(),
                null,
                EncounterType.OUTPATIENT,
                clinicianWorker.getId(),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addDeptRes.status());
        UUID visitDeptId = addDeptRes.data().departments().get(0).id();

        // Clinician tries to set COMPLETED -> should be rejected
        ApiResponse clinicianCompleteRes = visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(visitDeptId, VisitDepartmentStatus.COMPLETED),
                authUser // authUser has RoleName.CLINICIAN
        );
        assertEquals(ResponseStatus.ERROR, clinicianCompleteRes.status());
        assertTrue(clinicianCompleteRes.message().contains("Doctors and clinical staff cannot set department status to completed"));
    }

    @Test
    void testConsultationSaveSetsBillingOnlyWhenUnbilledProductsExist() {
        Visit testVisit = new Visit();
        testVisit.setPatient(patient);
        testVisit.setStatus(VisitStatus.IN_PROGRESS);
        testVisit.setVisitDate(LocalDateTime.now());
        testVisit = visitRepository.save(testVisit);

        Department testDept = new Department();
        testDept.setName("Unbilled Check Dept " + UUID.randomUUID());
        testDept = departmentRepository.save(testDept);

        ApiResponse<VisitDto> addDeptRes = visitDepartmentService.addVisitDepartment(
                testVisit.getId(),
                testDept.getId(),
                null,
                EncounterType.OUTPATIENT,
                clinicianWorker.getId(),
                authUser
        );
        assertEquals(ResponseStatus.SUCCESS, addDeptRes.status());
        UUID visitDeptId = addDeptRes.data().departments().get(0).id();

        // Set to ACTIVE
        visitDepartmentService.updateVisitDepartmentStatus(
                new com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput(visitDeptId, VisitDepartmentStatus.ACTIVE),
                authUser
        );

        // Case 1: No products exist. Completing consultation should NOT change status (remains ACTIVE, not BILLING, not COMPLETED)
        DepartmentForm testForm = new DepartmentForm();
        testForm.setDepartment(testDept);
        testForm.setTitle("Form " + UUID.randomUUID());
        testForm.setStatus(FormStatus.FINAL);
        testForm.setCurrentVersionNumber("1.0.0");
        testForm.setFormData("{}");
        testForm = departmentFormRepository.save(testForm);

        DepartmentFormVersion version = new DepartmentFormVersion();
        version.setForm(testForm);
        version.setVersionNumber("1.0.0");
        version.setTitle(testForm.getTitle());
        version.setFormData("{}");
        version.setStatus(FormStatus.FINAL);
        version = departmentFormVersionRepository.save(version);

        com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput input =
                new com.nexxserve.nexxclinic.graphql.input.ConsultationAnswersInput(
                        UUID.randomUUID(),
                        testVisit.getId(),
                        patient.getId(),
                        testDept.getId(),
                        testForm.getId(),
                        "1.0.0",
                        AnswerStatus.FINAL,
                        "{\"field\":\"value\"}"
                );

        ApiResponse<VisitDto> completeRes = visitService.completeVisit(input, true, authUser);
        assertEquals(ResponseStatus.SUCCESS, completeRes.status());

        VisitDepartment deptAfterNoProd = visitDepartmentRepository.findById(visitDeptId).orElseThrow();
        assertEquals(VisitDepartmentStatus.ACTIVE, deptAfterNoProd.getStatus(), "Should remain ACTIVE when no products exist");

        // Case 2: Unbilled product added. Completing consultation (or draft save) should change status to BILLING
        visitDepartmentService.addVisitDepartmentProduct(
                new com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput(
                        testVisit.getId(),
                        testDept.getId(),
                        product.getId(),
                        null,
                        BigDecimal.ONE,
                        null
                ),
                authUser
        );

        ApiResponse<VisitDto> draftSaveRes = visitService.completeVisit(input, false, authUser);
        assertEquals(ResponseStatus.SUCCESS, draftSaveRes.status());

        VisitDepartment deptAfterUnbilled = visitDepartmentRepository.findById(visitDeptId).orElseThrow();
        assertEquals(VisitDepartmentStatus.BILLING, deptAfterUnbilled.getStatus(), "Should transition to BILLING when unbilled product exists");
    }
}
