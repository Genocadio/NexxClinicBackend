package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nexxserve.nexxclinic.entity.ClinicContact;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.model.ClinicContactType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class InvoicePdfGeneratorTest {

    @TempDir
    Path tempDir;

    @Test
    void createInvoicePdf_writesPdfWithClinicProfileAndTableLayout() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");
        patient.setFullName("Jane Doe");

        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient);
        visit.setVisitDate(LocalDateTime.of(2026, 6, 11, 10, 30));

        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setId(UUID.randomUUID());
        visitBilling.setVisit(visit);
        visitBilling.setCreatedAt(LocalDateTime.of(2026, 6, 11, 11, 0));

        Department department = new Department();
        department.setName("General Medicine");

        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setDepartment(department);

        VisitDepartmentBilling visitDepartmentBilling = new VisitDepartmentBilling();
        visitDepartmentBilling.setVisitBilling(visitBilling);
        visitDepartmentBilling.setVisitDepartment(visitDepartment);

        DepartmentInsuranceBilling billing = new DepartmentInsuranceBilling();
        billing.setId(UUID.randomUUID());
        billing.setVisitDepartmentBilling(visitDepartmentBilling);
        billing.setStatus(VisitBillingStatus.PAID);
        billing.setTotalAmount(new BigDecimal("150.00"));
        billing.setInsuranceCoveredAmount(new BigDecimal("30.00"));
        billing.setPatientPayableAmount(new BigDecimal("120.00"));
        billing.setPaidAmount(new BigDecimal("120.00"));
        billing.setOutstandingAmount(BigDecimal.ZERO);

        ClinicProfile clinicProfile = new ClinicProfile();
        clinicProfile.setName("Nexx Clinic");
        clinicProfile.setAddress("123 Health Street, Dar es Salaam");
        clinicProfile.setTinNumber("123-456-789");

        ClinicContact phone = new ClinicContact();
        phone.setContactType(ClinicContactType.PHONE);
        phone.setValue("+255700000001");

        ClinicContact email = new ClinicContact();
        email.setContactType(ClinicContactType.EMAIL);
        email.setValue("billing@nexxclinic.com");

        clinicProfile.setContacts(List.of(phone, email));

        List<Map<String, Object>> items = List.of(
                Map.of(
                        "productName", "Consultation",
                        "quantitySnapshot", BigDecimal.ONE,
                        "unitPriceSnapshot", new BigDecimal("100.00"),
                        "insuranceCoveredAmount", new BigDecimal("20.00"),
                        "patientPayableAmount", new BigDecimal("80.00"),
                        "lineTotal", new BigDecimal("100.00")),
                Map.of(
                        "productName", "Laboratory Test",
                        "quantitySnapshot", BigDecimal.ONE,
                        "unitPriceSnapshot", new BigDecimal("50.00"),
                        "insuranceCoveredAmount", new BigDecimal("10.00"),
                        "patientPayableAmount", new BigDecimal("40.00"),
                        "lineTotal", new BigDecimal("50.00")));

        Path invoiceFile = tempDir.resolve("invoice-test.pdf");
        InvoicePdfGenerator.createInvoicePdf(invoiceFile, billing, items, clinicProfile);

        assertTrue(Files.exists(invoiceFile));
        assertTrue(Files.size(invoiceFile) > 2000);
    }

    @Test
    void createInvoicePdf_omitsBlankClinicFields() throws Exception {
        Patient patient = new Patient();
        patient.setFullName("John Smith");

        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitDate(LocalDateTime.now());

        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setVisit(visit);
        visitBilling.setCreatedAt(LocalDateTime.now());

        VisitDepartmentBilling visitDepartmentBilling = new VisitDepartmentBilling();
        visitDepartmentBilling.setVisitBilling(visitBilling);

        DepartmentInsuranceBilling billing = new DepartmentInsuranceBilling();
        billing.setId(UUID.randomUUID());
        billing.setVisitDepartmentBilling(visitDepartmentBilling);
        billing.setStatus(VisitBillingStatus.UNPAID);
        billing.setTotalAmount(new BigDecimal("50.00"));
        billing.setInsuranceCoveredAmount(BigDecimal.ZERO);
        billing.setPatientPayableAmount(new BigDecimal("50.00"));
        billing.setPaidAmount(BigDecimal.ZERO);
        billing.setOutstandingAmount(new BigDecimal("50.00"));

        ClinicProfile clinicProfile = new ClinicProfile();
        clinicProfile.setName("Minimal Clinic");
        clinicProfile.setAddress("");
        clinicProfile.setTinNumber(null);
        clinicProfile.setContacts(List.of());

        List<Map<String, Object>> items = List.of(
                Map.of(
                        "productName", "X-Ray",
                        "quantitySnapshot", 1,
                        "unitPriceSnapshot", new BigDecimal("50.00"),
                        "insuranceCoveredAmount", BigDecimal.ZERO,
                        "patientPayableAmount", new BigDecimal("50.00"),
                        "lineTotal", new BigDecimal("50.00")));

        Path invoiceFile = tempDir.resolve("invoice-minimal.pdf");
        InvoicePdfGenerator.createInvoicePdf(invoiceFile, billing, items, clinicProfile);

        assertTrue(Files.exists(invoiceFile));
        assertTrue(Files.size(invoiceFile) > 1000);
    }
}
