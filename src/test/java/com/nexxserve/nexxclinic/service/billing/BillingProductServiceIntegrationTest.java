package com.nexxserve.nexxclinic.service.billing;

import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentDto;
import com.nexxserve.nexxclinic.dto.out.VisitDepartmentProductDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentProduct;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.graphql.VisitBillingQueryController;
import com.nexxserve.nexxclinic.graphql.input.BillVisitInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentProductQuantityInput;
import com.nexxserve.nexxclinic.model.BillingConfirmationStatus;
import com.nexxserve.nexxclinic.model.BillingState;
import com.nexxserve.nexxclinic.model.CoverageType;
import com.nexxserve.nexxclinic.model.EncounterType;
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
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingItemRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import com.nexxserve.nexxclinic.service.VisitBillingService;
import com.nexxserve.nexxclinic.service.VisitDepartmentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BillingProductServiceIntegrationTest {

    @Autowired
    private BillingProductService billingProductService;

    @Autowired
    private VisitDepartmentService visitDepartmentService;

    @Autowired
    private VisitBillingService visitBillingService;

    @Autowired
    private VisitBillingQueryController visitBillingQueryController;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WorkerRepository workerRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

    @Autowired
    private VisitBillingItemRepository visitBillingItemRepository;

    @Autowired
    private VisitDepartmentBillingRepository visitDepartmentBillingRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.InsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.PatientInsuranceRepository patientInsuranceRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.VisitInsuranceRepository visitInsuranceRepository;

    @Autowired
    private com.nexxserve.nexxclinic.repository.ProductInsuranceCoverageRepository productInsuranceCoverageRepository;

    private Patient patient;
    private Department department;
    private Product product;
    private Worker worker;
    private Visit visit;
    private VisitDepartment visitDepartment;
    private AuthenticatedUser authUser;

    @BeforeEach
    void setUp() {
        worker = new Worker();
        worker.setFirstName("Billing");
        worker.setLastName("Officer");
        worker.setUsername("billing_" + UUID.randomUUID().toString().substring(0, 8));
        worker.setEmail("billing_" + UUID.randomUUID().toString().substring(0, 8) + "@clinic.com");
        worker.setGender(Gender.MALE);
        worker.setAccountStatus(com.nexxserve.nexxclinic.model.AccountStatus.ACTIVE);
        worker.setRoles(Set.of(RoleName.ADMIN, RoleName.FINANCE));
        worker.setActive(true);
        worker = workerRepository.saveAndFlush(worker);

        authUser = new AuthenticatedUser(
                worker.getId(),
                worker.getUsername(),
                Set.of(RoleName.FINANCE, RoleName.ADMIN),
                "token",
                java.time.Instant.now().plusSeconds(3600)
        );

        patient = new Patient();
        patient.setPatientIdentifier("PAT-" + UUID.randomUUID().toString().substring(0, 8));
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setFullName("Jane Doe");
        patient.setDateOfBirth(LocalDate.of(1995, 5, 10));
        patient.setGender(Gender.FEMALE);
        patient = patientRepository.saveAndFlush(patient);

        department = new Department();
        department.setName("Pharmacy Dept " + UUID.randomUUID().toString().substring(0, 8));
        department = departmentRepository.saveAndFlush(department);

        product = new Product();
        product.setName("Syringe 5ml");
        product.setCode("SYR-" + UUID.randomUUID().toString().substring(0, 6));
        product.setType(ProductType.CONSUMABLE_DEVICE);
        product.setUnit(ProductUnit.PIECE);
        product.setMetadata("{}");
        product.setClinicPrice(new BigDecimal("15.00"));
        product = productRepository.saveAndFlush(product);

        visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(LocalDateTime.now());
        visit = visitRepository.saveAndFlush(visit);

        visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);
        visitDepartment.setStatus(VisitDepartmentStatus.ACTIVE);
        visitDepartment.setEncounterType(EncounterType.OUTPATIENT);
        visitDepartment = visitDepartmentRepository.saveAndFlush(visitDepartment);
    }

    private void billVisitPrivate(VisitDepartmentProduct vdp, BigDecimal price) {
        BillVisitInput billInput = new BillVisitInput(
                visit.getId(),
                List.of(new BillVisitInput.BillVisitDepartmentInput(
                        visitDepartment.getId(),
                        List.of(new BillVisitInput.BillVisitDepartmentProductInput(
                                vdp.getId(),
                                null,
                                vdp.getQuantity(),
                                CoverageType.PRIVATE,
                                null,
                                ExemptionType.NONE,
                                null
                        )),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                price.multiply(vdp.getQuantity()),
                                PaymentMethod.CASH,
                                null
                        )),
                        "Initial bill",
                        null,
                        null
                ))
        );
        ApiResponse<?> res = visitBillingService.billVisit(billInput, authUser);
        assertEquals(ResponseStatus.SUCCESS, res.status(), "billVisit failed: " + res.message());
    }

    @Test
    void testAddProductAndBill_createsProductAndBillingItem() {
        var clinicalAddInput = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                product.getId(),
                null,
                new BigDecimal("2.0"),
                null
        );
        ApiResponse<VisitDepartmentDto> addRes = visitDepartmentService.addVisitDepartmentProduct(clinicalAddInput, authUser);
        assertEquals(ResponseStatus.SUCCESS, addRes.status());

        var products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        assertEquals(1, products.size());
        VisitDepartmentProduct vdp = products.get(0);

        billVisitPrivate(vdp, new BigDecimal("15.00"));

        // Check department product is in BILLED status
        VisitDepartmentProduct billedVdp = visitDepartmentProductRepository.findById(vdp.getId()).orElseThrow();
        assertEquals(product.getId(), billedVdp.getProduct().getId());
        assertEquals(0, new BigDecimal("2.0000").compareTo(billedVdp.getQuantity()));
        assertEquals(VisitProductStatus.BILLED, billedVdp.getStatus());

        // Check billing item exists
        var billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(vdp.getId());
        assertEquals(1, billingItems.size());
        assertEquals(new BigDecimal("30.00"), billingItems.get(0).getLineTotal());

        // Test Operator Confirmation
        ApiResponse<VisitDepartmentDto> confirmRes = visitDepartmentService.confirmVisitDepartmentProduct(vdp.getId(), authUser);
        assertEquals(ResponseStatus.SUCCESS, confirmRes.status());

        VisitDepartmentProduct confirmedVdp = visitDepartmentProductRepository.findById(vdp.getId()).orElseThrow();
        assertEquals(BillingConfirmationStatus.CONFIRMED, confirmedVdp.getBillingConfirmationStatus());
        assertEquals(worker.getId(), confirmedVdp.getConfirmedBy().getId());

        // Test direct SchemaMapping resolvers
        VisitDepartmentDto deptDto = new VisitDepartmentDto(
                visitDepartment.getId(), null, VisitDepartmentStatus.ACTIVE,
                EncounterType.OUTPATIENT, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now()
        );
        Map<String, Object> deptBillingMap = visitBillingQueryController.departmentBilling(deptDto);
        assertNotNull(deptBillingMap);
        assertEquals(new BigDecimal("30.00"), deptBillingMap.get("totalAmount"));

        VisitDepartmentProductDto prodDto = new VisitDepartmentProductDto(
                vdp.getId(), null, new BigDecimal("2.0"), VisitProductStatus.BILLED,
                BillingState.BILLED, ExemptionType.NONE,
                null, null, null, null, BillingConfirmationStatus.CONFIRMED, null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        Map<String, Object> itemMap = visitBillingQueryController.productBillingItem(prodDto);
        assertNotNull(itemMap);
        assertEquals(new BigDecimal("30.00"), itemMap.get("lineTotal"));
    }

    @Test
    void testSyncClinicalProductChangeToBilling_softDeletedProductZerosBillingItem() {
        var clinicalAddInput = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                product.getId(),
                null,
                new BigDecimal("2.0"),
                null
        );
        visitDepartmentService.addVisitDepartmentProduct(clinicalAddInput, authUser);

        var products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        VisitDepartmentProduct vdp = products.get(0);
        billVisitPrivate(vdp, new BigDecimal("15.00"));

        var billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(vdp.getId());
        UUID billingItemId = billingItems.get(0).getId();

        // Simulate soft deletion and test syncClinicalProductChangeToBilling
        vdp.setDeleted(true);
        visitDepartmentProductRepository.saveAndFlush(vdp);
        billingProductService.syncClinicalProductChangeToBilling(vdp);

        var softDeletedProduct = visitDepartmentProductRepository.findById(vdp.getId()).orElseThrow();
        assertTrue(softDeletedProduct.isDeleted());

        var zeroedItem = visitBillingItemRepository.findById(billingItemId).orElseThrow();
        assertEquals(new BigDecimal("0.00"), zeroedItem.getLineTotal());

        var deptBilling = visitDepartmentBillingRepository.findByVisitDepartmentId(visitDepartment.getId()).get(0);
        assertEquals(new BigDecimal("0.00"), deptBilling.getTotalAmount());
    }

    @Test
    void testDoctorAddsAndUpdatesProduct_automaticallySyncsToBilling() {
        // 1. Initial product and bill exist
        var clinicalAddInput1 = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                product.getId(),
                null,
                BigDecimal.ONE,
                null
        );
        visitDepartmentService.addVisitDepartmentProduct(clinicalAddInput1, authUser);

        var products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        VisitDepartmentProduct vdp = products.get(0);
        billVisitPrivate(vdp, new BigDecimal("15.00"));

        // 2. Doctor creates another product via standard clinical mutation addVisitDepartmentProduct
        Product secondProduct = new Product();
        secondProduct.setName("Paracetamol 500mg");
        secondProduct.setCode("PCM-" + UUID.randomUUID().toString().substring(0, 6));
        secondProduct.setType(ProductType.DRUG);
        secondProduct.setUnit(ProductUnit.PIECE);
        secondProduct.setMetadata("{}");
        secondProduct.setClinicPrice(new BigDecimal("5.00"));
        secondProduct = productRepository.saveAndFlush(secondProduct);

        var clinicalAddInput2 = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                secondProduct.getId(),
                null,
                BigDecimal.ONE,
                null
        );

        ApiResponse<VisitDepartmentDto> addRes = visitDepartmentService.addVisitDepartmentProduct(clinicalAddInput2, authUser);
        assertEquals(ResponseStatus.SUCCESS, addRes.status());

        // Verify billing item was auto-created for the second product
        var secondVdp = visitDepartmentProductRepository
                .findByVisitDepartmentIdAndProductId(visitDepartment.getId(), secondProduct.getId())
                .orElseThrow();
        var billingItems = visitBillingItemRepository.findByVisitDepartmentProductId(secondVdp.getId());
        assertEquals(1, billingItems.size());
        assertEquals(new BigDecimal("5.00"), billingItems.get(0).getLineTotal());

        // Verify department billing total was updated to 15.00 + 5.00 = 20.00
        var deptBillings = visitDepartmentBillingRepository.findByVisitDepartmentId(visitDepartment.getId());
        assertEquals(new BigDecimal("20.00"), deptBillings.get(0).getTotalAmount());

        // 3. Doctor updates quantity of second product to 4.0
        var clinicalUpdateInput = new UpdateVisitDepartmentProductQuantityInput(
                secondVdp.getId(),
                new BigDecimal("4.0")
        );
        ApiResponse<VisitDepartmentDto> updateRes = visitDepartmentService.updateVisitDepartmentProductQuantity(clinicalUpdateInput);
        assertEquals(ResponseStatus.SUCCESS, updateRes.status());

        // Verify billing item recalculated to 4 * 5.00 = 20.00, dept total = 15.00 + 20.00 = 35.00
        var updatedBillingItem = visitBillingItemRepository.findByVisitDepartmentProductId(secondVdp.getId()).get(0);
        assertEquals(new BigDecimal("20.00"), updatedBillingItem.getLineTotal());

        var updatedDeptBilling = visitDepartmentBillingRepository.findByVisitDepartmentId(visitDepartment.getId()).get(0);
        assertEquals(new BigDecimal("35.00"), updatedDeptBilling.getTotalAmount());
    }

    @Test
    void testSyncBilledProductsToVisitDepartments() {
        var clinicalAddInput = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                product.getId(),
                null,
                new BigDecimal("3.0"),
                null
        );
        visitDepartmentService.addVisitDepartmentProduct(clinicalAddInput, authUser);

        var products = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        VisitDepartmentProduct vdp = products.get(0);
        billVisitPrivate(vdp, new BigDecimal("15.00"));

        // Call synchronization explicitly
        billingProductService.syncBilledProductsToVisitDepartments(visit.getId());

        var refreshedProducts = visitDepartmentProductRepository.findByVisitDepartmentId(visitDepartment.getId());
        assertEquals(1, refreshedProducts.size());
        VisitDepartmentProduct refreshedVdp = refreshedProducts.get(0);
        assertEquals(0, new BigDecimal("3.0000").compareTo(refreshedVdp.getQuantity()));
        assertEquals(VisitProductStatus.BILLED, refreshedVdp.getStatus());
        assertNotNull(refreshedVdp.getBillingConfirmationStatus());
    }

    @Test
    void testInsuranceBillingWithDifferentPercentagesAndGetDepartmentBillingQuery() {
        // 1. Setup Insurance Provider & Patient Insurance with 15% co-pay (85% insurance coverage)
        var provider = new com.nexxserve.nexxclinic.entity.InsuranceProvider();
        provider.setInsuranceName("RSSB / RAMA " + UUID.randomUUID().toString().substring(0, 6));
        provider.setAcronym("RAMA-" + UUID.randomUUID().toString().substring(0, 4));
        provider.setSupportedByClinic(true);
        provider = insuranceProviderRepository.saveAndFlush(provider);

        var patientInsurance = new com.nexxserve.nexxclinic.entity.PatientInsurance();
        patientInsurance.setPatient(patient);
        patientInsurance.setInsuranceProvider(provider);
        patientInsurance.setInsuranceCardNumber("CARD-" + UUID.randomUUID().toString().substring(0, 8));
        patientInsurance.setPrincipalMember(true);
        patientInsurance.setValidFrom(LocalDate.now().minusDays(10));
        patientInsurance.setValidUntil(LocalDate.now().plusYears(1));
        patientInsurance.setPatientSharePercentage(15);
        patientInsurance = patientInsuranceRepository.saveAndFlush(patientInsurance);

        var visitInsurance = new com.nexxserve.nexxclinic.entity.VisitInsurance();
        visitInsurance.setVisit(visit);
        visitInsurance.setPatientInsurance(patientInsurance);
        visitInsuranceRepository.saveAndFlush(visitInsurance);

        // 2. Add product 1 with 15% patient share (total 100.00 -> 85.00 covered by insurance, 15.00 patient payable)
        Product labTest = new Product();
        labTest.setName("Full Blood Count");
        labTest.setCode("FBC-" + UUID.randomUUID().toString().substring(0, 6));
        labTest.setType(ProductType.BIOLOGICAL_ACT);
        labTest.setUnit(ProductUnit.PIECE);
        labTest.setMetadata("{}");
        labTest.setClinicPrice(new BigDecimal("100.00"));
        labTest = productRepository.saveAndFlush(labTest);

        var labCoverage = new com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage();
        labCoverage.setProduct(labTest);
        labCoverage.setInsuranceProvider(provider);
        labCoverage.setCost(new BigDecimal("100.00"));
        labCoverage.setCovered(true);
        labCoverage.setNotPaid(false);
        labCoverage.setRequireMedicalAdvisor(false);
        labCoverage.setMustPrescribedBy(com.nexxserve.nexxclinic.model.MustPrescribedBy.ALL);
        labCoverage.setDrugAdministrationFrequency(com.nexxserve.nexxclinic.model.DrugAdministrationFrequency.CUSTOM_HOURS);
        productInsuranceCoverageRepository.saveAndFlush(labCoverage);

        var addInput1 = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                labTest.getId(),
                null,
                BigDecimal.ONE,
                null
        );
        visitDepartmentService.addVisitDepartmentProduct(addInput1, authUser);

        // 3. Add product 2 with 10% override patient share (total 50.00)
        Product xray = new Product();
        xray.setName("Chest X-Ray");
        xray.setCode("XRAY-" + UUID.randomUUID().toString().substring(0, 6));
        xray.setType(ProductType.MEDICAL_ACT);
        xray.setUnit(ProductUnit.PIECE);
        xray.setMetadata("{}");
        xray.setClinicPrice(new BigDecimal("50.00"));
        xray = productRepository.saveAndFlush(xray);

        var xrayCoverage = new com.nexxserve.nexxclinic.entity.ProductInsuranceCoverage();
        xrayCoverage.setProduct(xray);
        xrayCoverage.setInsuranceProvider(provider);
        xrayCoverage.setCost(new BigDecimal("50.00"));
        xrayCoverage.setCovered(true);
        xrayCoverage.setNotPaid(false);
        xrayCoverage.setRequireMedicalAdvisor(false);
        xrayCoverage.setMustPrescribedBy(com.nexxserve.nexxclinic.model.MustPrescribedBy.ALL);
        xrayCoverage.setDrugAdministrationFrequency(com.nexxserve.nexxclinic.model.DrugAdministrationFrequency.CUSTOM_HOURS);
        productInsuranceCoverageRepository.saveAndFlush(xrayCoverage);

        var addInput2 = new CreateVisitDepartmentProductInput(
                visit.getId(),
                department.getId(),
                xray.getId(),
                null,
                BigDecimal.ONE,
                null
        );
        visitDepartmentService.addVisitDepartmentProduct(addInput2, authUser);

        var labVdp = visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(visitDepartment.getId(), labTest.getId()).orElseThrow();
        var xrayVdp = visitDepartmentProductRepository.findByVisitDepartmentIdAndProductId(visitDepartment.getId(), xray.getId()).orElseThrow();

        // Bill the visit with Insurance
        BillVisitInput billInput = new BillVisitInput(
                visit.getId(),
                List.of(new BillVisitInput.BillVisitDepartmentInput(
                        visitDepartment.getId(),
                        List.of(
                                new BillVisitInput.BillVisitDepartmentProductInput(
                                        labVdp.getId(),
                                        null,
                                        BigDecimal.ONE,
                                        CoverageType.INSURANCE,
                                        patientInsurance.getId(),
                                        ExemptionType.NONE,
                                        null
                                ),
                                new BillVisitInput.BillVisitDepartmentProductInput(
                                        xrayVdp.getId(),
                                        null,
                                        BigDecimal.ONE,
                                        CoverageType.INSURANCE,
                                        patientInsurance.getId(),
                                        ExemptionType.NONE,
                                        null
                                )
                        ),
                        List.of(new BillVisitInput.BillingPaymentInput(
                                new BigDecimal("22.50"),
                                PaymentMethod.CASH,
                                null
                        )),
                        "Insurance Bill",
                        null,
                        null
                ))
        );
        ApiResponse<?> billRes = visitBillingService.billVisit(billInput, authUser);
        assertEquals(ResponseStatus.SUCCESS, billRes.status(), "billVisit failed: " + billRes.message());

        // 4. Test query getVisitDepartmentBilling
        ApiResponse deptBillingRes = visitBillingQueryController.getVisitDepartmentBilling(visitDepartment.getId(), authUser);
        assertEquals(ResponseStatus.SUCCESS, deptBillingRes.status());
        assertNotNull(deptBillingRes.data());
        Map<String, Object> deptBillingData = (Map<String, Object>) deptBillingRes.data();

        // Total: 100 + 50 = 150.00
        // Insurance Covered: 85.00 + 42.50 = 127.50
        // Patient Payable: 15.00 + 7.50 = 22.50
        assertEquals(new BigDecimal("150.00"), deptBillingData.get("totalAmount"));
        assertEquals(new BigDecimal("127.50"), deptBillingData.get("insuranceCoveredAmount"));
        assertEquals(new BigDecimal("22.50"), deptBillingData.get("patientPayableAmount"));
    }
}
