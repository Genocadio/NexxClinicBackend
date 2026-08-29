package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitPriceEstimateRepository;
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
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for the uk_visit_price_estimate_product duplicate-key error.
 *
 * <p>Root cause: {@code deleteByVisitId} was a Spring Data derived delete that did
 * <em>not</em> flush pending inserts before running its individual DELETE statements.
 * When a second {@code addVisitDepartmentProduct} call triggered
 * {@code recomputeEstimates} inside the same transaction, Hibernate still had the
 * first estimate in its write-behind queue. The bulk DELETE ran, but Hibernate then
 * re-inserted the old estimate (from the L1 cache) alongside the new one, hitting
 * the {@code UNIQUE (visit_department_product_id)} constraint.
 *
 * <p>Fix: use {@code @Modifying(flushAutomatically = true, clearAutomatically = true)}
 * with an explicit JPQL bulk DELETE so the session is flushed before the DELETE and
 * cleared after it.
 */
@SpringBootTest
class AddVisitDepartmentProductEstimateRegressionTest {

    @Autowired private VisitDepartmentService visitDepartmentService;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitPriceEstimateRepository visitPriceEstimateRepository;

    // ─── helpers ──────────────────────────────────────────────────────────────

    private Worker savedWorker() {
        Worker w = new Worker();
        w.setFirstName("Reg");
        w.setLastName("Test");
        w.setEmail("reg-est-" + UUID.randomUUID() + "@test.com");
        w.setAccountStatus(AccountStatus.ACTIVE);
        w.setActive(true);
        w.setAutoReset(false);
        w.setMustChangeOnNextLogin(false);
        w.setRoles(Set.of(RoleName.CLINICIAN, RoleName.FINANCE));
        return workerRepository.save(w);
    }

    private AuthenticatedUser auth(Worker w) {
        return new AuthenticatedUser(
            w.getId(), w.getEmail(),
            w.getRoles(), "tok", Instant.now().plusSeconds(3600)
        );
    }

    private Product savedProduct(String name, BigDecimal price) {
        Product p = new Product();
        p.setName(name);
        p.setCode("REG-" + UUID.randomUUID());
        p.setType(ProductType.MEDICAL_ACT);
        p.setUnit(ProductUnit.PCS);
        p.setMetadata("{}");
        p.setClinicPrice(price);
        return productRepository.save(p);
    }

    private record Fixture(Worker actor, Visit visit, VisitDepartment dept) {}

    private Fixture savedFixture() {
        Worker actor = savedWorker();

        Patient patient = new Patient();
        patient.setFirstName("Reg");
        patient.setLastName("Patient");
        patient.setFullName("Reg Patient");
        patient.setDateOfBirth(LocalDate.of(1985, 6, 15));
        patient.setGender(Gender.FEMALE);
        patient = patientRepository.save(patient);

        Department department = new Department();
        department.setName("Reg-Dept-" + UUID.randomUUID());
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

        return new Fixture(actor, visit, vd);
    }

    // ─── regression test ──────────────────────────────────────────────────────

    /**
     * Adding two products sequentially to the same visit must not throw
     * uk_visit_price_estimate_product. Each add triggers recomputeEstimates,
     * which deletes all existing estimates and re-inserts one per active product.
     * The second add must delete the first product's estimate before creating two
     * new estimates (one per product), without hitting the unique constraint.
     */
    @Test
    @Transactional
    void addingMultipleProductsToSameVisit_doesNotThrowDuplicateKeyOnPriceEstimate() {
        Fixture fx = savedFixture();
        AuthenticatedUser auth = auth(fx.actor());

        Product product1 = savedProduct("Reg Product 1", new BigDecimal("150.00"));
        Product product2 = savedProduct("Reg Product 2", new BigDecimal("75.00"));

        // Add first product — recomputeEstimates runs, creates 1 estimate row
        ApiResponse<?> r1 = visitDepartmentService.addVisitDepartmentProduct(
            new CreateVisitDepartmentProductInput(
                fx.visit().getId(),
                fx.dept().getDepartment().getId(),
                product1.getId(),
                null,        // processorId
                BigDecimal.ONE,
                null         // status — defaults to PENDING
            ),
            auth
        );
        assertEquals(ResponseStatus.SUCCESS, r1.status(),
            "First addVisitDepartmentProduct failed: " + r1.message());

        // Add second product — recomputeEstimates must delete the first estimate
        // and re-insert two estimates (one per product) without hitting the
        // uk_visit_price_estimate_product constraint.
        ApiResponse<?> r2 = visitDepartmentService.addVisitDepartmentProduct(
            new CreateVisitDepartmentProductInput(
                fx.visit().getId(),
                fx.dept().getDepartment().getId(),
                product2.getId(),
                null,        // processorId
                BigDecimal.ONE,
                null         // status
            ),
            auth
        );
        assertEquals(ResponseStatus.SUCCESS, r2.status(),
            "Second addVisitDepartmentProduct failed (possible duplicate key): " + r2.message());

        // Verify exactly two estimates exist for the visit
        List<?> estimates = visitPriceEstimateRepository.findByVisitId(fx.visit().getId());
        assertEquals(2, estimates.size(),
            "Expected 2 price estimates after adding 2 products, found " + estimates.size());
    }

    /**
     * Adding three products sequentially exercises one additional recompute cycle
     * to ensure the fix holds across N products, not just the first pair.
     */
    @Test
    @Transactional
    void addingThreeProductsSequentially_createsThreeEstimatesWithoutConstraintViolation() {
        Fixture fx = savedFixture();
        AuthenticatedUser auth = auth(fx.actor());

        for (int i = 1; i <= 3; i++) {
            Product product = savedProduct("Seq Product " + i, new BigDecimal(i * 100));
            ApiResponse<?> r = visitDepartmentService.addVisitDepartmentProduct(
                new CreateVisitDepartmentProductInput(
                    fx.visit().getId(),
                    fx.dept().getDepartment().getId(),
                    product.getId(),
                    null,        // processorId
                    BigDecimal.ONE,
                    null         // status
                ),
                auth
            );
            assertEquals(ResponseStatus.SUCCESS, r.status(),
                "addVisitDepartmentProduct #" + i + " failed: " + r.message());
        }

        List<?> estimates = visitPriceEstimateRepository.findByVisitId(fx.visit().getId());
        assertEquals(3, estimates.size(),
            "Expected 3 price estimates after adding 3 products, found " + estimates.size());
    }
}
