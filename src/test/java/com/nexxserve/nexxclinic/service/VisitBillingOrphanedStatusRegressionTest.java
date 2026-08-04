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
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.ACTIVE);
        vd = visitDepartmentRepository.save(vd);

        Product product = new Product();
        product.setName("Test Act");
        product.setCode("TST-" + UUID.randomUUID());
        product.setType(ProductType.MEDICAL_ACT);
        product.setUnit(ProductUnit.PCS);
        product.setMetadata("{}");
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
                        null
                );
        return new BillVisitInput(fx.visit().getId(), List.of(departmentInput));
    }

    private long billingContainerCount(Fixture fx) {
        return visitBillingRepository.findByVisitIdOrderByCreatedAtDesc(fx.visit().getId()).size();
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
}
