package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.PatientInsuranceDto;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.entity.VisitInsurance;
import com.nexxserve.nexxclinic.graphql.input.CreatePatientInsuranceInput;
import com.nexxserve.nexxclinic.graphql.input.UpdatePatientInsuranceInput;
import com.nexxserve.nexxclinic.model.Gender;
import com.nexxserve.nexxclinic.model.ResponseStatus;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import com.nexxserve.nexxclinic.model.VisitDepartmentStatus;
import com.nexxserve.nexxclinic.model.VisitStatus;
import com.nexxserve.nexxclinic.repository.DepartmentInsuranceBillingRepository;
import com.nexxserve.nexxclinic.repository.DepartmentRepository;
import com.nexxserve.nexxclinic.repository.InsuranceProviderRepository;
import com.nexxserve.nexxclinic.repository.PatientInsuranceRepository;
import com.nexxserve.nexxclinic.repository.PatientRepository;
import com.nexxserve.nexxclinic.repository.VisitBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentBillingRepository;
import com.nexxserve.nexxclinic.repository.VisitDepartmentRepository;
import com.nexxserve.nexxclinic.repository.VisitInsuranceRepository;
import com.nexxserve.nexxclinic.repository.VisitRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the patient-insurance management rules:
 * <ul>
 *   <li>one active policy per insurance provider (overlapping windows rejected),</li>
 *   <li>one card number per patient (case-insensitive),</li>
 *   <li>a used insurance can only be deactivated, never hard-deleted.</li>
 * </ul>
 */
@SpringBootTest
class PatientInsuranceServiceTest {

    @Autowired
    private PatientService patientService;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private InsuranceProviderRepository insuranceProviderRepository;

    @Autowired
    private PatientInsuranceRepository patientInsuranceRepository;

    @Autowired
    private VisitRepository visitRepository;

    @Autowired
    private VisitInsuranceRepository visitInsuranceRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private VisitDepartmentRepository visitDepartmentRepository;

    @Autowired
    private VisitBillingRepository visitBillingRepository;

    @Autowired
    private VisitDepartmentBillingRepository visitDepartmentBillingRepository;

    @Autowired
    private DepartmentInsuranceBillingRepository departmentInsuranceBillingRepository;

    private Patient persistPatient() {
        Patient patient = new Patient();
        patient.setFirstName("Ins");
        patient.setLastName("Patient");
        patient.setFullName("Ins Patient");
        patient.setDateOfBirth(LocalDate.of(1990, 1, 1));
        patient.setGender(Gender.FEMALE);
        return patientRepository.save(patient);
    }

    private InsuranceProvider persistProvider() {
        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName("TestProvider-" + UUID.randomUUID());
        provider.setDefaultPatientSharePercentage(15);
        provider.setSupportedByClinic(true);
        return insuranceProviderRepository.save(provider);
    }

    private CreatePatientInsuranceInput insuranceInput(
            UUID patientId, InsuranceProvider provider, String card, LocalDate from, LocalDate until) {
        return new CreatePatientInsuranceInput(
                patientId,
                provider.getId(),
                card,
                null,
                true,
                null,
                null,
                from,
                until
        , null);
    }

    private PatientInsuranceDto createInsurance(
            Patient patient, InsuranceProvider provider, String card, LocalDate from, LocalDate until) {
        ApiResponse<PatientInsuranceDto> response = patientService.createPatientInsurance(
                insuranceInput(patient.getId(), provider, card, from, until)
        );
        assertEquals(ResponseStatus.SUCCESS, response.status(), "expected success: " + response.message());
        assertNotNull(response.data());
        return response.data();
    }

    @Test
    void testCreateRejectsDuplicateCardNumberPerPatient() {
        Patient patient = persistPatient();
        InsuranceProvider providerA = persistProvider();
        InsuranceProvider providerB = persistProvider();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate until = LocalDate.now().plusDays(30);

        createInsurance(patient, providerA, "CARD-123", from, until);

        ApiResponse<PatientInsuranceDto> response = patientService.createPatientInsurance(
                insuranceInput(patient.getId(), providerB, "card-123", from, until)
        );
        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals(
                "An insurance with this card number already exists for this patient.",
                response.message()
        );
    }

    @Test
    void testCreateRejectsSecondActivePolicySameProvider() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate until = LocalDate.now().plusDays(30);

        createInsurance(patient, provider, "CARD-A", from, until);

        ApiResponse<PatientInsuranceDto> response = patientService.createPatientInsurance(
                insuranceInput(patient.getId(), provider, "CARD-B", from, until)
        );
        assertEquals(ResponseStatus.ERROR, response.status());
        assertTrue(
                response.message().contains("already has an active insurance from the same provider"),
                response.message()
        );
    }

    @Test
    void testCreateAllowsNonOverlappingPoliciesSameProvider() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();

        createInsurance(patient, provider, "CARD-A",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(1));

        // Second policy starts after the first expires — no overlap, allowed.
        ApiResponse<PatientInsuranceDto> response = patientService.createPatientInsurance(
                insuranceInput(patient.getId(), provider, "CARD-B",
                        LocalDate.now().plusDays(2), LocalDate.now().plusDays(30))
        );
        assertEquals(ResponseStatus.SUCCESS, response.status(), response.message());
    }

    @Test
    void testCreateAllowsSecondPolicySameProviderAfterDeactivation() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate until = LocalDate.now().plusDays(30);

        PatientInsuranceDto first = createInsurance(patient, provider, "CARD-A", from, until);
        PatientInsurance loaded = patientInsuranceRepository.findById(first.id()).get();
        loaded.setDeactivated(true);
        patientInsuranceRepository.save(loaded);

        ApiResponse<PatientInsuranceDto> response = patientService.createPatientInsurance(
                insuranceInput(patient.getId(), provider, "CARD-B", from, until)
        );
        assertEquals(ResponseStatus.SUCCESS, response.status(), response.message());
    }

    @Test
    void testDeleteHardDeletesUnusedInsurance() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();
        PatientInsuranceDto created = createInsurance(patient, provider, "CARD-X",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        ApiResponse<Boolean> response = patientService.deletePatientInsurance(created.id());
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertTrue(patientInsuranceRepository.findById(created.id()).isEmpty());
    }

    @Test
    void testDeleteDeactivatesInsuranceUsedByVisit() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();
        PatientInsuranceDto created = createInsurance(patient, provider, "CARD-Y",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.CREATED);
        visit = visitRepository.save(visit);

        VisitInsurance link = new VisitInsurance();
        link.setVisit(visit);
        link.setPatientInsurance(patientInsuranceRepository.findById(created.id()).get());
        visitInsuranceRepository.save(link);

        ApiResponse<Boolean> response = patientService.deletePatientInsurance(created.id());
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertTrue(response.message().contains("deactivated"), response.message());

        PatientInsurance insurance = patientInsuranceRepository.findById(created.id()).get();
        assertTrue(insurance.isDeactivated());
    }

    @Test
    void testUpdateRejectsCardNumberConflict() {
        Patient patient = persistPatient();
        InsuranceProvider providerA = persistProvider();
        InsuranceProvider providerB = persistProvider();
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate until = LocalDate.now().plusDays(30);

        PatientInsuranceDto first = createInsurance(patient, providerA, "CARD-ONE", from, until);
        PatientInsuranceDto second = createInsurance(patient, providerB, "CARD-TWO", from, until);

        ApiResponse<PatientInsuranceDto> response = patientService.updatePatientInsurance(
                second.id(),
                new UpdatePatientInsuranceInput(
                        null, null, "CARD-ONE", null, null, null, null, null, null
                , null)
        );
        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals(
                "An insurance with this card number already exists for this patient.",
                response.message()
        );
        // The conflicting update must not have mutated the record.
        assertFalse(patientInsuranceRepository.findById(second.id()).get()
                .getInsuranceCardNumber().equals("CARD-ONE"));
    }

    @Test
    void testUpdateRejectsMovingUsedInsuranceToAnotherPatient() {
        Patient patientA = persistPatient();
        Patient patientB = persistPatient();
        InsuranceProvider provider = persistProvider();
        PatientInsuranceDto created = createInsurance(patientA, provider, "CARD-MOVE",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        Visit visit = new Visit();
        visit.setPatient(patientA);
        visit.setStatus(VisitStatus.CREATED);
        visit = visitRepository.save(visit);

        VisitInsurance link = new VisitInsurance();
        link.setVisit(visit);
        link.setPatientInsurance(patientInsuranceRepository.findById(created.id()).get());
        visitInsuranceRepository.save(link);

        ApiResponse<PatientInsuranceDto> response = patientService.updatePatientInsurance(
                created.id(),
                new UpdatePatientInsuranceInput(
                        patientB.getId(), null, null, null, null, null, null, null, null
                , null)
        );
        assertEquals(ResponseStatus.ERROR, response.status());
        assertEquals(
                "This insurance is in use and cannot be moved to another patient.",
                response.message()
        );
        // Still owned by patient A.
        assertEquals(patientA.getId(),
                patientInsuranceRepository.findById(created.id()).get().getPatient().getId());
    }

    @Test
    void testDeleteDeactivatesInsuranceReferencedByInvoiceOnly() {
        Patient patient = persistPatient();
        InsuranceProvider provider = persistProvider();
        PatientInsuranceDto created = createInsurance(patient, provider, "CARD-INV",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        // Build a billing bucket with a generated invoice that references the
        // insurance — no visit link, no billing item.
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setStatus(VisitStatus.CREATED);
        visit = visitRepository.save(visit);

        Department department = new Department();
        department.setName("Dept-" + UUID.randomUUID());
        department = departmentRepository.save(department);

        VisitDepartment vd = new VisitDepartment();
        vd.setVisit(visit);
        vd.setDepartment(department);
        vd.setStatus(VisitDepartmentStatus.BILLING);
        vd = visitDepartmentRepository.save(vd);

        VisitBilling vb = new VisitBilling();
        vb.setVisit(visit);
        vb = visitBillingRepository.save(vb);

        VisitDepartmentBilling vdb = new VisitDepartmentBilling();
        vdb.setVisitBilling(vb);
        vdb.setVisitDepartment(vd);
        vdb.setStatus(VisitBillingStatus.PAID);
        vdb.setTotalAmount(new BigDecimal("100.00"));
        vdb.setInsuranceCoveredAmount(new BigDecimal("85.00"));
        vdb.setPatientPayableAmount(new BigDecimal("15.00"));
        vdb.setPaidAmount(new BigDecimal("15.00"));
        vdb.setOutstandingAmount(BigDecimal.ZERO);
        vdb = visitDepartmentBillingRepository.save(vdb);

        DepartmentInsuranceBilling bucket = new DepartmentInsuranceBilling();
        bucket.setVisitDepartmentBilling(vdb);
        bucket.setPatientInsurance(patientInsuranceRepository.findById(created.id()).get());
        bucket.setStatus(VisitBillingStatus.PAID);
        bucket.setTotalAmount(new BigDecimal("100.00"));
        bucket.setInsuranceCoveredAmount(new BigDecimal("85.00"));
        bucket.setPatientPayableAmount(new BigDecimal("15.00"));
        bucket.setPaidAmount(new BigDecimal("15.00"));
        bucket.setOutstandingAmount(BigDecimal.ZERO);
        bucket.setInvoiceUrl("https://storage.example/invoice.pdf");
        departmentInsuranceBillingRepository.save(bucket);

        ApiResponse<Boolean> response = patientService.deletePatientInsurance(created.id());
        assertEquals(ResponseStatus.SUCCESS, response.status());
        assertTrue(response.message().contains("deactivated"), response.message());

        PatientInsurance insurance = patientInsuranceRepository.findById(created.id()).get();
        assertTrue(insurance.isDeactivated());
    }
}
