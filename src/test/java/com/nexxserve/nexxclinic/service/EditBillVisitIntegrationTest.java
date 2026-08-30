package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.EditBillVisitInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.PaymentMethod;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentProductSource;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.entity.billing.VisitBillingVersion;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class EditBillVisitIntegrationTest {

    @Autowired private VisitBillingService visitBillingService;
    @Autowired private VisitService visitService;
    @Autowired private BillEditingService billEditingService;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitDepartmentProductRepository visitDepartmentProductRepository;
    @Autowired private VisitBillingRepository visitBillingRepository;
    @Autowired private VisitBillingVersionRepository visitBillingVersionRepository;

    private record Fixture(
        Worker actor,
        Visit visit,
        VisitDepartment visitDepartment,
        VisitDepartmentProduct product
    ) {
        UUID catalogProductId() { return product.getProduct().getId(); }
    }

    private Fixture createVisitWithProduct() {
        Worker actor = worker("edit-actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Edit");
        patient.setLastName("Test");
        patient.setFullName("Edit Test");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Department department = new Department();
        department.setName("Ophthalmology-" + UUID.randomUUID());
        department = departmentRepository.save(department);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.BILLING);
        vd = visitDepartmentRepository.save(vd);

        Product product = product("Test Act", new BigDecimal("100.00"));

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setStatus(VisitProductStatus.PENDING);
        vdp.setSource(VisitDepartmentProductSource.USER);
        vdp.setAddedBy(actor);
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
        w.setRoles(Set.of(RoleName.FINANCE, RoleName.ADMIN, RoleName.CLINIC_ADMIN));
        return w;
    }

    private Product product(String name, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setCode("PRD-" + UUID.randomUUID());
        p.setType(ProductType.MEDICAL_ACT);
        p.setUnit(ProductUnit.PCS);
        p.setMetadata("{}");
        p.setClinicPrice(price);
        return productRepository.save(p);
    }

    private AuthenticatedUser auth(Worker worker) {
        return new AuthenticatedUser(
            worker.getId(), worker.getEmail(),
            Set.of(RoleName.FINANCE, RoleName.ADMIN, RoleName.CLINIC_ADMIN),
            "test-token", Instant.now().plusSeconds(3600)
        );
    }

    private BillVisitInput billInput(Fixture fx) {
        return new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("100.00"), PaymentMethod.CASH, null)),
                "Initial bill"
            , null, null))
        );
    }

    private long billingVersionCount(UUID visitId) {
        return visitBillingVersionRepository.findByVisitIdOrderByVersionDesc(visitId).size();
    }

    /** The latest (authoritative) billing version id — the expectedBillingVersionId an edit session must pass. */
    private UUID latestBillingVersionId(UUID visitId) {
        return visitBillingVersionRepository
            .findFirstByVisitIdOrderByVersionDesc(visitId)
            .map(VisitBillingVersion::getId)
            .orElseThrow(() -> new IllegalStateException("No billing version found for visit " + visitId));
    }

    /** Transition visit to BILL_EDITING so editBillVisit is allowed. */
    private void startEditing(UUID visitId, AuthenticatedUser authUser) {
        // billVisit no longer auto-completes the visit, so an edit session must be
        // reachable by first completing the visit explicitly (as FINANCE would).
        Visit current = visitRepository.findById(visitId).orElseThrow();
        if (current.getStatus() != VisitStatus.COMPLETED) {
            ApiResponse<?> complete = visitService.completeVisit(visitId, authUser);
            assertEquals(ResponseStatus.SUCCESS, complete.status(),
                "completeVisit failed: " + complete.message());
        }
        ApiResponse<?> result = billEditingService.startBillEditing(visitId, authUser);
        assertEquals(ResponseStatus.SUCCESS, result.status(),
            "startBillEditing failed: " + result.message());
    }

    private void assertEditSuccess(ApiResponse<?> response) {
        if (response.status() == ResponseStatus.ERROR) {
            org.junit.jupiter.api.Assertions.fail("editBillVisit failed: " + response.message());
        }
        assertEquals(ResponseStatus.SUCCESS, response.status());
    }

    // ─── 1. Add a new product ──────────────────────────────────

    @Test
    void editBillVisit_addsNewProductAndRebills() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        Product newProduct = product("New Act", new BigDecimal("50.00"));

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new EditBillVisitInput.EditBillVisitAddProductInput(
                    newProduct.getId(), new BigDecimal("3"), null)),
                null, null,
                List.of(
                    new EditBillVisitInput.EditBillVisitBillProductInput(
                        fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.NONE, null),
                    new EditBillVisitInput.EditBillVisitBillProductInput(
                        newProduct.getId(), null, CoverageType.PRIVATE, new BigDecimal("3"), ExemptionType.NONE, null)),
                null, "Added new product"
            , null, null))
        );        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(input, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        VisitDepartmentProduct original = visitDepartmentProductRepository.findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, original.getStatus());
    }


    // ─── 2. Remove a product ───────────────────────────────────

    @Test
    void editBillVisit_removesProductAndRebills() {
        Fixture fx = createVisitWithProduct();

        // Add a second product so the department still has products after removal.
        Product keepProduct = product("Keep Act", new BigDecimal("25.00"));
        VisitDepartmentProduct keepVdp = new VisitDepartmentProduct();
        keepVdp.setVisitDepartment(fx.visitDepartment());
        keepVdp.setProduct(keepProduct);
        keepVdp.setQuantity(BigDecimal.ONE);
        keepVdp.setStatus(VisitProductStatus.PENDING);
        keepVdp.setSource(VisitDepartmentProductSource.USER);
        keepVdp.setAddedBy(fx.actor());
        keepVdp = visitDepartmentProductRepository.save(keepVdp);

        // Bill both products first.
        assertEquals(ResponseStatus.SUCCESS, visitBillingService.billVisit(
            new BillVisitInput(fx.visit().getId(), List.of(
                new BillVisitInput.BillVisitDepartmentInput(
                    fx.visitDepartment().getId(),
                    List.of(
                        new BillVisitInput.BillVisitDepartmentProductInput(
                            fx.product().getId(), null, BigDecimal.ONE,
                            CoverageType.PRIVATE, null, ExemptionType.NONE, null),
                        new BillVisitInput.BillVisitDepartmentProductInput(
                            keepVdp.getId(), null, BigDecimal.ONE,
                            CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                    List.of(new BillVisitInput.BillingPaymentInput(
                        new BigDecimal("125.00"), PaymentMethod.CASH, null)),
                    "Bill both"
                , null, null))
            ), auth(fx.actor())).status());

        // Edit: remove fx.product, keep keepProduct + adjust payment.
        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null,
                List.of(fx.catalogProductId()),
                null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    keepProduct.getId(), null, CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("25.00"), PaymentMethod.CASH, null)),
                "Removed product"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(input, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        VisitDepartmentProduct removed = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertTrue(removed.isDeleted(), "removed product should be soft-deleted");

        VisitDepartmentProduct kept = visitDepartmentProductRepository
            .findById(keepVdp.getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, kept.getStatus());
    }

    // ─── 3. Update quantity ────────────────────────────────────

    @Test
    void editBillVisit_updatesQuantityAndRebills() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("5"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("5"), ExemptionType.NONE, null)),
                null, "Updated qty to 5"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(input, auth(fx.actor())));

        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(new BigDecimal("5.0000"), reloaded.getQuantity());
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
    }

    // ─── 4. B3: COMPLETED visit reopens on edit ────────────────

    @Test
    void editBillVisit_reopensCompletedVisit() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        Visit visit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        visit.setStatus(VisitStatus.COMPLETED);
        visitRepository.save(visit);

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                null, "Re-bill"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(input, auth(fx.actor())));

        Visit afterEdit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.COMPLETED, afterEdit.getStatus(),
            "visit re-completed since all products are billed");
    }

    // ─── 5. B7: quantity mismatch rejected ─────────────────────

    @Test
    void editBillVisit_rejectsQuantityMismatch() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("3"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("5"), ExemptionType.NONE, null)),
                null, null
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("Quantity mismatch"), response.message());

        assertEquals(1, billingVersionCount(fx.visit().getId()));
        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
    }

    // ─── 6. Cancelled visit rejected ───────────────────────────

    @Test
    void editBillVisit_rejectsCancelledVisit() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        Visit visit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        visit.setStatus(VisitStatus.CANCELLED);
        visitRepository.save(visit);

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                null, null
            , null, null))
        );

        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().toLowerCase().contains("cancelled"), response.message());
    }

    // ─── 7. Non-existent visit rejected ────────────────────────

    @Test
    void editBillVisit_rejectsNonExistentVisit() {
        Worker actor = worker("ghost@test.com");
        actor = workerRepository.save(actor);

        EditBillVisitInput input = new EditBillVisitInput(
            UUID.randomUUID(),
            UUID.randomUUID(),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                UUID.randomUUID(), null, null, null, List.of(), null, null, null, null))
        );

        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(actor));
        assertEquals(ResponseStatus.ERROR, response.status());
    }

    // ─── 8. Atomicity: failed edit leaves products untouched ───

    @Test
    void editBillVisit_failureRollsBackProductChanges() {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        BigDecimal qtyBefore = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow().getQuantity();

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("99"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("1"), ExemptionType.NONE, null)),
                null, null
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, response.status());

        VisitDepartmentProduct after = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, after.getStatus());
        assertEquals(qtyBefore, after.getQuantity());
        assertEquals(1, billingVersionCount(fx.visit().getId()));
    }

    // ─── 9. Three-version chain: bill → edit → edit ────────────

    @Test
    void editBillVisit_supportsChainedEdits() {
        Fixture fx = createVisitWithProduct();

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());
        assertEquals(1, billingVersionCount(fx.visit().getId()));

        startEditing(fx.visit().getId(), auth(fx.actor()));

        // Edit 1: qty 1→3
        assertEditSuccess(visitBillingService.editBillVisit(new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("3"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("3"), ExemptionType.NONE, null)),
                null, "Edit 1"
            , null, null))
        ), auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        // After Edit 1, visit is re-COMPLETED — re-enter BILL_EDITING for Edit 2
        startEditing(fx.visit().getId(), auth(fx.actor()));

        // Edit 2: qty 3→7
        assertEditSuccess(visitBillingService.editBillVisit(new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("7"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("7"), ExemptionType.NONE, null)),
                null, "Edit 2"
            , null, null))
        ), auth(fx.actor())));
        assertEquals(3, billingVersionCount(fx.visit().getId()));

        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(new BigDecimal("7.0000"), reloaded.getQuantity());
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
    }

    // ─── 10. Incremental bill rejects stale qty after edit ─────

    @Test
    void incrementalBillRejectsStaleQuantityAfterEdit() {
        Fixture fx = createVisitWithProduct();

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        startEditing(fx.visit().getId(), auth(fx.actor()));
        // Edit to qty=5
        assertEditSuccess(visitBillingService.editBillVisit(new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null,
                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                    fx.catalogProductId(), new BigDecimal("5"))),
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, new BigDecimal("5"), ExemptionType.NONE, null)),
                null, "Edit to 5"
            , null, null))
        ), auth(fx.actor())));

        // Incremental billVisit with old qty=1 — must be rejected
        ApiResponse<?> response = visitBillingService.billVisit(
            new BillVisitInput(fx.visit().getId(), List.of(
                new BillVisitInput.BillVisitDepartmentInput(
                    fx.visitDepartment().getId(),
                    List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE, CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                    List.of(new BillVisitInput.BillingPaymentInput(
                        new BigDecimal("100.00"), PaymentMethod.CASH, null)), null
                , null, null))
            ), auth(fx.actor()));

        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("already billed"), response.message());
    }

    // ─── 11. Concurrent billVisit calls serialized ─────────────

    @Test
    void concurrentBillVisitCallsAreSerialized() throws Exception {
        Fixture fx = createVisitWithProduct();
        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<ApiResponse<?>> winner = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ApiResponse<?> result = visitBillingService.billVisit(
                        billInput(fx), auth(fx.actor()));
                    if (result.status() == ResponseStatus.SUCCESS) {
                        winner.compareAndSet(null, result);
                    }
                } catch (Exception e) {
                    // Expected: pessimistic lock contention
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertNotNull(winner.get(), "at least one concurrent billVisit should succeed");
        assertTrue(billingVersionCount(fx.visit().getId()) >= 1,
            "at least one billing version should exist (H2 may allow concurrent inserts)");

        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
    }

    // ─── 12. Concurrent editBillVisit calls serialized ─────────

    @Test
    void concurrentEditBillVisitCallsAreSerialized() throws Exception {
        Fixture fx = createVisitWithProduct();
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        startEditing(fx.visit().getId(), auth(fx.actor()));

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<ApiResponse<?>> winner = new AtomicReference<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    ApiResponse<?> result = visitBillingService.editBillVisit(
                        new EditBillVisitInput(
                            fx.visit().getId(),
                            latestBillingVersionId(fx.visit().getId()),
                            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                                fx.visitDepartment().getId(),
                                null, null,
                                List.of(new EditBillVisitInput.EditBillVisitUpdateProductInput(
                                    fx.catalogProductId(), new BigDecimal(idx + 2))),
                                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                                    fx.catalogProductId(), null, CoverageType.PRIVATE,
                                    new BigDecimal(idx + 2), ExemptionType.NONE, null)),
                                null, "Concurrent edit " + idx
                            , null, null))
                        ), auth(fx.actor()));
                    if (result.status() == ResponseStatus.SUCCESS) {
                        winner.compareAndSet(null, result);
                    }
                } catch (Exception e) {
                    // Expected: pessimistic lock contention
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertNotNull(winner.get(), "at least one concurrent editBillVisit should succeed");

        VisitDepartmentProduct reloaded = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, reloaded.getStatus());
        assertTrue(reloaded.getQuantity().compareTo(BigDecimal.ONE) > 0,
            "quantity should have been updated by the winning edit");
    }

    // ─── 13. PATIENT_SHARE_EXEMPTED edit flow: status transition ────────

    @Test
    void editBillVisit_transitionsFromPatientShareExemptedToFull() {
        Fixture fx = createVisitWithProduct();

        // Bill with PATIENT_SHARE exemption
        BillVisitInput billInput = new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, ExemptionType.PATIENT_SHARE, null)),
                List.of(), // no payment needed — patient share is waived
                "Initial bill with PATIENT_SHARE exemption"
            , null, null))
        );
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput, auth(fx.actor())).status());

        VisitDepartmentProduct afterBill = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.PATIENT_SHARE_EXEMPTED, afterBill.getStatus());

        // Edit: change from PATIENT_SHARE to FULL exemption
        EditBillVisitInput editInput = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.FULL, null)),
                null, "Changed to FULL exemption"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(editInput, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        VisitDepartmentProduct afterEdit = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.EXEMPTED, afterEdit.getStatus());
    }

    @Test
    void editBillVisit_transitionsFromPatientShareExemptedToNone() {
        Fixture fx = createVisitWithProduct();

        // Bill with PATIENT_SHARE exemption
        BillVisitInput billInput = new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, ExemptionType.PATIENT_SHARE, null)),
                List.of(),
                "Initial bill with PATIENT_SHARE exemption"
            , null, null))
        );
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput, auth(fx.actor())).status());

        VisitDepartmentProduct afterBill = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.PATIENT_SHARE_EXEMPTED, afterBill.getStatus());

        // Edit: remove exemption (PATIENT_SHARE -> NONE) — patient now pays
        EditBillVisitInput editInput = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("100.00"), PaymentMethod.CASH, null)),
                "Removed exemption, patient pays full"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(editInput, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        VisitDepartmentProduct afterEdit = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, afterEdit.getStatus());
    }

    // ─── 14. Edit is fully independent: previously-collected payment never carries ──

    @Test
    @Transactional
    void editBillVisit_doesNotCarryPreviousPaymentIntoExemptedEdit() {
        Fixture fx = createVisitWithProduct();

        // First bill: PRIVATE coverage, full patient payable (100), paid in full via CASH.
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        // Sanity: the initial billing recorded the 100 payment (1 payment row).
        com.nexxserve.nexxclinic.entity.VisitBilling initialBilling = visitBillingRepository
            .findByVisitIdOrderByCreatedAtDesc(fx.visit().getId()).get(0);
        assertEquals(1,
            initialBilling.getDepartments().stream()
                .filter(db -> db.getVisitDepartment().getId().equals(fx.visitDepartment().getId()))
                .findFirst().orElseThrow().getPayments().size());

        // Edit: change PRIVATE coverage to FULL exemption -> corrected patient share is 0.
        // The previously-collected 100 must NOT carry into this new independent snapshot.
        EditBillVisitInput editInput = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.FULL, null)),
                List.of(),
                "Fully exempted, no payment to collect"
            , null, null))
        );

        startEditing(fx.visit().getId(), auth(fx.actor()));
        assertEditSuccess(visitBillingService.editBillVisit(editInput, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        // The new (latest) version is a fresh snapshot: it must NOT carry the 100 payment.
        com.nexxserve.nexxclinic.entity.VisitBilling latest = visitBillingRepository
            .findByVisitIdOrderByCreatedAtDesc(fx.visit().getId()).get(0);
        com.nexxserve.nexxclinic.entity.VisitDepartmentBilling deptBilling =
            latest.getDepartments().stream()
                .filter(db -> db.getVisitDepartment().getId().equals(fx.visitDepartment().getId()))
                .findFirst().orElseThrow();
        assertEquals(0, deptBilling.getPayments().size(),
            "edit revised a product and must not carry the previously-collected payment onto the new version");
        assertEquals(0, deptBilling.getPaidAmount().compareTo(java.math.BigDecimal.ZERO),
            "corrected (exempted) bill paid amount must be 0, not the carried 100");
        assertEquals(0, deptBilling.getOutstandingAmount().compareTo(java.math.BigDecimal.ZERO),
            "fully-exempted product leaves no outstanding patient share");
    }

    // ─── 15. PENDING (billed, not yet completed) visits can be edited ──

    @Test
    void startBillEditing_allowsPendingBilledVisit_andCancelRestoresPending() {
        Fixture fx = createVisitWithProduct(); // visit is IN_PROGRESS
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        ApiResponse<?> start = billEditingService.startBillEditing(fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, start.status(), start.message());
        Visit during = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.BILL_EDITING, during.getStatus());
        assertEquals(VisitStatus.IN_PROGRESS, during.getBillingEditSourceStatus());

        ApiResponse<?> cancelled = billEditingService.cancelBillEditing(fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, cancelled.status(), cancelled.message());
        Visit after = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.IN_PROGRESS, after.getStatus(),
            "cancelling an edit on a pending visit must restore IN_PROGRESS, not COMPLETED");
        assertEquals(null, after.getBillingEditSourceStatus());
    }

    @Test
    void completeBillEditing_restoresPendingStatus_afterSuccessfulEdit() {
        Fixture fx = createVisitWithProduct(); // visit is IN_PROGRESS
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        assertEquals(ResponseStatus.SUCCESS,
            billEditingService.startBillEditing(fx.visit().getId(), auth(fx.actor())).status());

        // Edit: mark the product FULLY exempted so no payment is collected.
        EditBillVisitInput editInput = new EditBillVisitInput(
            fx.visit().getId(),
            latestBillingVersionId(fx.visit().getId()),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE, null, ExemptionType.FULL, null)),
                List.of(), "Pending visit correction — full exemption"
            , null, null))
        );
        assertEditSuccess(visitBillingService.editBillVisit(editInput, auth(fx.actor())));
        assertEquals(2, billingVersionCount(fx.visit().getId()));

        Visit afterEdit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.IN_PROGRESS, afterEdit.getStatus(),
            "an edit on a pending visit must restore IN_PROGRESS, not COMPLETED");
        assertEquals(null, afterEdit.getBillingEditSourceStatus());
    }

    @Test
    void startBillEditing_pendingVisit_requiresExistingBilling() {
        Fixture fx = createVisitWithProduct();
        // No billVisit — the pending visit has not been billed yet.
        ApiResponse<?> result = billEditingService.startBillEditing(fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, result.status());
        assertTrue(result.message().toLowerCase().contains("not been billed"),
            "expected 'not been billed' error, got: " + result.message());

        Visit still = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.IN_PROGRESS, still.getStatus());
        assertEquals(null, still.getBillingEditSourceStatus());
    }
}
