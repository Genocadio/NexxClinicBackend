package com.nexxserve.nexxclinic.controller;

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
import com.nexxserve.nexxclinic.service.InvoicePdfGenerator;
import com.nexxserve.nexxclinic.service.PaperSize;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dev-only endpoint that generates sample invoice PDFs for visual layout
 * verification across all paper sizes.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>{@code GET /api/invoices/preview/{size}} — single paper size (letter, a4p, a4l, pos)</li>
 *   <li>{@code GET /api/invoices/preview/all} — all 4 sizes bundled in a ZIP</li>
 * </ul>
 *
 * <p><b>NOT for production</b> — the mock data is hardcoded and the endpoints
 * require no authentication.</p>
 */
@RestController
@RequestMapping("/api/invoices/preview")
public class InvoicePreviewController {

    private static final Logger log = LoggerFactory.getLogger(InvoicePreviewController.class);

    // ─── Single paper-size preview ────────────────────────────────────────────

    @GetMapping("/{size}")
    public ResponseEntity<byte[]> previewSingle(@PathVariable String size) {
        PaperSize ps = PaperSize.from(size);
        try {
            byte[] pdf = generatePreviewPdf(ps);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"invoice-preview-" + ps.key() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
        } catch (Exception e) {
            log.error("Invoice preview generation failed for size={}: {}", ps.key(), e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.TEXT_PLAIN)
                .body(("Preview generation failed: " + e.getMessage()).getBytes());
        }
    }

    // ─── All paper sizes in a ZIP ─────────────────────────────────────────────

    @GetMapping("/all")
    public ResponseEntity<byte[]> previewAll() {
        try {
            Path zipFile = Files.createTempFile("invoice-previews-", ".zip");
            try {
                // Use java.util.zip for zero extra dependencies
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(
                        Files.newOutputStream(zipFile))) {
                    for (PaperSize ps : PaperSize.values()) {
                        byte[] pdf = generatePreviewPdf(ps);
                        java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry(
                            "invoice-preview-" + ps.key() + ".pdf");
                        zos.putNextEntry(entry);
                        zos.write(pdf);
                        zos.closeEntry();
                    }
                }
                byte[] zipBytes = Files.readAllBytes(zipFile);
                return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"invoice-previews-all.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(zipBytes.length)
                    .body(zipBytes);
            } finally {
                Files.deleteIfExists(zipFile);
            }
        } catch (Exception e) {
            log.error("Invoice preview ZIP generation failed: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                .contentType(MediaType.TEXT_PLAIN)
                .body(("Preview generation failed: " + e.getMessage()).getBytes());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  MOCK DATA BUILDER
    // ═══════════════════════════════════════════════════════════════════════════

    private static byte[] generatePreviewPdf(PaperSize paperSize) throws IOException {
        // ── Clinic ────────────────────────────────────────────────────────────
        ClinicProfile clinic = new ClinicProfile();
        clinic.setName("NexxMed Medical Center");
        clinic.setAddress("KN 5 Rd, Kigali, Rwanda — P.O. Box 1234");
        clinic.setTinNumber("RW12345678");
        ClinicContact phone = new ClinicContact();
        phone.setContactType(ClinicContactType.PHONE);
        phone.setValue("+250 788 123 456");
        ClinicContact email = new ClinicContact();
        email.setContactType(ClinicContactType.EMAIL);
        email.setValue("billing@nexxmed.rw");
        clinic.setContacts(List.of(phone, email));

        // ── Patient ───────────────────────────────────────────────────────────
        Patient patient = new Patient();
        patient.setFirstName("Jean");
        patient.setMiddleName("Baptiste");
        patient.setLastName("Uwimana");
        patient.setFullName("Jean Baptiste Uwimana");
        patient.setPrimaryPhoneNumber("+250 791 234 567");

        // ── Insurance ─────────────────────────────────────────────────────────
        InsuranceProvider provider = new InsuranceProvider();
        provider.setInsuranceName("Santé Plus Rwanda");
        provider.setAcronym("SPR");

        PatientInsurance patientInsurance = new PatientInsurance();
        patientInsurance.setInsuranceProvider(provider);
        patientInsurance.setInsuranceCardNumber("SPR-2024-98765");
        patientInsurance.setPatientSharePercentage(20);

        // ── Visit ─────────────────────────────────────────────────────────────
        Visit visit = new Visit();
        visit.setPatient(patient);
        visit.setVisitDate(LocalDateTime.of(2026, 8, 25, 10, 0));

        // ── VisitBilling (container) ──────────────────────────────────────────
        VisitBilling visitBilling = new VisitBilling();
        visitBilling.setVisit(visit);
        visitBilling.setCreatedAt(LocalDateTime.of(2026, 8, 25, 14, 30));

        // ── Department ────────────────────────────────────────────────────────
        Department department = new Department();
        department.setName("Radiology & Imaging");

        VisitDepartment visitDepartment = new VisitDepartment();
        visitDepartment.setDepartment(department);

        // ── VisitDepartmentBilling ────────────────────────────────────────────
        VisitDepartmentBilling vdb = new VisitDepartmentBilling();
        vdb.setVisitBilling(visitBilling);
        vdb.setVisitDepartment(visitDepartment);

        // ── DepartmentInsuranceBilling ────────────────────────────────────────
        DepartmentInsuranceBilling billing = new DepartmentInsuranceBilling();
        billing.setId(UUID.randomUUID());
        billing.setVisitDepartmentBilling(vdb);
        billing.setPatientInsurance(patientInsurance);
        billing.setStatus(VisitBillingStatus.PARTIALLY_PAID);
        billing.setTotalAmount(new BigDecimal("185.00"));
        billing.setInsuranceCoveredAmount(new BigDecimal("148.00"));
        billing.setPatientPayableAmount(new BigDecimal("37.00"));
        billing.setPaidAmount(new BigDecimal("20.00"));
        billing.setOutstandingAmount(new BigDecimal("17.00"));

        // ── Items ─────────────────────────────────────────────────────────────
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(buildItem("X-Ray Chest (PA View)", 1, 45.00, 36.00, 9.00));
        items.add(buildItem("CT Scan Abdomen with Contrast", 1, 85.00, 68.00, 17.00));
        items.add(buildItem("Ultrasound — Abdominal", 1, 35.00, 28.00, 7.00));
        items.add(buildItem("MRI Brain (with/without contrast)", 1, 120.00, 96.00, 24.00));
        items.add(buildItem("Mammography Screening", 1, 55.00, 44.00, 11.00));
        items.add(buildItem("DEXA Bone Density Scan", 1, 60.00, 48.00, 12.00));
        items.add(buildItem("Fluoroscopy — Barium Swallow", 1, 40.00, 32.00, 8.00));
        items.add(buildItem("Nuclear Medicine — Thyroid Scan", 1, 75.00, 60.00, 15.00));
        items.add(buildItem("Echocardiogram (2D Echo)", 1, 65.00, 52.00, 13.00));
        items.add(buildItem("PET-CT Full Body Scan", 1, 250.00, 200.00, 50.00));

        // Recalculate totals to match items
        BigDecimal total = items.stream()
            .map(i -> (BigDecimal) i.get("lineTotal"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal insCovered = items.stream()
            .map(i -> (BigDecimal) i.get("insuranceCoveredAmount"))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal patientPay = total.subtract(insCovered);

        billing.setTotalAmount(total);
        billing.setInsuranceCoveredAmount(insCovered);
        billing.setPatientPayableAmount(patientPay);
        billing.setPaidAmount(new BigDecimal("50.00"));
        billing.setOutstandingAmount(patientPay.subtract(new BigDecimal("50.00")));

        // ── Render ────────────────────────────────────────────────────────────
        Path tempFile = Files.createTempFile("preview-" + paperSize.key(), ".pdf");
        try {
            InvoicePdfGenerator.createInvoicePdf(tempFile, billing, items, clinic, paperSize);
            return Files.readAllBytes(tempFile);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private static Map<String, Object> buildItem(
            String name, int qty, double unitPrice,
            double insCovered, double patientPay) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("productName", name);
        m.put("quantitySnapshot", BigDecimal.valueOf(qty));
        m.put("unitPriceSnapshot", BigDecimal.valueOf(unitPrice));
        m.put("insuranceCoveredAmount", BigDecimal.valueOf(insCovered));
        m.put("patientPayableAmount", BigDecimal.valueOf(patientPay));
        m.put("lineTotal", BigDecimal.valueOf(unitPrice));
        return m;
    }
}
