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
import com.nexxserve.nexxclinic.repository.VisitBillingVersionRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ExemptionTypeIntegrationTest {

    @Autowired private VisitBillingService visitBillingService;
    @Autowired private WorkerRepository workerRepository;
    @Autowired private PatientRepository patientRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private VisitRepository visitRepository;
    @Autowired private VisitDepartmentRepository visitDepartmentRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VisitDepartmentProductRepository visitDepartmentProductRepository;
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
        Worker actor = worker("exempt-actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Exempt");
        patient.setLastName("Patient");
        patient.setFullName("Exempt Patient");
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

        Product product = product("Exempt Act", new BigDecimal("100.00"));

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

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractItem(Map<String, Object> billingData) {
        List<Map<String, Object>> departments =
            (List<Map<String, Object>>) billingData.get("departments");
        Map<String, Object> dept = departments.get(0);
        List<Map<String, Object>> insuranceBillings =
            (List<Map<String, Object>>) dept.get("insuranceBillings");
        Map<String, Object> ib = insuranceBillings.get(0);
        List<Map<String, Object>> items = (List<Map<String, Object>>) ib.get("items");
        return items.get(0);
    }

    private BillVisitInput billInput(Fixture fx, ExemptionType exemptionType) {
        List<BillVisitInput.BillingPaymentInput> payments =
            (exemptionType == ExemptionType.NONE)
                ? List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("100.00"), PaymentMethod.CASH, null))
                : List.of();
        return new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, exemptionType)),
                payments,
                "Test billing"
            ))
        );
    }

    private BillVisitInput billInputNoPayment(Fixture fx, ExemptionType exemptionType) {
        return new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, exemptionType)),
                List.of(),
                "Test billing"
            ))
        );
    }

    private BillVisitInput billInputNoNote(Fixture fx, ExemptionType exemptionType) {
        return new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                    fx.product().getId(), null, BigDecimal.ONE,
                    CoverageType.PRIVATE, null, exemptionType)),
                List.of(),
                null
            ))
        );
    }

    // ─── 1. FULL exemption zeroes all amounts ──────────────────

    @Test
    void billVisit_withFullExemption_allAmountsZero() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInput(fx, ExemptionType.FULL), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("0.00"), item.get("unitPriceSnapshot"));
        assertEquals(new BigDecimal("0.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("0.00"), item.get("insuranceCoveredAmount"));
        assertEquals(new BigDecimal("0.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.EXEMPTED, vdp.getStatus());
    }

    // ─── 2. PATIENT_SHARE on PRIVATE — lineTotal kept, covered=0, patientAmount=0 ─

    @Test
    void billVisit_withPatientShareExemption_onPrivate_fullLineTotal() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInput(fx, ExemptionType.PATIENT_SHARE), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("100.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("0.00"), item.get("insuranceCoveredAmount"));
        assertEquals(new BigDecimal("0.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.PATIENT_SHARE_EXEMPTED, vdp.getStatus());
    }

    // ─── 3. NONE exemption — normal billing ────────────────────

    @Test
    void billVisit_withNoExemption_normalBilling() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInput(fx, ExemptionType.NONE), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("100.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("100.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.BILLED, vdp.getStatus());
    }

    // ─── 4. Edit: NONE → FULL exemption ───────────────────────

    @Test
    void editBillVisit_changeFromNoneToFullExemption() {
        Fixture fx = createVisitWithProduct();

        // Bill normally with NONE but no payment (outstanding=100) so the edit
        // to FULL (patientPayable=0) does not carry forward a previous payment.
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(
                billInputNoPayment(fx, ExemptionType.NONE), auth(fx.actor())).status());

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE,
                    null, ExemptionType.FULL)),
                List.of(),
                "Changed to full exemption"
            ))
        );

        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("0.00"), item.get("unitPriceSnapshot"));
        assertEquals(new BigDecimal("0.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("0.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.EXEMPTED, vdp.getStatus());
    }

    // ─── 5. Edit: NONE → PATIENT_SHARE exemption ──────────────

    @Test
    void editBillVisit_changeFromNoneToPatientShareExemption() {
        Fixture fx = createVisitWithProduct();

        // Bill normally with NONE but no payment so the edit to PATIENT_SHARE
        // does not carry forward a previous payment that would exceed patientPayable.
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(
                billInputNoPayment(fx, ExemptionType.NONE), auth(fx.actor())).status());

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE,
                    null, ExemptionType.PATIENT_SHARE)),
                List.of(),
                "Changed to patient share exemption"
            ))
        );

        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("100.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("0.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.PATIENT_SHARE_EXEMPTED, vdp.getStatus());
    }

    // ─── 6. Edit: FULL → PATIENT_SHARE exemption ──────────────

    @Test
    void editBillVisit_changeFromFullToPatientShareExemption() {
        Fixture fx = createVisitWithProduct();

        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(
                billInput(fx, ExemptionType.FULL), auth(fx.actor())).status());

        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null, null, null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.catalogProductId(), null, CoverageType.PRIVATE,
                    null, ExemptionType.PATIENT_SHARE)),
                List.of(),
                "Changed from full to patient share"
            ))
        );

        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> billingData = (Map<String, Object>) response.data();
        Map<String, Object> item = extractItem(billingData);

        assertEquals(new BigDecimal("100.00"), item.get("lineTotal"));
        assertEquals(new BigDecimal("0.00"), item.get("patientPayableAmount"));

        VisitDepartmentProduct vdp = visitDepartmentProductRepository
            .findById(fx.product().getId()).orElseThrow();
        assertEquals(VisitProductStatus.PATIENT_SHARE_EXEMPTED, vdp.getStatus());
    }

    // ─── 7. FULL exemption requires note ──────────────────────

    @Test
    void billVisit_fullExemption_requiresNote() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInputNoNote(fx, ExemptionType.FULL), auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("billing note is required"), response.message());
    }

    // ─── 8. PATIENT_SHARE exemption requires note ─────────────

    @Test
    void billVisit_patientShareExemption_requiresNote() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInputNoNote(fx, ExemptionType.PATIENT_SHARE), auth(fx.actor()));
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("billing note is required"), response.message());
    }

    // ─── 9. isVisitFullyBilled counts PATIENT_SHARE_EXEMPTED ─

    @Test
    void isVisitFullyBilled_withPatientShareExempted() {
        Fixture fx = createVisitWithProduct();
        ApiResponse<?> response = visitBillingService.billVisit(
            billInput(fx, ExemptionType.PATIENT_SHARE), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        Visit visit = visitRepository.findById(fx.visit().getId()).orElseThrow();
        assertEquals(VisitStatus.COMPLETED, visit.getStatus(),
            "PATIENT_SHARE_EXEMPTED product should count as fully billed");
    }
}
