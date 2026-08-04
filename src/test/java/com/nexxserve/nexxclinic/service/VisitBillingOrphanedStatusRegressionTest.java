package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.PaymentMethod;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductSnapshotRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the orphaned-{@code BILLED/EXEMPTED} state and the
 * {@code billVisit} all-or-nothing guarantee.
 *
 * <p>Background: a failed {@code billVisit} used to commit the in-memory product
 * status stamping (BILLED/EXEMPTED + billedBy) while NO billing container was ever
 * created — Spring only rolls back on exceptions, not on returned error
 * {@link ApiResponse}s. That left products permanently {@code BILLED} with
 * {@code visitBilling} answering "Visit billing not found." — unrecoverable by both
 * {@code billVisit} and {@code editBillVisit}. Two fixes are covered here:
 * <ol>
 *   <li><b>Rollback guard</b> — {@code billVisit} marks the transaction rollback-only
 *       on any error, so a failed bill never persists partial product mutations.</li>
 *   <li><b>Self-healing</b> — on first-time {@code billVisit}, products that carry
 *       BILLED/EXEMPTED status with no billing container are reset to PENDING and
 *       billed normally.</li>
 * </ol>
 *
 * <p>Deliberately NOT {@code @Transactional} on the class: each {@code billVisit}
 * call runs in its own real transaction, so the rollback guard actually rolls back
 * and the assertions read the committed state (inside a test transaction the
 * rollback-only flag would be invisible to later reads).
 */
@SpringBootTest
class VisitBillingOrphanedStatusRegressionTest {

    @Autowired
    private VisitBillingService visitBillingService;

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

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository visitDepartmentBillingRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository visitBillingVersionRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitBillingPaymentRepository visitBillingPaymentRepository;

    @Autowired
    private VisitBillingItemRepository visitBillingItemRepository;

    @Autowired
    private VisitDepartmentProductSnapshotRepository visitDepartmentProductSnapshotRepository;

    // ─────────────────────────────────────────────────────────────
    // Fixture
    // ─────────────────────────────────────────────────────────────

    private record Fixture(
            Worker actor,
            Visit visit,
            VisitDepartment visitDepartment,
            VisitDepartmentProduct product
    ) {}

    /**
     * Persists patient -> visit -> department -> visit department -> one product
     * priced at 100.00 with the given status. Returns the pieces the assertions need.
     */
    private Fixture persistVisit(VisitProductStatus productStatus) {
        Worker actor = worker("bill-actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Billing");
        patient.setLastName("Regression");
        patient.setFullName("Billing Regression");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Department department = new Department();
        department.setName("Ophthalmology-" + UUID.randomUUID());
        department = departmentRepository.save(department);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(com.nexxserve.nexxclinic.model.VisitStatus.IN_PROGRESS);
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.BILLING);
        vd = visitDepartmentRepository.save(vd);

        Product product = new Product();
        product.setName("Test Act");
        product.setCode("TST-" + UUID.randomUUID());
        product.setType(ProductType.MEDICAL_ACT);
        product.setUnit(ProductUnit.PCS);
        product.setMetadata("{}");
        product.setClinicPrice(new BigDecimal("100.00"));
        product = productRepository.save(product);

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setPrice(new BigDecimal("100.00"));
        vdp.setStatus(productStatus);
        vdp.setSource(VisitDepartmentProductSource.USER);
        vdp.setAddedBy(actor);
        if (productStatus == VisitProductStatus.BILLED || productStatus == VisitProductStatus.EXEMPTED) {
            // Re-create the orphaned state: stamped as billed but NO billing container.
            vdp.setBilledBy(actor);
        }
        vdp = visitDepartmentProductRepository.save(vdp);
        visitDepartmentProductRepository.flush();

        return new Fixture(actor, visit, vd, vdp);
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
        w.setRoles(Set.of(RoleName.FINANCE, RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.CLINICIAN));
        return w;
    }

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitDepartmentNoteRepository visitDepartmentNoteRepository;

    private AuthenticatedUser auth(Worker worker) {
        // Business rule: cannot bill or edit if there are unread notes.
        // We need to make sure the actor has read all notes.
        // Or simply ensure no notes exist.
        return new AuthenticatedUser(
                worker.getId(),
                worker.getEmail(),
                Set.of(RoleName.FINANCE, RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.CLINICIAN),
                "test-token",
                Instant.now().plusSeconds(3600)
        );
    }

    private BillVisitInput billInput(Fixture fx, BigDecimal payment) {
        BillVisitInput.BillVisitDepartmentProductInput productInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(),
                        null,
                        BigDecimal.ONE,
                        new BigDecimal("100.00"),
                        null,
                        false
                );
        BillVisitInput.BillVisitDepartmentInput departmentInput =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(),
                        List.of(productInput),
                        payment == null
                                ? null
                                : List.of(new BillVisitInput.BillingPaymentInput(
                                        payment, PaymentMethod.CASH, null)),
                        "Dept Note"
                );
        return new BillVisitInput(fx.visit().getId(), List.of(departmentInput));
    }

    private long billingContainerCount(Fixture fx) {
        return visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(fx.visit().getId()).size();
    }

    @Autowired
    private com.nexxserve.nexxclinic.repository.PatientInsuranceRepository patientInsuranceRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.InsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository productInsuranceCoverageRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitInsuranceRepository visitInsuranceRepository;

    @Test
    void testCoveragePercentageMath() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        com.nexxserve.nexxclinic.entity.InsuranceProvider provider = new com.nexxserve.nexxclinic.entity.InsuranceProvider();
        provider.setInsuranceName("Test Insurance");
        provider.setSupportedByClinic(true);
        provider.setDefaultCoveragePercentage(15); // Patient pays 15%
        provider = insuranceProviderRepository.save(provider);

        com.nexxserve.nexxclinic.entity.PatientInsurance insurance = new com.nexxserve.nexxclinic.entity.PatientInsurance();
        insurance.setPatient(fx.visit().getPatient());
        insurance.setInsuranceProvider(provider);
        insurance.setInsuranceCardNumber("CARD123");
        insurance.setPrincipalMember(true);
        insurance.setValidFrom(LocalDate.now().minusDays(1));
        insurance.setValidUntil(LocalDate.now().plusDays(10));
        insurance = patientInsuranceRepository.save(insurance);

        com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage coverage = new com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage();
        coverage.setProduct(fx.product().getProduct());
        coverage.setInsuranceProvider(provider);
        coverage.setCost(new BigDecimal("100.00"));
        coverage.setCovered(true);
        coverage.setMustPrescribedBy(com.nexxserve.nexxclinic.model.MustPrescribedBy.ALL);
        coverage.setDrugAdministrationFrequency(com.nexxserve.nexxclinic.model.DrugAdministrationFrequency.CUSTOM_HOURS);
        coverage = productInsuranceCoverageRepository.save(coverage);

        // A patientInsuranceId is only accepted for a visit that has it LINKED.
        com.nexxserve.nexxclinic.entity.VisitInsurance visitInsurance = new com.nexxserve.nexxclinic.entity.VisitInsurance();
        visitInsurance.setVisit(fx.visit());
        visitInsurance.setPatientInsurance(insurance);
        visitInsuranceRepository.save(visitInsurance);

        BillVisitInput.BillVisitDepartmentProductInput productInput =
            new BillVisitInput.BillVisitDepartmentProductInput(
                fx.product().getId(),
                null,
                BigDecimal.ONE,
                new BigDecimal("100.00"),
                insurance.getId(),
                false
            );
        BillVisitInput.BillVisitDepartmentInput departmentInput =
            new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(productInput),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("15.00"), PaymentMethod.CASH, null)),
                "Note for unread notes check"
            );
        BillVisitInput input = new BillVisitInput(fx.visit().getId(), List.of(departmentInput));

        com.nexxserve.nexxclinic.entity.Worker actor = fx.actor();
        actor.setRoles(Set.of(RoleName.FINANCE, RoleName.ADMIN, RoleName.CLINIC_ADMIN, RoleName.CLINICIAN));
        workerRepository.save(actor);
        
        ApiResponse<?> response = visitBillingService.billVisit(input, auth(fx.actor()));

        if (response.status() == ResponseStatus.ERROR) {
            System.err.println("DEBUG: billVisit failed: " + response.message());
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());

        // Read the department billing by visit department (scalar columns only, avoids
        // triggering lazy collection loading outside a transaction).
        com.nexxserve.nexxclinic.entity.VisitDepartmentBilling vdb = visitDepartmentBillingRepository
            .findByVisitDepartmentId(fx.visitDepartment().getId())
            .get(0);

        // Insurance should cover 85%, patient pays 15%
        assertEquals(new BigDecimal("85.00"), vdb.getInsuranceCoveredAmount());
        assertEquals(new BigDecimal("15.00"), vdb.getPatientPayableAmount());
    }

    @Test
    void testBillVisitDoesNotAutoCompleteWithEmptySecondDepartment() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        // Add a second department that is ACTIVE
        Department dept2 = new Department();
        dept2.setName("Laboratory-" + UUID.randomUUID());
        dept2 = departmentRepository.save(dept2);

        VisitDepartment vd2 = new VisitDepartment();
        vd2.setVisit(fx.visit());
        vd2.setDepartment(dept2);
        vd2.setStatus(VisitDepartmentStatus.ACTIVE);
        vd2 = visitDepartmentRepository.save(vd2);

        // Bill the first department
        ApiResponse<?> response = visitBillingService.billVisit(
            billInput(fx, new BigDecimal("100.00")),
            auth(fx.actor())
        );

        if (response.status() == ResponseStatus.ERROR) {
            System.err.println("DEBUG: billVisit (1) failed: " + response.message());
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());

        // Visit should NOT be COMPLETED because vd2 is ACTIVE
        Visit updatedVisit = visitRepository.findById(fx.visit().getId()).get();
        // Since it's not COMPLETED, it should remain whatever it was (likely ACTIVE or whatever its default is)
        assertTrue(updatedVisit.getStatus() != com.nexxserve.nexxclinic.model.VisitStatus.COMPLETED);

        // Now complete vd2
        vd2.setStatus(VisitDepartmentStatus.COMPLETED);
        visitDepartmentRepository.save(vd2);

        // Edit bill or re-bill (incremental)
        // Since we already billed vd1, we can try to "bill" again if we want to trigger the check.
        // Actually, billVisit now redirects to edit flow.
        response = visitBillingService.billVisit(
            billInput(fx, new BigDecimal("100.00")),
            auth(fx.actor())
        );

        if (response.status() == ResponseStatus.ERROR) {
            System.err.println("DEBUG: billVisit (2) failed: " + response.message());
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());
        updatedVisit = visitRepository.findById(fx.visit().getId()).get();
        assertEquals(com.nexxserve.nexxclinic.model.VisitStatus.COMPLETED, updatedVisit.getStatus());
    }

    // ─────────────────────────────────────────────────────────────
    // Self-healing: orphaned BILLED status with no container
    // ─────────────────────────────────────────────────────────────

    @Test
    void billVisitSelfHealsOrphanedBilledProductsAndCreatesTheBill() {
        Fixture fx = persistVisit(VisitProductStatus.BILLED);
        // Sanity: the fixture IS the orphaned state (no container yet).
        assertEquals(0, billingContainerCount(fx));

        ApiResponse<?> response = visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.SUCCESS, response.status());

        // The bill now exists and is queryable (the user's "Visit billing not found."
        // symptom must be gone).
        assertEquals(1, billingContainerCount(fx));
        assertEquals(
                ResponseStatus.SUCCESS,
                visitBillingService.visitBilling(fx.visit().getId()).status()
        );

        // The product was re-billed: BILLED again, stamped by the actor.
        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
                .findById(fx.product().getId())
                .orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
        assertEquals(fx.actor().getId(), reloaded.getBilledBy().getId());
    }

    @Test
    void billVisitSelfHealsOrphanedExemptedProducts() {
        Fixture fx = persistVisit(VisitProductStatus.EXEMPTED);
        assertEquals(0, billingContainerCount(fx));

        ApiResponse<?> response = visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertEquals(1, billingContainerCount(fx));
        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
                .findById(fx.product().getId())
                .orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
    }

    // ─────────────────────────────────────────────────────────────
    // Rollback guard: a failed bill leaves products untouched
    // ─────────────────────────────────────────────────────────────

    @Test
    void billVisitOverpaymentErrorLeavesPendingProductsUntouched() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        ApiResponse<?> response = visitBillingService.billVisit(
                // Payment (150.00) exceeds patient payable (100.00, no insurance).
                billInput(fx, new BigDecimal("150.00")),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("exceed the patient payable"));

        // The rollback guard: the in-memory BILLED stamping must NOT have committed.
        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
                .findById(fx.product().getId())
                .orElseThrow();
        assertEquals(VisitProductStatus.PENDING, reloaded.getStatus());
        assertNull(reloaded.getBilledBy());

        // And no billing container was created.
        assertEquals(0, billingContainerCount(fx));
    }

    @Test
    void failedPrepareBillLeavesZeroDbWritesAndDeferredOrphanFlush() {
        // Orphaned product: stamped BILLED by the actor but with NO billing container.
        Fixture fx = persistVisit(VisitProductStatus.BILLED);
        assertEquals(0, billingContainerCount(fx));

        // Bill with an OVERPAYMENT (150 > 100 patient payable). This error is raised at
        // the very END of the validation pass — AFTER prepareBill already reset the
        // orphaned product to PENDING in-memory and built the whole billing container
        // in memory. The rollback-only guard must roll back ALL of it: the deferred
        // orphan flush must NOT commit, and no billing artifact may be written.
        ApiResponse<?> response = visitBillingService.billVisit(
                billInput(fx, new BigDecimal("150.00")),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("exceed the patient payable"));

        // Deferred orphan flush contract: the in-memory PENDING reset applied by
        // prepareBill must be rolled back, so the product keeps its original orphaned
        // BILLED status (a commit here would re-create the unrecoverable "BILLED with
        // no container" state that this whole class guards against).
        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
                .findById(fx.product().getId())
                .orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
        assertEquals(fx.actor().getId(), reloaded.getBilledBy().getId());

        // Zero DB writes: no container, no version, no department billing, no billing
        // items (which would imply insurance buckets), no payments, no snapshots, and
        // no billing notes.
        assertEquals(0, billingContainerCount(fx));
        assertTrue(visitBillingVersionRepository
                        .findByVisitIdOrderByVersionDesc(fx.visit().getId()).isEmpty(),
                "no billing version must be minted");
        assertTrue(visitDepartmentBillingRepository
                        .findByVisitDepartmentId(fx.visitDepartment().getId()).isEmpty(),
                "no department billing rows");
        assertTrue(visitBillingItemRepository
                        .findByVisitDepartmentProductId(fx.product().getId()).isEmpty(),
                "no billing items");
        assertTrue(visitBillingPaymentRepository
                        .findByVisitDepartmentIdWithBuckets(fx.visitDepartment().getId()).isEmpty(),
                "no payment rows");
        assertTrue(visitDepartmentProductSnapshotRepository
                        .findByVisitDepartmentProductId(fx.product().getId()).isEmpty(),
                "no product snapshots");
        assertEquals(0,
                visitDepartmentNoteRepository.countByVisitDepartmentId(fx.visitDepartment().getId()),
                "no billing notes");

        // The persistBill auto-complete must not have run either: the department stays
        // BILLING (handed to finance) and the visit stays IN_PROGRESS.
        Visit updatedVisit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(com.nexxserve.nexxclinic.model.VisitStatus.IN_PROGRESS, updatedVisit.getStatus());
        VisitDepartment updatedDept = visitDepartmentRepository
                .findById(fx.visitDepartment().getId()).orElseThrow();
        assertEquals(VisitDepartmentStatus.BILLING, updatedDept.getStatus());
    }

    // ─────────────────────────────────────────────────────────────
    // Happy path sanity
    // ─────────────────────────────────────────────────────────────

    @Test
    void billVisitHappyPathCreatesContainerAndMarksProductBilled() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        ApiResponse<?> response = visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")),
                auth(fx.actor())
        );

        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertEquals(1, billingContainerCount(fx));

        VisitBilling container = visitBillingRepository
                .findByVisitIdOrderByCreatedAtDesc(fx.visit().getId())
                .get(0);
        assertEquals(fx.visit().getId(), container.getVisit().getId());

        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
                .findById(fx.product().getId())
                .orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
        assertEquals(fx.actor().getId(), reloaded.getBilledBy().getId());
    }

    // ─────────────────────────────────────────────────────────────
    // Incremental re-bill: identity guard
    // ─────────────────────────────────────────────────────────────

    @Test
    void testIncrementalRebillRejectsChangedPriceWithCleanError() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        assertEquals(ResponseStatus.SUCCESS, visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")), auth(fx.actor())).status());

        // Same product re-billed with a CHANGED price. An incremental billVisit must
        // NOT accept it (a correction belongs in editBillVisit), must return a clean
        // error, and must NOT create a second billing version.
        BillVisitInput.BillVisitDepartmentProductInput changedPrice =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE, new BigDecimal("150.00"), null, false);
        BillVisitInput.BillVisitDepartmentInput deptInput =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(), List.of(changedPrice),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                new BigDecimal("150.00"), PaymentMethod.CASH, null)),
                        null);
        ApiResponse<?> response = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(deptInput)), auth(fx.actor()));

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("already billed"), response.message());
        assertTrue(response.message().contains("editBillVisit"), response.message());
        assertEquals(1, billingContainerCount(fx));
    }

    @Test
    void testIncrementalRebillOfIdenticalLineSucceedsAndVersionsIncrement() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        assertEquals(ResponseStatus.SUCCESS, visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")), auth(fx.actor())).status());
        assertEquals(1, visitBillingVersionRepository
                .findByVisitIdOrderByVersionDesc(fx.visit().getId()).get(0).getVersion());

        // Identical re-bill (same qty, price, no insurance) → accepted and produces
        // the next billing version.
        ApiResponse<?> response = visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")), auth(fx.actor()));
        if (response.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertEquals(2, visitBillingVersionRepository
                .findByVisitIdOrderByVersionDesc(fx.visit().getId()).get(0).getVersion());
    }

    // ─────────────────────────────────────────────────────────────
    // Bucket attribution: partial payments tied to the insurance bucket
    // ─────────────────────────────────────────────────────────────

    @Test
    void testPartialPaymentIsTiedToInsuranceBucket() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        com.nexxserve.nexxclinic.entity.InsuranceProvider provider = new com.nexxserve.nexxclinic.entity.InsuranceProvider();
        provider.setInsuranceName("Bucket Insurance");
        provider.setSupportedByClinic(true);
        provider.setDefaultCoveragePercentage(15);
        provider = insuranceProviderRepository.save(provider);

        com.nexxserve.nexxclinic.entity.PatientInsurance insurance = new com.nexxserve.nexxclinic.entity.PatientInsurance();
        insurance.setPatient(fx.visit().getPatient());
        insurance.setInsuranceProvider(provider);
        insurance.setInsuranceCardNumber("CARD789");
        insurance.setPrincipalMember(true);
        insurance.setValidFrom(LocalDate.now().minusDays(1));
        insurance.setValidUntil(LocalDate.now().plusDays(10));
        insurance = patientInsuranceRepository.save(insurance);

        com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage coverage = new com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage();
        coverage.setProduct(fx.product().getProduct());
        coverage.setInsuranceProvider(provider);
        coverage.setCost(new BigDecimal("100.00"));
        coverage.setCovered(true);
        coverage.setMustPrescribedBy(com.nexxserve.nexxclinic.model.MustPrescribedBy.ALL);
        coverage.setDrugAdministrationFrequency(com.nexxserve.nexxclinic.model.DrugAdministrationFrequency.CUSTOM_HOURS);
        productInsuranceCoverageRepository.save(coverage);

        com.nexxserve.nexxclinic.entity.VisitInsurance visitInsurance = new com.nexxserve.nexxclinic.entity.VisitInsurance();
        visitInsurance.setVisit(fx.visit());
        visitInsurance.setPatientInsurance(insurance);
        visitInsuranceRepository.save(visitInsurance);

        BillVisitInput.BillVisitDepartmentProductInput productInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE, new BigDecimal("100.00"),
                        insurance.getId(), false);
        BillVisitInput.BillVisitDepartmentInput deptInput =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(), List.of(productInput),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                new BigDecimal("10.00"), PaymentMethod.CASH, "REF-1")),
                        "Note");
        ApiResponse<?> response = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(deptInput)), auth(fx.actor()));
        if (response.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());

        // The 10.00 payment must be attributed to the insurance bucket (the same row
        // that holds the provider's covered amount).
        java.util.List<com.nexxserve.nexxclinic.entity.VisitBillingPayment> payments =
                visitBillingPaymentRepository.findByVisitDepartmentIdWithBuckets(fx.visitDepartment().getId());
        assertEquals(1, payments.size());
        assertNotNull(payments.get(0).getDepartmentInsuranceBilling(), "payment must be tied to the insurance bucket");
        assertEquals(new BigDecimal("10.00"), payments.get(0).getAmount());
        assertEquals(insurance.getId(),
                payments.get(0).getDepartmentInsuranceBilling().getPatientInsurance().getId());
    }

    // ─────────────────────────────────────────────────────────────
    // Payment fidelity: methods/references preserved across buckets
    // and across carried-forward re-bills
    // ─────────────────────────────────────────────────────────────

    @Test
    void testMultiplePaymentsSplitAcrossBucketsPreserveMethodAndReference() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        // Insure the fixture product (patient pays 15% of the 100.00 coverage cost).
        com.nexxserve.nexxclinic.entity.InsuranceProvider provider = new com.nexxserve.nexxclinic.entity.InsuranceProvider();
        provider.setInsuranceName("Split Insurance");
        provider.setSupportedByClinic(true);
        provider.setDefaultCoveragePercentage(15);
        provider = insuranceProviderRepository.save(provider);

        com.nexxserve.nexxclinic.entity.PatientInsurance unsavedInsurance = new com.nexxserve.nexxclinic.entity.PatientInsurance();
        unsavedInsurance.setPatient(fx.visit().getPatient());
        unsavedInsurance.setInsuranceProvider(provider);
        unsavedInsurance.setInsuranceCardNumber("SPLIT123");
        unsavedInsurance.setPrincipalMember(true);
        unsavedInsurance.setValidFrom(LocalDate.now().minusDays(1));
        unsavedInsurance.setValidUntil(LocalDate.now().plusDays(10));
        com.nexxserve.nexxclinic.entity.PatientInsurance insurance = patientInsuranceRepository.save(unsavedInsurance);

        com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage coverage = new com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage();
        coverage.setProduct(fx.product().getProduct());
        coverage.setInsuranceProvider(provider);
        coverage.setCost(new BigDecimal("100.00"));
        coverage.setCovered(true);
        coverage.setMustPrescribedBy(com.nexxserve.nexxclinic.model.MustPrescribedBy.ALL);
        coverage.setDrugAdministrationFrequency(com.nexxserve.nexxclinic.model.DrugAdministrationFrequency.CUSTOM_HOURS);
        productInsuranceCoverageRepository.save(coverage);

        com.nexxserve.nexxclinic.entity.VisitInsurance visitInsurance = new com.nexxserve.nexxclinic.entity.VisitInsurance();
        visitInsurance.setVisit(fx.visit());
        visitInsurance.setPatientInsurance(insurance);
        visitInsuranceRepository.save(visitInsurance);

        // Add a second, uninsured product to the SAME department (payable 100.00).
        Product uninsured = new Product();
        uninsured.setName("Uninsured Act");
        uninsured.setCode("UNS-" + UUID.randomUUID());
        uninsured.setType(ProductType.MEDICAL_ACT);
        uninsured.setUnit(ProductUnit.PCS);
        uninsured.setMetadata("{}");
        uninsured.setClinicPrice(new BigDecimal("100.00"));
        uninsured = productRepository.save(uninsured);

        VisitDepartmentProduct uninsuredVdp = new VisitDepartmentProduct();
        uninsuredVdp.setVisitDepartment(fx.visitDepartment());
        uninsuredVdp.setProduct(uninsured);
        uninsuredVdp.setQuantity(BigDecimal.ONE);
        uninsuredVdp.setPrice(new BigDecimal("100.00"));
        uninsuredVdp.setStatus(VisitProductStatus.PENDING);
        uninsuredVdp.setSource(VisitDepartmentProductSource.USER);
        uninsuredVdp.setAddedBy(fx.actor());
        uninsuredVdp = visitDepartmentProductRepository.save(uninsuredVdp);

        // Total patient payable = 15.00 (insured) + 100.00 (uninsured) = 115.00.
        // Pay 100 CASH + 15 MOBILE_MONEY: the CASH payment must be split across the
        // two buckets, and every bucket row must keep its original method/reference.
        BillVisitInput.BillVisitDepartmentProductInput insuredInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE, new BigDecimal("100.00"),
                        insurance.getId(), false);
        BillVisitInput.BillVisitDepartmentProductInput uninsuredInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        uninsuredVdp.getId(), null, BigDecimal.ONE, new BigDecimal("100.00"),
                        null, false);
        BillVisitInput.BillVisitDepartmentInput deptInput =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(),
                        List.of(insuredInput, uninsuredInput),
                        List.of(
                                new BillVisitInput.BillingPaymentInput(
                                        new BigDecimal("100.00"), PaymentMethod.CASH, "REF-CASH"),
                                new BillVisitInput.BillingPaymentInput(
                                        new BigDecimal("15.00"), PaymentMethod.MOBILE_MONEY, "REF-MOMO")),
                        "Note");
        ApiResponse<?> response = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(deptInput)), auth(fx.actor()));
        if (response.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("Success expected but got error: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());

        java.util.List<com.nexxserve.nexxclinic.entity.VisitBillingPayment> payments =
                visitBillingPaymentRepository.findByVisitDepartmentIdWithBuckets(fx.visitDepartment().getId());
        // 100 CASH is split: 15 -> insured bucket, 85 -> uninsured bucket; then 15 MOMO.
        assertEquals(3, payments.size());
        assertEquals(
                new BigDecimal("115.00"),
                payments.stream()
                        .map(com.nexxserve.nexxclinic.entity.VisitBillingPayment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        long cashRows = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.CASH).count();
        long momoRows = payments.stream()
                .filter(p -> p.getPaymentMethod() == PaymentMethod.MOBILE_MONEY).count();
        assertEquals(2, cashRows, "two CASH rows (15 + 85 split)");
        assertEquals(1, momoRows);
        for (com.nexxserve.nexxclinic.entity.VisitBillingPayment p : payments) {
            assertEquals(
                    p.getPaymentMethod() == PaymentMethod.CASH ? "REF-CASH" : "REF-MOMO",
                    p.getReference());
        }

        // The insured bucket must receive exactly the 15.00 it is owed.
        java.util.List<com.nexxserve.nexxclinic.entity.VisitBillingPayment> insuredRows =
                payments.stream()
                        .filter(p -> p.getDepartmentInsuranceBilling() != null
                                && p.getDepartmentInsuranceBilling().getPatientInsurance() != null
                                && p.getDepartmentInsuranceBilling().getPatientInsurance().getId().equals(insurance.getId()))
                        .toList();
        assertEquals(1, insuredRows.size());
        assertEquals(new BigDecimal("15.00"), insuredRows.get(0).getAmount());
    }

    @Test
    void testCarriedForwardPaymentsPreserveMethodOnIncrementalRebill() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        BillVisitInput.BillVisitDepartmentProductInput productInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE, new BigDecimal("100.00"), null, false);
        BillVisitInput.BillVisitDepartmentInput firstDept =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(), List.of(productInput),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                new BigDecimal("100.00"), PaymentMethod.MOBILE_MONEY, "MOMO-REF")),
                        "Note");
        ApiResponse<?> first = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(firstDept)), auth(fx.actor()));
        if (first.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("First bill failed: " + first.message());
        }
        assertEquals(ResponseStatus.SUCCESS, first.status());

        // Identical re-bill WITHOUT explicit payments: the version-1 MOBILE_MONEY
        // payment must be carried forward with its method/reference intact (never
        // re-created as CASH).
        BillVisitInput.BillVisitDepartmentInput rebillDept =
                new BillVisitInput.BillVisitDepartmentInput(
                        fx.visitDepartment().getId(), List.of(productInput), null, "Note");
        ApiResponse<?> second = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(rebillDept)), auth(fx.actor()));
        if (second.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("Second bill failed: " + second.message());
        }
        assertEquals(ResponseStatus.SUCCESS, second.status());

        java.util.List<com.nexxserve.nexxclinic.entity.VisitBillingPayment> payments =
                visitBillingPaymentRepository.findByVisitDepartmentIdWithBuckets(fx.visitDepartment().getId());
        // One payment row per version (2), both MOBILE_MONEY with the original reference.
        assertEquals(2, payments.size());
        for (com.nexxserve.nexxclinic.entity.VisitBillingPayment p : payments) {
            assertEquals(PaymentMethod.MOBILE_MONEY, p.getPaymentMethod());
            assertEquals("MOMO-REF", p.getReference());
            assertEquals(new BigDecimal("100.00"), p.getAmount());
        }
    }

    @Test
    void testIncrementalBillVisitKeepsPreviouslyBilledDepartmentInLatestVersion() {
        Fixture fx = persistVisit(VisitProductStatus.PENDING);

        // Bill department A first (version 1).
        assertEquals(ResponseStatus.SUCCESS, visitBillingService.billVisit(
                billInput(fx, new BigDecimal("100.00")), auth(fx.actor())).status());

        // Add department B with its own product.
        Department deptB = new Department();
        deptB.setName("Cardiology-" + UUID.randomUUID());
        deptB = departmentRepository.save(deptB);
        VisitDepartment vdB = new VisitDepartment();
        vdB.setVisit(fx.visit());
        vdB.setDepartment(deptB);
        vdB.setStatus(VisitDepartmentStatus.BILLING);
        vdB = visitDepartmentRepository.save(vdB);

        Product productB = new Product();
        productB.setName("Cardio Act");
        productB.setCode("CRD-" + UUID.randomUUID());
        productB.setType(ProductType.MEDICAL_ACT);
        productB.setUnit(ProductUnit.PCS);
        productB.setMetadata("{}");
        productB.setClinicPrice(new BigDecimal("50.00"));
        productB = productRepository.save(productB);

        VisitDepartmentProduct vdpB = new VisitDepartmentProduct();
        vdpB.setVisitDepartment(vdB);
        vdpB.setProduct(productB);
        vdpB.setQuantity(BigDecimal.ONE);
        vdpB.setPrice(new BigDecimal("50.00"));
        vdpB.setStatus(VisitProductStatus.PENDING);
        vdpB.setSource(VisitDepartmentProductSource.USER);
        vdpB.setAddedBy(fx.actor());
        vdpB = visitDepartmentProductRepository.save(vdpB);

        // Incremental billVisit for department B ONLY. Department A must be carried
        // forward into the new version instead of being dropped.
        BillVisitInput.BillVisitDepartmentProductInput productBInput =
                new BillVisitInput.BillVisitDepartmentProductInput(
                        vdpB.getId(), null, BigDecimal.ONE, new BigDecimal("50.00"), null, false);
        BillVisitInput.BillVisitDepartmentInput deptBInput =
                new BillVisitInput.BillVisitDepartmentInput(
                        vdB.getId(), List.of(productBInput),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                new BigDecimal("50.00"), PaymentMethod.CASH, null)),
                        "Note B");
        ApiResponse<?> response = visitBillingService.billVisit(
                new BillVisitInput(fx.visit().getId(), List.of(deptBInput)), auth(fx.actor()));
        if (response.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("Second bill failed: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());

        // The latest version must STILL contain department A (carried forward) AND B.
        ApiResponse<?> latestResponse = visitBillingService.visitBilling(fx.visit().getId());
        assertEquals(ResponseStatus.SUCCESS, latestResponse.status());
        Map<String, Object> billingMap = (Map<String, Object>) latestResponse.data();
        List<?> departments = (List<?>) billingMap.get("departments");
        assertEquals(2, departments.size(), "incremental version must carry forward dept A");

        Set<UUID> deptIds = departments.stream()
                .map(d -> (UUID) ((Map<String, Object>) ((Map<String, Object>) d).get("visitDepartment")).get("id"))
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(deptIds.contains(fx.visitDepartment().getId()), "dept A must be carried forward");
        assertTrue(deptIds.contains(vdB.getId()), "dept B must be present");
    }
}
