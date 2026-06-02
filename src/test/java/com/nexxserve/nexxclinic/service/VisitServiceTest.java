package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Product;
import com.nexxserve.nexxclinic.entity.Worker;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.auth.AuthenticatedUser;
import com.nexxserve.nexxclinic.graphql.input.SearchPatientHistoryInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignItemInput;
import com.nexxserve.nexxclinic.graphql.input.AddVisitVitalSignsInput;
import com.nexxserve.nexxclinic.graphql.input.AddDiagnosisInput;
import com.nexxserve.nexxclinic.graphql.input.AddMedicationInput;
import com.nexxserve.nexxclinic.graphql.input.CreateVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.graphql.input.UpdateVisitDepartmentStatusInput;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentInput;
import com.nexxserve.nexxclinic.graphql.input.AddChildVisitDepartmentProductInput;
import com.nexxserve.nexxclinic.model.ApiResponse;
import com.nexxserve.nexxclinic.model.AccountStatus;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.ProductType;
import com.nexxserve.nexxclinic.model.ProductUnit;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.ProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentProductRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.WorkerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@SuppressWarnings("unchecked")
class VisitServiceTest {

    @Autowired
    private VisitService visitService;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private InsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private PatientInsuranceRepository patientInsuranceRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private VisitDepartmentProductRepository visitDepartmentProductRepository;

    @Autowired
    private VisitInsuranceRepository visitInsuranceRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private WorkerRepository workerRepository;

    private VisitDepartment visitDepartment;
    private Worker processorOne;
    private Worker processorTwo;
    private Product product;

    @BeforeEach
    void setUp() {
        Patient patient = new Patient();
        patient.setFirstName("John");
        patient.setLastName("Doe");
        patient.setFullName("John Doe");
        patient.setDateOfBirth(java.time.LocalDate.now().minusYears(30));
        patient.setGender(com.nexxserve.nexxclinic.model.Gender.MALE);
        patient = patientRepository.save(patient);

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.IN_PROGRESS);
        visit.setVisitDate(java.time.LocalDateTime.now());
        visit = visitRepository.save(visit);

        Department department = new Department();
        department.setName("General Medicine");
        department = departmentRepository.save(department);

        visitDepartment = new VisitDepartment();
        visitDepartment.setVisit(visit);
        visitDepartment.setDepartment(department);
        visitDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartment = visitDepartmentRepository.save(visitDepartment);

        processorOne = new Worker();
        processorOne.setFirstName("Proc");
        processorOne.setLastName("One");
        processorOne.setUsername("proc-one");
        processorOne.setAccountStatus(AccountStatus.ACTIVE);
        processorOne.setActive(true);
        processorOne.setMaxActiveSessions(1);
        processorOne = workerRepository.save(processorOne);

        processorTwo = new Worker();
        processorTwo.setFirstName("Proc");
        processorTwo.setLastName("Two");
        processorTwo.setUsername("proc-two");
        processorTwo.setAccountStatus(AccountStatus.ACTIVE);
        processorTwo.setActive(true);
        processorTwo.setMaxActiveSessions(1);
        processorTwo = workerRepository.save(processorTwo);

        product = new Product();
        product.setName("Test Product");
        product.setCode("TP-001");
        product.setDescription("Test product");
        product.setType(ProductType.DRUG);
        product.setUnit(ProductUnit.TABLET);
        product.setClinicPrice(BigDecimal.valueOf(25));
        product = productRepository.save(product);
    }

    @Test
    void testAddDiagnosis() {
        AddDiagnosisInput input = new AddDiagnosisInput(
                visitDepartment.getId().toString(),
                "Flu",
                "ICD-10-Flu"
        );

        ApiResponse response = visitService.addDiagnosisToVisitDepartment(input);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertNotNull(data.get("id"));
        assertEquals("Flu", data.get("diagnosisName"));
        assertEquals("ICD-10-Flu", data.get("icd11Code"));
    }

    @Test
    void testAddMedication() {
        AddMedicationInput input = new AddMedicationInput(
                visitDepartment.getId().toString(),
                "Paracetamol",
                "Take 500mg twice a day"
        );

        ApiResponse response = visitService.addMedicationToVisitDepartment(input);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertNotNull(data.get("id"));
        assertEquals("Paracetamol", data.get("medicationName"));
        assertEquals("Take 500mg twice a day", data.get("instructions"));
    }

    @Test
    void testAddVisitVitalSigns() {
        AddVisitVitalSignItemInput weight = new AddVisitVitalSignItemInput("Weight", "58", "kg");
        AddVisitVitalSignItemInput pulse = new AddVisitVitalSignItemInput("Pulse rate", "72", "bpm");
        AddVisitVitalSignsInput input = new AddVisitVitalSignsInput(
                visitDepartment.getVisit().getId(),
                java.util.List.of(weight, pulse)
        );

        ApiResponse response = visitService.addVisitVitalSigns(input, null);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        Map<String, Object> data = (Map<String, Object>) response.data();
        assertTrue(data.containsKey("vitalSigns"));
        java.util.List<?> vitalSigns = (java.util.List<?>) data.get("vitalSigns");
        assertEquals(1, vitalSigns.size());

        Map<String, Object> group = (Map<String, Object>) vitalSigns.get(0);
        assertNotNull(group.get("id"));
        assertNotNull(group.get("createdAt"));
        assertNull(group.get("addedBy"));

        java.util.List<?> measurements = (java.util.List<?>) group.get("measurements");
        assertEquals(2, measurements.size());

        Map<String, Object> first = (Map<String, Object>) measurements.get(0);
        assertEquals("Weight", first.get("measurementName"));
        assertEquals("58", first.get("value"));
        assertEquals("kg", first.get("unit"));
    }

    @Test
    void testLinkAndUnlinkVisitInsurances() {
        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName("Test Insurance");
        provider.setDefaultCoveragePercentage(80);
        provider.setSupportedByClinic(true);
        provider = insuranceProviderRepository.save(provider);

        PatientInsurance patientInsurance = new PatientInsurance();
        patientInsurance.setPatient(visitDepartment.getVisit().getPatient());
        patientInsurance.setInsuranceProvider(provider);
        patientInsurance.setInsuranceCardNumber("CARD-001");
        patientInsurance.setPrincipalMember(true);
        patientInsurance.setValidFrom(LocalDate.now().minusDays(1));
        patientInsurance.setValidUntil(LocalDate.now().plusYears(1));
        patientInsurance = patientInsuranceRepository.save(patientInsurance);

        ApiResponse linkResponse = visitService.linkVisitInsurances(
                visitDepartment.getVisit().getId(),
                java.util.List.of(patientInsurance.getId()),
                null
        );

        assertEquals(ResponseStatus.SUCCESS, linkResponse.status());
        Map<String, Object> linkedVisit = (Map<String, Object>) linkResponse.data();
        java.util.List<?> linkedInsurances = (java.util.List<?>) linkedVisit.get("linkedInsurances");
        assertEquals(1, linkedInsurances.size());

        Map<String, Object> linkedInsurance = (Map<String, Object>) linkedInsurances.get(0);
        assertEquals(patientInsurance.getId(), UUID.fromString(linkedInsurance.get("id").toString()));

        ApiResponse unlinkResponse = visitService.unlinkVisitInsurances(
                visitDepartment.getVisit().getId(),
                java.util.List.of(patientInsurance.getId()),
                null
        );

        assertEquals(ResponseStatus.SUCCESS, unlinkResponse.status());
        Map<String, Object> unlinkedVisit = (Map<String, Object>) unlinkResponse.data();
        java.util.List<?> remainingInsurances = (java.util.List<?>) unlinkedVisit.get("linkedInsurances");
        assertTrue(remainingInsurances.isEmpty());
        assertTrue(visitInsuranceRepository.findByVisitId(visitDepartment.getVisit().getId()).isEmpty());
    }

    @Test
    void testLinkVisitInsurancesRejectsInsuranceOutsidePatientCoverage() {
        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName("Other Insurance");
        provider.setDefaultCoveragePercentage(70);
        provider.setSupportedByClinic(true);
        provider = insuranceProviderRepository.save(provider);

        Patient otherPatient = new Patient();
        otherPatient.setFirstName("Jane");
        otherPatient.setLastName("Roe");
        otherPatient.setFullName("Jane Roe");
        otherPatient.setDateOfBirth(LocalDate.now().minusYears(28));
        otherPatient.setGender(com.nexxserve.nexxclinic.model.Gender.FEMALE);
        otherPatient = patientRepository.save(otherPatient);

        PatientInsurance otherPatientInsurance = new PatientInsurance();
        otherPatientInsurance.setPatient(otherPatient);
        otherPatientInsurance.setInsuranceProvider(provider);
        otherPatientInsurance.setInsuranceCardNumber("CARD-002");
        otherPatientInsurance.setPrincipalMember(true);
        otherPatientInsurance.setValidFrom(LocalDate.now().minusDays(1));
        otherPatientInsurance.setValidUntil(LocalDate.now().plusYears(1));
        otherPatientInsurance = patientInsuranceRepository.save(otherPatientInsurance);

        ApiResponse response = visitService.linkVisitInsurances(
                visitDepartment.getVisit().getId(),
                java.util.List.of(otherPatientInsurance.getId()),
                null
        );

        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals("Each insurance must exist and belong to the selected patient.", response.message());
        assertTrue(visitInsuranceRepository.findByVisitId(visitDepartment.getVisit().getId()).isEmpty());
    }

    @Test
    void testGetPatientHistoryFiltersByYearAndPatient() {
        UUID requestedPatientId = visitDepartment.getVisit().getPatient().getId();
        UUID requestedDepartmentId = visitDepartment.getDepartment().getId();

        Department specialtyDepartment = new Department();
        specialtyDepartment.setName("Specialty");
        specialtyDepartment = departmentRepository.save(specialtyDepartment);

        Department extraDepartment = new Department();
        extraDepartment.setName("Extra");
        extraDepartment = departmentRepository.save(extraDepartment);

        Visit secondVisit = new Visit();
        secondVisit.setPatient(visitDepartment.getVisit().getPatient());
        secondVisit.setStatus(VisitStatus.IN_PROGRESS);
        secondVisit.setVisitDate(LocalDateTime.now().minusDays(2));
        secondVisit = visitRepository.save(secondVisit);

        VisitDepartment extraVisitDepartment = new VisitDepartment();
        extraVisitDepartment.setVisit(visitDepartment.getVisit());
        extraVisitDepartment.setDepartment(extraDepartment);
        extraVisitDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartmentRepository.save(extraVisitDepartment);

        VisitDepartment nonMatchingDepartment = new VisitDepartment();
        nonMatchingDepartment.setVisit(secondVisit);
        nonMatchingDepartment.setDepartment(specialtyDepartment);
        nonMatchingDepartment.setStatus(VisitDepartmentStatus.PENDING);
        visitDepartmentRepository.save(nonMatchingDepartment);

        SearchPatientHistoryInput input = new SearchPatientHistoryInput(
                LocalDate.now().getYear(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(requestedDepartmentId),
                0,
                20
        );

            ApiResponse response = visitService.getPatientHistory(requestedPatientId, input);
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertNotNull(response.data());

        java.util.List<?> visits = (java.util.List<?>) response.data();
        assertEquals(1, visits.size());

        Map<String, Object> visit = (Map<String, Object>) visits.get(0);
        Map<String, Object> patient = (Map<String, Object>) visit.get("patient");
        assertEquals(visitDepartment.getVisit().getPatient().getId(), UUID.fromString(patient.get("id").toString()));

        java.util.List<?> departments = (java.util.List<?>) visit.get("departments");
        assertEquals(1, departments.size());
        Map<String, Object> department = (Map<String, Object>) departments.get(0);
        assertNotNull(department.get("id"));

        Map<String, Object> pagination = (Map<String, Object>) response.pagination();
        assertEquals(1L, pagination.get("total"));
    }

        @Test
        void testActivatingDepartmentAddsCurrentUserAsProcessor() {
        AuthenticatedUser authUser = new AuthenticatedUser(
            processorOne.getId(),
            processorOne.getUsername(),
            Set.of(),
            null,
            null
        );

        UpdateVisitDepartmentStatusInput input = new UpdateVisitDepartmentStatusInput(
            visitDepartment.getId(),
            VisitDepartmentStatus.ACTIVE
        );

        ApiResponse response = visitService.updateVisitDepartmentStatus(input, authUser);
        assertEquals(ResponseStatus.SUCCESS, response.status());

        VisitDepartment refreshed = visitDepartmentRepository.findById(visitDepartment.getId()).orElseThrow();
        assertEquals(1, refreshed.getProcessors().size());
        assertEquals(processorOne.getId(), refreshed.getProcessors().get(0).getId());
        }

        @Test
        void testAddVisitDepartmentProductUsesExplicitProcessorWhenManyAreAvailable() {
        AuthenticatedUser authUser = new AuthenticatedUser(
            processorOne.getId(),
            processorOne.getUsername(),
            Set.of(),
            null,
            null
        );

        visitService.updateVisitDepartmentStatus(
            new UpdateVisitDepartmentStatusInput(visitDepartment.getId(), VisitDepartmentStatus.ACTIVE),
            authUser
        );
        visitService.addVisitDepartmentProcessor(visitDepartment.getId(), processorTwo.getId(), authUser);

        ApiResponse missingProcessorResponse = visitService.addVisitDepartmentProduct(
            new CreateVisitDepartmentProductInput(
                visitDepartment.getVisit().getId(),
                visitDepartment.getDepartment().getId(),
                product.getId(),
                null,
                BigDecimal.valueOf(3),
                null,
                null
            ),
            null
        );
        assertEquals(ResponseStatus.ERROR, missingProcessorResponse.status());

        ApiResponse explicitProcessorResponse = visitService.addVisitDepartmentProduct(
            new CreateVisitDepartmentProductInput(
                visitDepartment.getVisit().getId(),
                visitDepartment.getDepartment().getId(),
                product.getId(),
                processorTwo.getId(),
                BigDecimal.valueOf(3),
                null,
                null
            ),
            authUser
        );
        assertEquals(ResponseStatus.SUCCESS, explicitProcessorResponse.status());

        Map<String, Object> departmentData = (Map<String, Object>) explicitProcessorResponse.data();
        java.util.List<?> products = (java.util.List<?>) departmentData.get("products");
        assertEquals(1, products.size());

        Map<String, Object> addedProduct = (Map<String, Object>) products.get(0);
        Map<String, Object> processor = (Map<String, Object>) addedProduct.get("processor");
        assertEquals(processorTwo.getId(), UUID.fromString(processor.get("id").toString()));
        }

        @Test
        void testAddAndRemoveChildVisitDepartmentDeletesEmptyChild() {
        Department childDepartment = new Department();
        childDepartment.setName("Laboratory");
        childDepartment.setSupportRequests(true);
        childDepartment = departmentRepository.save(childDepartment);

        visitService.updateVisitDepartmentStatus(
            new UpdateVisitDepartmentStatusInput(visitDepartment.getId(), VisitDepartmentStatus.ACTIVE),
            new AuthenticatedUser(processorOne.getId(), processorOne.getUsername(), Set.of(), null, null)
        );

        ApiResponse addChildResponse = visitService.addChildVisitDepartment(
            new AddChildVisitDepartmentInput(
                visitDepartment.getId(),
                childDepartment.getId(),
                java.util.List.of(new AddChildVisitDepartmentProductInput(product.getId(), 1.0)),
                null,
                null
            ),
            new AuthenticatedUser(processorOne.getId(), processorOne.getUsername(), Set.of(), null, null)
        );
        assertEquals(ResponseStatus.SUCCESS, addChildResponse.status());

        java.util.List<VisitDepartment> children = visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId());
        assertEquals(1, children.size());

        VisitDepartment child = children.get(0);
        assertEquals(childDepartment.getId(), child.getDepartment().getId());

        assertEquals(1, visitDepartmentProductRepository.findByVisitDepartmentId(child.getId()).size());

        UUID childProductId = visitDepartmentProductRepository.findByVisitDepartmentId(child.getId()).get(0).getId();
        ApiResponse removeProductResponse = visitService.removeVisitDepartmentProduct(childProductId);
        assertEquals(ResponseStatus.SUCCESS, removeProductResponse.status());

        assertTrue(visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId()).isEmpty());
        }

        @Test
        void testAddChildVisitDepartmentRequiresProcessorIdWhenMultipleProcessors() {
        Department childDepartment = new Department();
        childDepartment.setName("Laboratory");
        childDepartment.setSupportRequests(true);
        childDepartment = departmentRepository.save(childDepartment);

        visitService.updateVisitDepartmentStatus(
            new UpdateVisitDepartmentStatusInput(visitDepartment.getId(), VisitDepartmentStatus.ACTIVE),
            new AuthenticatedUser(processorOne.getId(), processorOne.getUsername(), Set.of(), null, null)
        );

        // Add multiple processors to parent visit department
        java.util.List<Worker> processors = new java.util.ArrayList<>();
        processors.add(processorOne);
        processors.add(processorTwo);
        visitDepartment.setProcessors(processors);
        visitDepartmentRepository.save(visitDepartment);

        // Should succeed without processorId even when parent has multiple processors
        ApiResponse addChildNoProcessorResponse = visitService.addChildVisitDepartment(
            new AddChildVisitDepartmentInput(
                visitDepartment.getId(),
                childDepartment.getId(),
                java.util.List.of(new AddChildVisitDepartmentProductInput(product.getId(), 1.0)),
                null,
                null
            ),
            new AuthenticatedUser(processorOne.getId(), processorOne.getUsername(), Set.of(), null, null)
        );
        assertEquals(ResponseStatus.SUCCESS, addChildNoProcessorResponse.status());

        // Child department should have been created with products
        java.util.List<VisitDepartment> children = visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId());
        assertEquals(1, children.size());
        assertEquals(1, visitDepartmentProductRepository.findByVisitDepartmentId(children.get(0).getId()).size());

        // Should also succeed with valid processorId
        ApiResponse addChildWithProcessorResponse = visitService.addChildVisitDepartment(
            new AddChildVisitDepartmentInput(
                visitDepartment.getId(),
                childDepartment.getId(),
                java.util.List.of(new AddChildVisitDepartmentProductInput(product.getId(), 1.0)),
                processorTwo.getId(),
                null
            ),
            new AuthenticatedUser(processorOne.getId(), processorOne.getUsername(), Set.of(), null, null)
        );
        assertEquals(ResponseStatus.SUCCESS, addChildWithProcessorResponse.status());
        assertEquals(2, visitDepartmentRepository.findByParentVisitDepartmentId(visitDepartment.getId()).size());
        assertEquals(1, visitDepartmentProductRepository.findByVisitDepartmentId(children.get(0).getId()).size());
        }
}
