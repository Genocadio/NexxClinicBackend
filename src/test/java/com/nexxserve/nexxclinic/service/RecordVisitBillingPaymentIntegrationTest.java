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
import com.nexxserve.nexxclinic.graphql.input.RecordVisitBillingPaymentInput;
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
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RecordVisitBillingPaymentIntegrationTest {

    @Autowired private VisitBillingService visitBillingService;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitDepartmentProductRepository visitDepartmentProductRepository;

    private record Fixture(
        Worker actor,
        Visit visit,
        VisitDepartment visitDepartment,
        VisitDepartmentProduct product
    ) {}

    private Fixture createVisitWithProduct() {
        Worker actor = worker("pay-actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Pay");
        patient.setLastName("Patient");
        patient.setFullName("Pay Patient");
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

        Product product = product("Payment Act", new BigDecimal("100.00"));

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

    private BillVisitInput billInputNoPayment(Fixture fx) {
        return new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, ExemptionType.NONE)),
                List.of(),
                "Bill with no payment"
            ))
        );
    }

    @SuppressWarnings("unchecked")
    private UUID extractInsuranceBillingId(ApiResponse<?> response) {
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        List<Map<String, Object>> departments =
            (List<Map<String, Object>>) billingData.get("departments");
        Map<String, Object> dept = departments.get(0);
        List<Map<String, Object>> insuranceBillings =
            (List<Map<String, Object>>) dept.get("insuranceBillings");
        Map<String, Object> ib = insuranceBillings.get(0);
        return (UUID) ib.get("id");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractDepartmentBilling(ApiResponse<?> response) {
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        List<Map<String, Object>> departments =
            (List<Map<String, Object>>) billingData.get("departments");
        return departments.get(0);
    }

    private ApiResponse<?> billVisitNoPayment(Fixture fx) {
        return visitBillingService.billVisit(billInputNoPayment(fx), auth(fx.actor()));
    }

    // ─── 1. Full payment succeeds ─────────────────────────────

    @Test
    void recordPayment_success() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            billingId, new BigDecimal("100.00"), PaymentMethod.CASH, null, "Full payment");

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, payResponse.status());

        Map<String, Object> deptBilling = extractDepartmentBilling(payResponse);
        assertEquals(new BigDecimal("100.00"), deptBilling.get("paidAmount"));
        assertEquals(new BigDecimal("0.00"), deptBilling.get("outstandingAmount"));
    }

    // ─── 2. Partial payment with note succeeds ────────────────

    @Test
    void recordPayment_partialPayment_withNote() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            billingId, new BigDecimal("40.00"), PaymentMethod.CASH, null,
            "Partial payment note");

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, payResponse.status());

        Map<String, Object> deptBilling = extractDepartmentBilling(payResponse);
        assertEquals(new BigDecimal("40.00"), deptBilling.get("paidAmount"));
        BigDecimal outstanding = new BigDecimal(deptBilling.get("outstandingAmount").toString());
        assertTrue(outstanding.compareTo(BigDecimal.ZERO) > 0,
            "outstanding should be > 0 for partial payment");
    }

    // ─── 3. Partial payment without note is rejected ──────────

    @Test
    void recordPayment_partialPayment_noNote_rejected() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            billingId, new BigDecimal("40.00"), PaymentMethod.CASH, null, null);

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, payResponse.status());
        assertTrue(payResponse.message().contains("billing note is required"),
            payResponse.message());
    }

    // ─── 4. Overpayment is rejected ───────────────────────────

    @Test
    void recordPayment_overpayment_rejected() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            billingId, new BigDecimal("150.00"), PaymentMethod.CASH, null, null);

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, payResponse.status());
        assertTrue(payResponse.message().contains("Payment amount would exceed"),
            payResponse.message());
    }

    // ─── 5. Payment on cancelled visit is rejected ────────────

    @Test
    void recordPayment_cancelledVisit_rejected() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        Visit visit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        visit.setStatus(VisitStatus.CANCELLED);
        visitRepository.save(visit);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            billingId, new BigDecimal("100.00"), PaymentMethod.CASH, null, null);

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, payResponse.status());
        assertTrue(payResponse.message().contains("Cancelled visits cannot accept billing payments"),
            payResponse.message());
    }

    // ─── 6. Non-existent billing is rejected ──────────────────

    @Test
    void recordPayment_nonExistentBilling_rejected() {
        Worker actor = worker("ghost-pay-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
            UUID.randomUUID(), new BigDecimal("100.00"), PaymentMethod.CASH, null, null);

        ApiResponse<?> payResponse = visitBillingService.recordVisitBillingPayment(
            input, auth(actor));
        assertEquals(ResponseStatus.ERROR, payResponse.status());
        assertTrue(payResponse.message().contains("not found"),
            payResponse.message());
    }

    // ─── 7. Concurrent payments serialized by pessimistic lock ───────────

    @Test
    void recordPayment_concurrentPaymentsSerialized() throws Exception {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> billResponse = billVisitNoPayment(fx);
        assertEquals(ResponseStatus.SUCCESS, billResponse.status());

        UUID billingId = extractInsuranceBillingId(billResponse);

        int threadCount = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        java.util.concurrent.atomic.AtomicInteger successCount = new java.util.concurrent.atomic.AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    // Each thread tries to pay 100.00 on a 100.00 payable — only one should succeed.
                    RecordVisitBillingPaymentInput input = new RecordVisitBillingPaymentInput(
                        billingId, new BigDecimal("100.00"), PaymentMethod.CASH, null, "concurrent");
                    ApiResponse<?> result = visitBillingService.recordVisitBillingPayment(
                        input, auth(fx.actor()));
                    if (result.status() == ResponseStatus.SUCCESS) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    // Expected: pessimistic lock contention or rollback
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        // At most one payment should succeed; the others should fail (overpayment or lock contention).
        assertTrue(successCount.get() >= 1, "at least one payment should succeed");
        assertTrue(successCount.get() <= 1, "at most one payment should succeed — no double payment");
    }
}
