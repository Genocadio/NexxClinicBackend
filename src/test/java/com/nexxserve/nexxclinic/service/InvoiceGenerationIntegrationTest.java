package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitBillingItem;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartmentNote;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.NoteType;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.service.billing.InvoiceGenerator;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Integration test for the refactored {@link InvoiceGenerator#generateInvoice}
 * (three-phase split: snapshot in a short read-only tx, PDF render + Supabase upload
 * OUTSIDE any transaction, then a short write tx to persist the object path).
 *
 * <p>{@code SupabaseStorageService} is mocked so no real HTTP call ever happens; the
 * repository is a {@link MockitoSpyBean} so the billing rows are genuinely persisted
 * to H2 exactly as production writes them — only {@code findById} is stubbed (empty)
 * in the persist-failure test to simulate the billing row vanishing mid-flight.
 *
 * <p>Covers:
 * <ul>
 *   <li>success — the invoice is uploaded and the object path is persisted on the
 *       billing row (re-read from the real repository to prove DB persistence);</li>
 *   <li>validation failures (unread notes / stale billing version / not fully billed)
 *       return a clean {@link ApiResponse} error WITHOUT touching Supabase;</li>
 *   <li>persist failure — the uploaded file is cleaned up via
 *       {@code SupabaseStorageService.delete} (orphan cleanup).</li>
 * </ul>
 */
@SpringBootTest
@Transactional
class InvoiceGenerationIntegrationTest {

    @Autowired
    private InvoiceGenerator invoiceGenerator;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

    @Autowired
    private VisitBillingRepository visitBillingRepository;

    @MockitoSpyBean
    private VisitBillingVersionRepository visitBillingVersionRepository;

    @Autowired
    private VisitDepartmentNoteRepository visitDepartmentNoteRepository;

    @MockitoBean
    private SupabaseStorageService supabaseStorageService;

    @MockitoSpyBean
    private DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;

    // ─────────────────────────────────────────────────────────────
    // Fixture
    // ─────────────────────────────────────────────────────────────

    private record Fixture(
            Worker actor,
            DepartmentInsuranceBilling billing,
            VisitDepartment visitDepartment,
            Visit visit
    ) {}

    /**
     * Persists a fully-billed visit graph in H2: patient -> visit -> department ->
     * billed product -> billing container -> department billing -> insurance billing
     * -> item, plus billing version 1. Returns the pieces the assertions need.
     */
    private Fixture persistBilledVisit(VisitProductStatus productStatus) {
        Worker actor = worker("actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Invoice");
        patient.setLastName("Patient");
        patient.setFullName("Invoice Patient");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Department department = new Department();
        department.setName("Cardiology-" + UUID.randomUUID());
        department = departmentRepository.save(department);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.ACTIVE);
        vd = visitDepartmentRepository.save(vd);

        Product product = new Product();
        product.setName("Paracetamol");
        product.setCode("PCM-" + UUID.randomUUID());
        product.setType(ProductType.DRUG);
        product.setUnit(ProductUnit.TABLET);
        product.setMetadata("{}");
        product = productRepository.save(product);

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setStatus(productStatus);
        vdp.setSource(VisitDepartmentProductSource.USER);
        vdp = visitDepartmentProductRepository.save(vdp);

        VisitBillingVersion version = new VisitBillingVersion();
        version.setVisit(visit);
        version.setVersion(1);
        version = visitBillingVersionRepository.save(version);

        VisitBilling container = new VisitBilling();
        container.setVisit(visit);
        container.setBillingVersion(version);

        VisitDepartmentBilling deptBilling = new VisitDepartmentBilling();
        deptBilling.setVisitBilling(container);
        deptBilling.setVisitDepartment(vd);
        container.getDepartments().add(deptBilling);

        DepartmentInsuranceBilling billing = new DepartmentInsuranceBilling();
        billing.setVisitDepartmentBilling(deptBilling);
        billing.setBillingVersion(version);
        deptBilling.getInsuranceBillings().add(billing);

        VisitBillingItem item = new VisitBillingItem();
        item.setDepartmentInsuranceBilling(billing);
        item.setVisitDepartmentProduct(vdp);
        item.setBillingVersion(version);
        item.setUnitPriceSnapshot(new BigDecimal("100.00"));
        item.setQuantitySnapshot(BigDecimal.ONE);
        item.setLineTotal(new BigDecimal("100.00"));
        item.setInsuranceCoveredAmount(BigDecimal.ZERO);
        item.setPatientPayableAmount(new BigDecimal("100.00"));
        billing.getItems().add(item);

        visitBillingRepository.saveAndFlush(container);

        return new Fixture(actor, billing, vd, visit);
    }

    private Worker worker(String email) {
        Worker w = new Worker();
        w.setFirstName("Test");
        w.setLastName("Worker");
        w.setEmail(email);
        w.setAccountStatus(AccountStatus.ACTIVE);
        w.setActive(true);
        w.setAutoReset(false);
        w.setMustChangeOnNextLogin(false);
        w.setRoles(Set.of(RoleName.FINANCE));
        return w;
    }

    private AuthenticatedUser auth(Worker worker) {
        return new AuthenticatedUser(
                worker.getId(),
                worker.getEmail(),
                Set.of(RoleName.FINANCE),
                "test-token",
                Instant.now().plusSeconds(3600)
        );
    }

    /** Stubs the Supabase storage calls used by the render/upload phase. */
    private void stubStorage(String objectPath) {
        when(supabaseStorageService.buildObjectPath(any(), anyString()))
            .thenReturn(objectPath);
        when(supabaseStorageService.invoiceBucket()).thenReturn("data");
    }

    // ─────────────────────────────────────────────────────────────
    // Success path
    // ─────────────────────────────────────────────────────────────

    @Test
    void generateInvoiceUploadsAndPersistsInvoiceUrl() throws Exception {
        Fixture fx = persistBilledVisit(VisitProductStatus.BILLED);
        String objectPath = "invoices/TestClinic/invoice-" + fx.billing().getId() + ".pdf";
        stubStorage(objectPath);
        when(supabaseStorageService.signedUrl(objectPath, 300))
            .thenReturn("https://signed/" + objectPath);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertEquals("Invoice generated successfully.", response.message());
        assertNotNull(response.data());
        Map<?, ?> data = (Map<?, ?>) response.data();
        assertEquals("https://signed/" + objectPath, data.get("signedUrl"));

        // The object path was genuinely persisted: re-read the row from the real
        // repository (spy) and assert the invoiceUrl column holds it.
        DepartmentInsuranceBilling reloaded = departmentInsuranceBillingRepository
            .findById(fx.billing().getId())
            .orElseThrow();
        assertEquals(objectPath, reloaded.getInvoiceUrl());

        // Phase 2 uploaded the file; after persist, the signed URL was generated.
        verify(supabaseStorageService).upload(any(byte[].class), eq(objectPath));
        verify(supabaseStorageService).signedUrl(objectPath, 300);
    }

    // ─────────────────────────────────────────────────────────────
    // Validation failures — clean errors, no Supabase interaction
    // ─────────────────────────────────────────────────────────────

    @Test
    void generateInvoiceRejectsUnreadNotesWithoutTouchingSupabase() {
        Fixture fx = persistBilledVisit(VisitProductStatus.BILLED);

        // Another worker writes a note on the visit department; the acting user has
        // not read it, so the unread-notes gate must reject the request.
        Worker author = worker("note-author-" + UUID.randomUUID() + "@test.com");
        author = workerRepository.save(author);
        VisitDepartmentNote note = new VisitDepartmentNote();
        note.setVisitDepartment(fx.visitDepartment());
        note.setContent("Unread note blocking invoice generation.");
        note.setCreatedBy(author);
        note.setNoteType(NoteType.BILLING);
        visitDepartmentNoteRepository.save(note);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("unread notes"));
        verifyNoInteractions(supabaseStorageService);
    }

    @Test
    void generateInvoiceRejectsStaleBillingVersionWithoutTouchingSupabase() {
        Fixture fx = persistBilledVisit(VisitProductStatus.BILLED);

        // A newer billing version exists -> the billing row under test is no longer
        // the latest, so invoice generation must be rejected.
        VisitBillingVersion newer = new VisitBillingVersion();
        newer.setVisit(fx.visit());
        newer.setVersion(2);
        visitBillingVersionRepository.save(newer);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("latest billing version"));
        verifyNoInteractions(supabaseStorageService);
    }

    @Test
    void generateInvoiceRejectsNotFullyBilledWithoutTouchingSupabase() {
        Fixture fx = persistBilledVisit(VisitProductStatus.PENDING);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("all visit products are billed"));
        verifyNoInteractions(supabaseStorageService);
    }

    // ─────────────────────────────────────────────────────────────
    // Persist failure -> orphaned-upload cleanup
    // ─────────────────────────────────────────────────────────────

    @Test
    void generateInvoicePersistFailureCleansUpOrphanedUpload() throws Exception {
        Fixture fx = persistBilledVisit(VisitProductStatus.BILLED);
        // Phase 3: the billing row has vanished (concurrent edit/delete) -> the spy's
        // findById returns empty, persist reports false, and the uploaded file must be
        // deleted from Supabase. Only this lookup is stubbed — the rest of the flow
        // runs against the real repository.
        when(departmentInsuranceBillingRepository.findById(fx.billing().getId()))
            .thenReturn(Optional.empty());
        String objectPath = "invoices/TestClinic/invoice-" + fx.billing().getId() + ".pdf";
        stubStorage(objectPath);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("no longer available"));
        // The file was uploaded, then orphaned and deleted.
        verify(supabaseStorageService).upload(any(byte[].class), eq(objectPath));
        verify(supabaseStorageService).delete("data", objectPath);
    }

    // ─────────────────────────────────────────────────────────────
    // TOCTOU race: a newer version minted between snapshot and persist
    // ─────────────────────────────────────────────────────────────

    @Test
    void generateInvoiceDoesNotPersistUrlWhenNewerVersionAppearsDuringUpload() throws Exception {
        Fixture fx = persistBilledVisit(VisitProductStatus.BILLED);

        // A concurrent editBillVisit mints version 2 while the PDF renders/upload
        // (Phase 2). Simulate the race via sequential stubbing of the latest-version
        // lookup: it returns version 1 during the Phase-1 snapshot (validation passes),
        // then version 2 during the Phase-3 persist re-check (the row under test is
        // stale and must be rejected).
        VisitBillingVersion newer = new VisitBillingVersion();
        newer.setVisit(fx.visit());
        newer.setVersion(2);
        newer = visitBillingVersionRepository.save(newer);
        when(visitBillingVersionRepository.findFirstByVisitIdOrderByVersionDesc(fx.visit().getId()))
            .thenReturn(
                Optional.of(fx.billing().getBillingVersion()), // Phase 1: version 1 is latest
                Optional.of(newer)                             // Phase 3: version 2 is latest now
            );

        String objectPath = "invoices/TestClinic/invoice-" + fx.billing().getId() + ".pdf";
        stubStorage(objectPath);

        ApiResponse<?> response = invoiceGenerator.generateInvoice(
                fx.billing().getId(),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("no longer available"));
        // The stale row must NOT receive a fresh invoice URL.
        assertNull(departmentInsuranceBillingRepository
                .findById(fx.billing().getId()).orElseThrow().getInvoiceUrl(),
                "stale billing must not receive a fresh invoice URL");
        // The file was uploaded, then orphaned and deleted.
        verify(supabaseStorageService).upload(any(byte[].class), eq(objectPath));
        verify(supabaseStorageService).delete("data", objectPath);
    }
}
