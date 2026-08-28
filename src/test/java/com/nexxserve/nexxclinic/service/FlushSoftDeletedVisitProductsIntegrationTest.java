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
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FlushSoftDeletedVisitProductsIntegrationTest {

    @Autowired private VisitBillingService visitBillingService;
    @Autowired private BillEditingService billEditingService;
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
        VisitDepartmentProduct product,
        VisitDepartmentProduct keepProduct
    ) {
        UUID catalogProductId() { return product.getProduct().getId(); }
        UUID keepCatalogProductId() { return keepProduct.getProduct().getId(); }
    }

    private Fixture createVisitWithTwoProducts() {
        Worker actor = worker("flush-actor-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        Patient patient = new Patient();
        patient.setFirstName("Flush");
        patient.setLastName("Patient");
        patient.setFullName("Flush Patient");
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

        Product product = product("Flush Act", new BigDecimal("100.00"));
        Product keepProduct = product("Keep Act", new BigDecimal("50.00"));

        VisitDepartmentProduct vdp = new VisitDepartmentProduct();
        vdp.setVisitDepartment(vd);
        vdp.setProduct(product);
        vdp.setQuantity(BigDecimal.ONE);
        vdp.setStatus(VisitProductStatus.PENDING);
        vdp.setSource(VisitDepartmentProductSource.USER);
        vdp.setAddedBy(actor);
        vdp = visitDepartmentProductRepository.save(vdp);

        VisitDepartmentProduct keepVdp = new VisitDepartmentProduct();
        keepVdp.setVisitDepartment(vd);
        keepVdp.setProduct(keepProduct);
        keepVdp.setQuantity(BigDecimal.ONE);
        keepVdp.setStatus(VisitProductStatus.PENDING);
        keepVdp.setSource(VisitDepartmentProductSource.USER);
        keepVdp.setAddedBy(actor);
        keepVdp = visitDepartmentProductRepository.save(keepVdp);

        visitDepartmentProductRepository.flush();

        return new Fixture(actor, visit, vd, vdp, keepVdp);
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

    private void softDeleteProduct(Fixture fx) {
        EditBillVisitInput input = new EditBillVisitInput(
            fx.visit().getId(),
            List.of(new EditBillVisitInput.EditBillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                null,
                List.of(fx.catalogProductId()),
                null,
                List.of(new EditBillVisitInput.EditBillVisitBillProductInput(
                    fx.keepCatalogProductId(), null, CoverageType.PRIVATE,
                    null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("50.00"), PaymentMethod.CASH, null)),
                "Soft delete product"
            , null, null))
        );
        ApiResponse<?> response = visitBillingService.editBillVisit(input, auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status(),
            "soft-delete via editBillVisit failed: " + response.message());
    }

    // ─── 1. Soft-deleted with no billing history → hard-deleted ──

    @Test
    void flush_softDeletedNoBillingHistory_hardDeleted() {
        Fixture fx = createVisitWithTwoProducts();

        softDeleteProduct(fx);

        assertTrue(
            visitDepartmentProductRepository.findById(fx.product().getId()).orElseThrow().isDeleted(),
            "product should be soft-deleted before flush");

        ApiResponse<?> response = visitBillingService.flushSoftDeletedVisitProducts(
            fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();
        assertEquals(1, data.get("deletedCount"));

        assertFalse(visitDepartmentProductRepository.findById(fx.product().getId()).isPresent(),
            "product should be hard-deleted after flush");
    }

    // ─── 2. Soft-deleted with billing history → skipped ──────────

    @Test
    void flush_softDeletedWithBillingHistory_skipped() {
        Fixture fx = createVisitWithTwoProducts();

        // Bill both products so the target product has billing history
        BillVisitInput billInput = new BillVisitInput(
            fx.visit().getId(),
            List.of(new BillVisitInput.BillVisitDepartmentInput(
                fx.visitDepartment().getId(),
                List.of(
                    new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.product().getId(), null, BigDecimal.ONE,
                        CoverageType.PRIVATE, null, ExemptionType.NONE, null),
                    new BillVisitInput.BillVisitDepartmentProductInput(
                        fx.keepProduct().getId(), null, BigDecimal.ONE,
                        CoverageType.PRIVATE, null, ExemptionType.NONE, null)),
                List.of(new BillVisitInput.BillingPaymentInput(
                    new BigDecimal("150.00"), PaymentMethod.CASH, null)),
                "Bill before soft-delete"
            , null, null))
        );
        assertEquals(ResponseStatus.SUCCESS,
            visitBillingService.billVisit(billInput, auth(fx.actor())).status());

        // Enter BILL_EDITING mode before soft-deleting
        billEditingService.startBillEditing(fx.visit().getId(), auth(fx.actor()));

        // Now soft-delete via edit (remove only fx.product, keep keepProduct)
        softDeleteProduct(fx);

        assertTrue(
            visitDepartmentProductRepository.findById(fx.product().getId()).orElseThrow().isDeleted(),
            "product should be soft-deleted");

        ApiResponse<?> response = visitBillingService.flushSoftDeletedVisitProducts(
            fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();
        assertEquals(0, data.get("deletedCount"));
        assertEquals(1, data.get("skippedCount"));

        assertTrue(visitDepartmentProductRepository.findById(fx.product().getId()).isPresent(),
            "product with billing history should NOT be hard-deleted");
    }

    // ─── 3. No soft-deleted products → deletedCount=0 ──────────

    @Test
    void flush_noSoftDeletedProducts_returnsZero() {
        Fixture fx = createVisitWithTwoProducts();

        ApiResponse<?> response = visitBillingService.flushSoftDeletedVisitProducts(
            fx.visit().getId(), auth(fx.actor()));
        assertEquals(ResponseStatus.SUCCESS, response.status());

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.data();
        assertEquals(0, data.get("deletedCount"));
    }

    // ─── 4. Null visitId is rejected ──────────────────────────

    @Test
    void flush_nullVisitId_rejected() {
        Worker actor = worker("flush-null-" + UUID.randomUUID() + "@test.com");
        actor = workerRepository.save(actor);

        ApiResponse<?> response = visitBillingService.flushSoftDeletedVisitProducts(
            null, auth(actor));
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(response.message().contains("visitId is required"), response.message());
    }
}
