package com.nexxserve.nexxclinic.service.invoice;

import com.nexxserve.nexxclinic.entity.ClinicContact;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.Department;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.entity.VisitDepartmentBilling;
import com.nexxserve.nexxclinic.model.ClinicContactType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoiceViewMapperTest {

    private InvoiceViewMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new InvoiceViewMapper();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Patient patient(String first, String middle, String last, String full, String phone) {
        Patient p = new Patient();
        p.setFirstName(first);
        p.setMiddleName(middle);
        p.setLastName(last);
        p.setFullName(full);
        p.setPrimaryPhoneNumber(phone);
        return p;
    }

    private Visit visit(Patient patient, LocalDateTime visitDate) {
        Visit v = new Visit();
        v.setId(UUID.randomUUID());
        v.setPatient(patient);
        v.setVisitDate(visitDate);
        return v;
    }

    private VisitBilling visitBilling(Visit visit, LocalDateTime createdAt) {
        VisitBilling vb = new VisitBilling();
        vb.setId(UUID.randomUUID());
        vb.setVisit(visit);
        vb.setCreatedAt(createdAt);
        return vb;
    }

    private VisitDepartment visitDepartment(String deptName) {
        Department dept = new Department();
        dept.setId(UUID.randomUUID());
        dept.setName(deptName);
        VisitDepartment vd = new VisitDepartment();
        vd.setId(UUID.randomUUID());
        vd.setDepartment(dept);
        return vd;
    }

    private DepartmentInsuranceBilling billing(
            VisitBilling vb, VisitDepartment vd, PatientInsurance pi,
            VisitBillingStatus status, String total, String ins, String patient, String paid, String outstanding
    ) {
        VisitDepartmentBilling vdb = new VisitDepartmentBilling();
        vdb.setId(UUID.randomUUID());
        vdb.setVisitBilling(vb);
        vdb.setVisitDepartment(vd);

        DepartmentInsuranceBilling b = new DepartmentInsuranceBilling();
        b.setId(UUID.randomUUID());
        b.setVisitDepartmentBilling(vdb);
        b.setPatientInsurance(pi);
        b.setStatus(status);
        b.setTotalAmount(new BigDecimal(total));
        b.setInsuranceCoveredAmount(new BigDecimal(ins));
        b.setPatientPayableAmount(new BigDecimal(patient));
        b.setPaidAmount(new BigDecimal(paid));
        b.setOutstandingAmount(new BigDecimal(outstanding));
        return b;
    }

    private Map<String, Object> item(String name, String qty, String unitPrice, String ins, String patient, String total) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productName", name);
        m.put("quantitySnapshot", new BigDecimal(qty));
        m.put("unitPriceSnapshot", new BigDecimal(unitPrice));
        m.put("insuranceCoveredAmount", new BigDecimal(ins));
        m.put("patientPayableAmount", new BigDecimal(patient));
        m.put("lineTotal", new BigDecimal(total));
        return m;
    }

    private ClinicProfile clinic(String name, String address, String tin, ClinicContact... contacts) {
        ClinicProfile cp = new ClinicProfile();
        cp.setName(name);
        cp.setAddress(address);
        cp.setTinNumber(tin);
        cp.setContacts(List.of(contacts));
        return cp;
    }

    private ClinicContact contact(ClinicContactType type, String value) {
        ClinicContact c = new ClinicContact();
        c.setContactType(type);
        c.setValue(value);
        return c;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Clinic mapping
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void clinicMapsPhoneAndEmailSeparately() {
        ClinicProfile cp = clinic("NexxMed", "KN 5 Rd", "RW123",
            contact(ClinicContactType.PHONE, "+250 788 123"),
            contact(ClinicContactType.EMAIL, "billing@nexxmed.rw"),
            contact(ClinicContactType.POBOX, "P.O. Box 1234")
        );

        Visit v = visit(patient("Jane", null, "Doe", "Jane Doe", null), LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "100", "0", "100", "100", "0");

        InvoiceView view = mapper.map(b, List.of(), cp);

        assertEquals("NexxMed", view.clinic().name());
        assertEquals("KN 5 Rd", view.clinic().address());
        assertEquals("+250 788 123", view.clinic().phone());
        assertEquals("billing@nexxmed.rw", view.clinic().email());
        assertEquals(1, view.clinic().otherContacts().size());
        assertEquals("P.O. Box", view.clinic().otherContacts().get(0).label());
        assertEquals("P.O. Box 1234", view.clinic().otherContacts().get(0).value());
        assertEquals("RW123", view.clinic().tinNumber());
    }

    @Test
    void clinicNullDefaults() {
        Visit v = visit(patient("Jane", null, "Doe", "Jane Doe", null), LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "100", "0", "100", "100", "0");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("Medical Clinic", view.clinic().name());
        assertNull(view.clinic().address());
        assertNull(view.clinic().phone());
        assertNull(view.clinic().email());
        assertTrue(view.clinic().otherContacts().isEmpty());
        assertNull(view.clinic().tinNumber());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Patient name formatting
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void patientName_usesFullName() {
        Patient p = patient("Jean", "Baptiste", "Uwimana", "Jean Baptiste Uwimana", "+250 791 234");
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "100", "0", "100", "100", "0");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("Jean Baptiste Uwimana", view.patientName());
        assertEquals("+250 791 234", view.patientPhone());
    }

    @Test
    void patientName_fallbackToParts_whenFullNameBlank() {
        Patient p = patient("Jane", "Marie", "Doe", "  ", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Lab");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.UNPAID, "50", "0", "50", "0", "50");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("Jane Marie Doe", view.patientName());
    }

    @Test
    void patientName_unknown_whenAllNull() {
        Patient p = patient(null, null, null, null, null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Lab");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.UNPAID, "50", "0", "50", "0", "50");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("Unknown", view.patientName());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Insurance line
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void insuranceLine_providerNameAndCard() {
        InsuranceProvider prov = new InsuranceProvider();
        prov.setInsuranceName("Santé Plus");
        PatientInsurance pi = new PatientInsurance();
        pi.setInsuranceProvider(prov);
        pi.setInsuranceCardNumber("SPR-12345");

        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, pi, VisitBillingStatus.PAID, "100", "80", "20", "20", "0");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("Santé Plus \u2014 SPR-12345", view.insuranceLine());
    }

    @Test
    void insuranceLine_null_whenNoInsurance() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "100", "0", "100", "100", "0");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertNull(view.insuranceLine());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Status
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void statusLabels() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");

        DepartmentInsuranceBilling bPaid = billing(vb, vd, null, VisitBillingStatus.PAID, "100", "0", "100", "100", "0");
        assertEquals("PAID", mapper.map(bPaid, List.of(), null).statusLabel());

        DepartmentInsuranceBilling bPartial = billing(vb, vd, null, VisitBillingStatus.PARTIALLY_PAID, "100", "50", "50", "30", "20");
        assertEquals("PARTIALLY PAID", mapper.map(bPartial, List.of(), null).statusLabel());

        DepartmentInsuranceBilling bUnpaid = billing(vb, vd, null, VisitBillingStatus.UNPAID, "100", "0", "100", "0", "100");
        assertEquals("UNPAID", mapper.map(bUnpaid, List.of(), null).statusLabel());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Items mapping
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void itemsMappedCorrectly() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "150", "30", "120", "120", "0");

        List<Map<String, Object>> items = List.of(
            item("X-Ray", "1", "100.00", "20.00", "80.00", "100.00"),
            item("Consultation", "2", "25.00", "10.00", "40.00", "50.00")
        );

        InvoiceView view = mapper.map(b, items, null);

        assertEquals(2, view.items().size());
        assertEquals("X-Ray", view.items().get(0).productName());
        assertEquals("1", view.items().get(0).qty());
        assertEquals("100.00", view.items().get(0).unitPrice());
        assertEquals("20.00", view.items().get(0).insuranceCovered());
        assertEquals("80.00", view.items().get(0).patientPayable());
        assertEquals("100.00", view.items().get(0).lineTotal());

        assertEquals("Consultation", view.items().get(1).productName());
        assertEquals("2", view.items().get(1).qty());
    }

    @Test
    void emptyItems() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID, "0", "0", "0", "0", "0");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertTrue(view.items().isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Totals formatting
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void totalsFormatted() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PARTIALLY_PAID,
            "185.00", "148.00", "37.00", "20.00", "17.00");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertEquals("185.00", view.totalAmount());
        assertEquals("148.00", view.insuranceCoveredAmount());
        assertEquals("37.00", view.patientPayableAmount());
        assertEquals("20.00", view.paidAmount());
        assertEquals("17.00", view.outstandingAmount());
        assertTrue(view.hasOutstanding());
    }

    @Test
    void zeroOutstanding_hasOutstandingFalse() {
        Patient p = patient("Jane", null, "Doe", "Jane Doe", null);
        Visit v = visit(p, LocalDateTime.now());
        VisitBilling vb = visitBilling(v, LocalDateTime.now());
        VisitDepartment vd = visitDepartment("Radiology");
        DepartmentInsuranceBilling b = billing(vb, vd, null, VisitBillingStatus.PAID,
            "100.00", "0.00", "100.00", "100.00", "0.00");

        InvoiceView view = mapper.map(b, List.of(), null);

        assertFalse(view.hasOutstanding());
        assertEquals("0.00", view.outstandingAmount());
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Formatting helpers
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    void fmtMoneyHandlesNull() {
        assertEquals("0.00", InvoiceViewMapper.fmtMoney(null));
    }

    @Test
    void fmtMoneyHandlesBigDecimal() {
        assertEquals("123.46", InvoiceViewMapper.fmtMoney(new BigDecimal("123.456")));
    }

    @Test
    void fmtMoneyHandlesString() {
        assertEquals("99.99", InvoiceViewMapper.fmtMoney("99.99"));
    }

    @Test
    void fmtMoneyHandlesInteger() {
        assertEquals("50", InvoiceViewMapper.fmtMoney(50));
    }
}
