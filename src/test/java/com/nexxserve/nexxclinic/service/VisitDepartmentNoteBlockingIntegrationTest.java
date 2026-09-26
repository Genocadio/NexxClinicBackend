package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.*;
import com.nexxserve.nexxclinic.graphql.input.*;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class VisitDepartmentNoteBlockingIntegrationTest {

    @Autowired
    private VisitBillingService visitBillingService;

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
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

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
    private WorkerRepository workerRepository;

    private Patient patient;
    private Department department;
    private Product product;
    private DepartmentForm form;
    private DepartmentFormVersion formVersion;

    private Worker clinicianWorker;
    private Worker financeWorker;
    private Worker noteCreatorWorker;

    private AuthenticatedUser clinicianAuth;
    private AuthenticatedUser financeAuth;
    private AuthenticatedUser adminAuth;
    private AuthenticatedUser managerAuth;

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Smith");
        patient.setFullName("John Smith");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        department = new Department();
        department.setName("Cardiology " + UUID.randomUUID());
        department = departmentRepository.save(department);

        product = new Product();
        product.setName("Consultation " + UUID.randomUUID());
        product.setCode("CONS-" + UUID.randomUUID().toString().substring(0, 8));
        product.setType(ProductType.MEDICAL_ACT);
        product.setUnit(ProductUnit.PIECE);
        product.setClinicPrice(BigDecimal.valueOf(50.0));
        product.setMetadata("{}");
        product = productRepository.save(product);

        form = new DepartmentForm();
        form.setDepartment(department);
        form.setTitle("Cardio Assessment");
        form = departmentFormRepository.save(form);

        formVersion = new DepartmentFormVersion();
        formVersion.setForm(form);
        formVersion.setTitle(form.getTitle());
        formVersion.setVersionNumber("1.0");
        formVersion.setStatus(FormStatus.FINAL);
        formVersion.setFormData("{}");
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

        financeWorker = new Worker();
        financeWorker.setFirstName("Fin");
        financeWorker.setLastName("Officer");
        financeWorker.setUsername("finance_" + UUID.randomUUID());
        financeWorker.setEmail("fin_" + UUID.randomUUID() + "@clinic.test");
        financeWorker.setPasswordHash("hash");
        financeWorker.setAccountStatus(AccountStatus.ACTIVE);
        financeWorker.setRoles(Set.of(RoleName.FINANCE));
        financeWorker = workerRepository.save(financeWorker);

        noteCreatorWorker = new Worker();
        noteCreatorWorker.setFirstName("Nurse");
        noteCreatorWorker.setLastName("Creator");
        noteCreatorWorker.setUsername("nurse_" + UUID.randomUUID());
        noteCreatorWorker.setEmail("nurse_" + UUID.randomUUID() + "@clinic.test");
        noteCreatorWorker.setPasswordHash("hash");
        noteCreatorWorker.setAccountStatus(AccountStatus.ACTIVE);
        noteCreatorWorker.setRoles(Set.of(RoleName.NURSE));
        noteCreatorWorker = workerRepository.save(noteCreatorWorker);

        Worker adminWorker = new Worker();
        adminWorker.setFirstName("Admin");
        adminWorker.setLastName("User");
        adminWorker.setUsername("admin_" + UUID.randomUUID());
        adminWorker.setEmail("admin_" + UUID.randomUUID() + "@clinic.test");
        adminWorker.setPasswordHash("hash");
        adminWorker.setAccountStatus(AccountStatus.ACTIVE);
        adminWorker.setRoles(Set.of(RoleName.ADMIN));
        adminWorker = workerRepository.save(adminWorker);

        Worker managerWorker = new Worker();
        managerWorker.setFirstName("Manager");
        managerWorker.setLastName("User");
        managerWorker.setUsername("manager_" + UUID.randomUUID());
        managerWorker.setEmail("manager_" + UUID.randomUUID() + "@clinic.test");
        managerWorker.setPasswordHash("hash");
        managerWorker.setAccountStatus(AccountStatus.ACTIVE);
        managerWorker.setRoles(Set.of(RoleName.MANAGER));
        managerWorker = workerRepository.save(managerWorker);

        clinicianAuth = new AuthenticatedUser(clinicianWorker.getId(), clinicianWorker.getUsername(), Set.of(RoleName.CLINICIAN), "token", Instant.now());
        financeAuth = new AuthenticatedUser(financeWorker.getId(), financeWorker.getUsername(), Set.of(RoleName.FINANCE), "token", Instant.now());
        adminAuth = new AuthenticatedUser(adminWorker.getId(), adminWorker.getUsername(), Set.of(RoleName.ADMIN), "token", Instant.now());
        managerAuth = new AuthenticatedUser(managerWorker.getId(), managerWorker.getUsername(), Set.of(RoleName.MANAGER), "token", Instant.now());
    }

    private Visit createVisitWithDepartment() {
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.ACTIVE);
        vd = visitDepartmentRepository.save(vd);

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setStatus(VisitProductStatus.PENDING);
        visitDepartmentProductRepository.save(vdp);

        return visit;
    }

    @Test
    void unreadBillingNote_blocksFinance_butAllowsAdminAndManager() {
        Visit visit = createVisitWithDepartment();
        VisitDepartment vd = visitDepartmentRepository.findByVisitId(visit.getId()).get(0);
        VisitDepartmentProduct vdp = visitDepartmentProductRepository.findByVisitDepartmentId(vd.getId()).get(0);

        // Add a BILLING note created by noteCreatorWorker
        AddVisitDepartmentNoteInput noteInput = new AddVisitDepartmentNoteInput(
                vd.getId(),
                null,
                NoteType.BILLING,
                "Important billing discrepancy on Cardiology"
        );
        AuthenticatedUser creatorAuth = new AuthenticatedUser(noteCreatorWorker.getId(), noteCreatorWorker.getUsername(), Set.of(RoleName.NURSE), "token", Instant.now());
        ApiResponse noteRes = visitDepartmentNoteService.addVisitDepartmentNote(noteInput, creatorAuth);
        assertEquals(ResponseStatus.SUCCESS, noteRes.status());

        BillVisitInput.BillVisitDepartmentProductInput prodInput = new BillVisitInput.BillVisitDepartmentProductInput(
                vdp.getId(),
                null,
                BigDecimal.ONE,
                CoverageType.PRIVATE,
                null,
                null,
                null
        );
        BillVisitInput.BillingPaymentInput payment = new BillVisitInput.BillingPaymentInput(
                BigDecimal.valueOf(50.0),
                PaymentMethod.CASH,
                "REF-123"
        );
        BillVisitInput billInput = new BillVisitInput(
                visit.getId(),
                List.of(new BillVisitInput.BillVisitDepartmentInput(
                        vd.getId(),
                        List.of(prodInput),
                        List.of(payment),
                        null,
                        null,
                        null
                ))
        );

        // Finance user tries to bill the visit -> should be blocked by unread note
        ApiResponse finRes = visitBillingService.billVisit(billInput, financeAuth);
        assertEquals(ResponseStatus.ERROR, finRes.status());
        assertTrue(finRes.message().toLowerCase().contains("unread notes"), "Expected error about unread notes but was: " + finRes.message());

        // Admin tries to bill -> exempt from notes block!
        ApiResponse adminRes = visitBillingService.billVisit(billInput, adminAuth);
        assertEquals(ResponseStatus.SUCCESS, adminRes.status(), "Admin billing failed: " + adminRes.message());
    }

    @Test
    void markingBillingNoteViewed_unblocksFinanceBilling() {
        Visit visit = createVisitWithDepartment();
        VisitDepartment vd = visitDepartmentRepository.findByVisitId(visit.getId()).get(0);
        VisitDepartmentProduct vdp = visitDepartmentProductRepository.findByVisitDepartmentId(vd.getId()).get(0);

        AddVisitDepartmentNoteInput noteInput = new AddVisitDepartmentNoteInput(
                vd.getId(),
                null,
                NoteType.BILLING,
                "Please verify coverage"
        );
        AuthenticatedUser creatorAuth = new AuthenticatedUser(noteCreatorWorker.getId(), noteCreatorWorker.getUsername(), Set.of(RoleName.NURSE), "token", Instant.now());
        visitDepartmentNoteService.addVisitDepartmentNote(noteInput, creatorAuth);

        // Mark note viewed for finance user
        ApiResponse markRes = visitDepartmentNoteService.markVisitDepartmentNotesViewed(vd.getId(), financeAuth);
        assertEquals(ResponseStatus.SUCCESS, markRes.status());

        BillVisitInput.BillVisitDepartmentProductInput prodInput = new BillVisitInput.BillVisitDepartmentProductInput(
                vdp.getId(),
                null,
                BigDecimal.ONE,
                CoverageType.PRIVATE,
                null,
                null,
                null
        );
        BillVisitInput.BillingPaymentInput payment = new BillVisitInput.BillingPaymentInput(
                BigDecimal.valueOf(50.0),
                PaymentMethod.CASH,
                "REF-123"
        );
        BillVisitInput billInput = new BillVisitInput(
                visit.getId(),
                List.of(new BillVisitInput.BillVisitDepartmentInput(
                        vd.getId(),
                        List.of(prodInput),
                        List.of(payment),
                        null,
                        null,
                        null
                ))
        );

        // Finance bills -> should succeed now
        ApiResponse finRes = visitBillingService.billVisit(billInput, financeAuth);
        assertEquals(ResponseStatus.SUCCESS, finRes.status(), "Finance billing failed: " + finRes.message());
    }

    @Test
    void unreadDepartmentNote_blocksClinicianConsultationActions_butAllowsAdminAndManager() {
        Visit visit = createVisitWithDepartment();
        VisitDepartment vd = visitDepartmentRepository.findByVisitId(visit.getId()).get(0);

        // Add a department consultation note created by someone else
        AddVisitDepartmentNoteInput noteInput = new AddVisitDepartmentNoteInput(
                vd.getId(),
                null,
                NoteType.CONSULTATION,
                "Patient reported dizziness in waiting room"
        );
        AuthenticatedUser creatorAuth = new AuthenticatedUser(noteCreatorWorker.getId(), noteCreatorWorker.getUsername(), Set.of(RoleName.NURSE), "token", Instant.now());
        visitDepartmentNoteService.addVisitDepartmentNote(noteInput, creatorAuth);

        // 1. Consultation answers submission
        ConsultationAnswersInput answersInput = new ConsultationAnswersInput(
                UUID.randomUUID(),
                visit.getId(),
                patient.getId(),
                department.getId(),
                form.getId(),
                "1.0",
                AnswerStatus.FINAL,
                "{\"q1\":\"normal\"}"
        );
        ApiResponse ansRes = departmentFormService.upsertConsultationAnswers(answersInput, clinicianAuth);
        assertEquals(ResponseStatus.ERROR, ansRes.status());
        assertTrue(ansRes.message().toLowerCase().contains("unread department notes"));

        // Admin submitting answers -> allowed
        ApiResponse adminAnsRes = departmentFormService.upsertConsultationAnswers(answersInput, adminAuth);
        assertEquals(ResponseStatus.SUCCESS, adminAnsRes.status());

        // 2. Add Diagnosis
        AddDiagnosisInput diagInput = new AddDiagnosisInput(vd.getId().toString(), "Hypertension", "BA00");
        ApiResponse diagRes = visitDepartmentService.addDiagnosisToVisitDepartment(diagInput, clinicianAuth);
        assertEquals(ResponseStatus.ERROR, diagRes.status());
        assertTrue(diagRes.message().toLowerCase().contains("unread department notes"));

        ApiResponse adminDiagRes = visitDepartmentService.addDiagnosisToVisitDepartment(diagInput, adminAuth);
        assertEquals(ResponseStatus.SUCCESS, adminDiagRes.status());

        // 3. Add Medication
        AddMedicationInput medInput = new AddMedicationInput(vd.getId().toString(), "Amlodipine", "5mg daily");
        ApiResponse medRes = visitDepartmentService.addMedicationToVisitDepartment(medInput, clinicianAuth);
        assertEquals(ResponseStatus.ERROR, medRes.status());
        assertTrue(medRes.message().toLowerCase().contains("unread department notes"));

        ApiResponse adminMedRes = visitDepartmentService.addMedicationToVisitDepartment(medInput, adminAuth);
        assertEquals(ResponseStatus.SUCCESS, adminMedRes.status());

        // 4. Add Product
        Product newProduct = new Product();
        newProduct.setName("ECG Test " + UUID.randomUUID());
        newProduct.setCode("ECG-" + UUID.randomUUID().toString().substring(0, 8));
        newProduct.setType(ProductType.MEDICAL_ACT);
        newProduct.setUnit(ProductUnit.PIECE);
        newProduct.setClinicPrice(BigDecimal.valueOf(20.0));
        newProduct.setMetadata("{}");
        newProduct = productRepository.save(newProduct);

        CreateVisitDepartmentProductInput prodInput = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                newProduct.getId(),
                null,
                BigDecimal.ONE,
                VisitProductStatus.PENDING
        );
        ApiResponse prodRes = visitDepartmentService.addVisitDepartmentProduct(prodInput, clinicianAuth);
        assertEquals(ResponseStatus.ERROR, prodRes.status());
        assertTrue(prodRes.message().toLowerCase().contains("unread department notes"));

        ApiResponse adminProdRes = visitDepartmentService.addVisitDepartmentProduct(prodInput, adminAuth);
        assertEquals(ResponseStatus.SUCCESS, adminProdRes.status());
    }
}
