package com.nexxserve.nexxclinic.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitBilling;
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
    void createInvoicePdf_writesPdfUsingBundledFonts() throws Exception {
        Patient patient = new Patient();
        patient.setFirstName("Jane");
        patient.setLastName("Doe");

        Visit visit = new Visit();
        visit.setId(UUID.randomUUID());
        visit.setPatient(patient);
        visit.setVisitDate(LocalDateTime.now());

        VisitBilling billing = new VisitBilling();
        billing.setId(UUID.randomUUID());
        billing.setVisit(visit);
        billing.setStatus(VisitBillingStatus.UNPAID);
        billing.setBillingDate(LocalDateTime.now());
        billing.setTotalAmount(new BigDecimal("100.00"));
        billing.setInsuranceCoveredAmount(new BigDecimal("20.00"));
        billing.setPatientPayableAmount(new BigDecimal("80.00"));

        List<Map<String, Object>> items = List.of(
                Map.of(
                        "productName", "Consultation",
                        "quantitySnapshot", 1,
                        "unitPriceSnapshot", new BigDecimal("100.00"),
                        "lineTotal", new BigDecimal("100.00")));

        Path invoiceFile = tempDir.resolve("invoice-test.pdf");
        InvoicePdfGenerator.createInvoicePdf(invoiceFile, billing, items);

        assertTrue(Files.exists(invoiceFile));
        assertTrue(Files.size(invoiceFile) > 1000);
    }
}
