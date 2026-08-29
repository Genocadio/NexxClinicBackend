package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.ChangeVisitDateInput;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.ExemptionType;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.PaymentMethod;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.RoleName;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitProductStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class VisitDateBillingDateIntegrationTest {

    @Autowired private VisitBillingService visitBillingService;
    @Autowired private VisitService visitService;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitDepartmentProductRepository visitDepartmentProductRepository;
    @Autowired private VisitBillingRepository visitBillingRepository;
    @Autowired private DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;

    private record Fixture(
        Worker actor,
        Visit visit,
        VisitDepartment visitDepartment,
        VisitDepartmentProduct product
    ) {
        UUID catalogProductId() { return product.getProduct().getId(); }
        UUID vdId() { return visitDepartment.getId(); }
    }

    private Fixture createVisitWithProduct(LocalDateTime visitDate) {
        Worker actor = worker("vd-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Date");
        patient.setLastName("Test");
        patient.setFullName("Date Test");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.MALE);
        patient = patientRepository.save(patient);

        Department department = new Department();
        department.setName("Cardiology-" + UUID.randomUUID());
        department = departmentRepository.save(department);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(visitDate);
        visit = visitRepository.save(visit);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.BILLING);
        vd = visitDepartmentRepository.save(vd);

        Product product = new Product();
        product.setName("Test Act");
        product.setCode("PRD-" + UUID.randomUUID());
        product.setType(ProductType.MEDICAL_ACT);
        product.setUnit(ProductUnit.PCS);
        product.setMetadata("{}");
        product.setClinicPrice(new BigDecimal("100.00"));
        product = productRepository.save(product);

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setStatus(VisitProductStatus.PENDING);
        vdp.setSource(com.nexxserve.nexxclinic.model.VisitDepartmentProductSource.USER);
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
                fx.vdId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("100.00"), PaymentMethod.CASH, null)),
                "Initial bill"
            , null, null))
        );
    }

    private DepartmentInsuranceBilling findBilling(UUID visitId) {
        return departmentInsuranceBillingRepository
            .findAllByVisitId(visitId)
            .iterator().next();
    }

    // ─── Scenario 1: billed visit, edit visit date forward ─────────────

    @Test
    @Transactional
    void billedVisit_editVisitDateForwardShiftsBillingDateBySameDelta() {
        LocalDateTime visitOriginal = LocalDateTime.now().minusDays(2);
        Fixture fx = createVisitWithProduct(visitOriginal);

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        DepartmentInsuranceBilling before = findBilling(fx.visit().getId());
        assertNotNull(before.getBillingDate());

        LocalDateTime newVisitDate = visitOriginal.plusDays(3);
        ApiResponse<com.nexxserve.nexxclinic.dto.out.VisitDto> resp =
            visitService.changeVisitDate(
                new ChangeVisitDateInput(fx.visit().getId(), newVisitDate),
                auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, resp.status(), resp.message());

        DepartmentInsuranceBilling after = findBilling(fx.visit().getId());

        // The gap between visit date and billing date must be preserved.
        Duration beforeGap = Duration.between(visitOriginal, before.getBillingDate());
        Duration afterGap = Duration.between(newVisitDate, after.getBillingDate());
        assertEquals(beforeGap, afterGap,
            "billing date should shift by exactly the same delta as the visit date");
        // Clamp: billing date must be at least 5 minutes after the (new) visit date.
        assertFalse(after.getBillingDate().isBefore(newVisitDate.plusMinutes(5)),
            "billing date must never be before the new visit date + 5 minutes");
    }

    // ─── Scenario 1b: billed visit, edit visit date backward ────────────

    @Test
    @Transactional
    void billedVisit_editVisitDateBackwardShiftsBillingDateBySameDelta() {
        LocalDateTime visitOriginal = LocalDateTime.now().plusDays(2);
        Fixture fx = createVisitWithProduct(visitOriginal);

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        // The billing happened at/around now, but the visit date is 2 days ahead, so the
        // original billing date was clamped to visit + 5 minutes (it was before the visit).
        DepartmentInsuranceBilling before = findBilling(fx.visit().getId());
        assertNotNull(before.getBillingDate());

        LocalDateTime newVisitDate = visitOriginal.minusDays(4);
        ApiResponse<com.nexxserve.nexxclinic.dto.out.VisitDto> resp =
            visitService.changeVisitDate(
                new ChangeVisitDateInput(fx.visit().getId(), newVisitDate),
                auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, resp.status(), resp.message());

        DepartmentInsuranceBilling after = findBilling(fx.visit().getId());

        Duration beforeGap = Duration.between(visitOriginal, before.getBillingDate());
        Duration afterGap = Duration.between(newVisitDate, after.getBillingDate());
        assertEquals(beforeGap, afterGap,
            "billing date should shift backward by exactly the same delta as the visit date");
        assertFalse(after.getBillingDate().isBefore(newVisitDate.plusMinutes(5)),
            "billing date must never be before the new visit date + 5 minutes");
    }

    // ─── Scenario 2: unbilled visit with future visit date, then bill ───

    @Test
    @Transactional
    void unbilledVisitWithFutureDate_billClampsBillingDateAfterVisit() {
        LocalDateTime futureVisit = LocalDateTime.now().plusDays(30);
        Fixture fx = createVisitWithProduct(futureVisit);

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput(fx), auth(fx.actor())).status());

        DepartmentInsuranceBilling billing = findBilling(fx.visit().getId());
        assertNotNull(billing.getBillingDate());
        // Current billing time was before the (future) visit date, so billing date
        // must be clamped to at least 5 minutes after the visit date, never backdated.
        assertTrue(billing.getBillingDate().compareTo(futureVisit.minusMinutes(1)) >= 0,
            "billing date must not be before the visit date");
        assertFalse(billing.getBillingDate().isBefore(futureVisit.plusMinutes(5)),
            "billing date must be clamped to at least 5 minutes after the visit date");
    }
}
