package com.nexxserve.nexxclinic.service.invoice;

import com.nexxserve.nexxclinic.entity.ClinicContact;
import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.entity.InsuranceProvider;
import com.nexxserve.nexxclinic.entity.Patient;
import com.nexxserve.nexxclinic.entity.PatientInsurance;
import com.nexxserve.nexxclinic.entity.Visit;
import com.nexxserve.nexxclinic.entity.VisitDepartment;
import com.nexxserve.nexxclinic.model.ClinicContactType;
import com.nexxserve.nexxclinic.model.VisitBillingStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Converts the existing entity graph into a flat {@link InvoiceView} for
 * Thymeleaf consumption. Formatting logic is ported from the old PDFBox
 * {@code InvoicePdfGenerator}.
 */
@Component
public class InvoiceViewMapper {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    //  PUBLIC API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Map a single insurance billing row into an InvoiceView with default copy type.
     */
    public InvoiceView map(
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile
    ) {
        return map(billing, items, clinicProfile, "DEFAULT");
    }

    /**
     * Map a single insurance billing row into an InvoiceView with a specific copy type.
     *
     * @param billing       the billing entity (all associations eagerly loaded)
     * @param items         the item maps from {@link com.nexxserve.nexxclinic.service.billing.BillingDataMapper#visitBillingItemToMap}
     * @param clinicProfile the clinic profile (may be null)
     * @param copyType      "INSURANCE", "PATIENT", or "DEFAULT" (null defaults to "DEFAULT")
     */
    public InvoiceView map(
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile,
            String copyType
    ) {
        Visit visit = resolveVisit(billing);
        Patient pat = visit == null ? null : visit.getPatient();
        VisitDepartment vd = billing.getVisitDepartmentBilling() == null
                ? null : billing.getVisitDepartmentBilling().getVisitDepartment();

        String insuranceLine = buildInsuranceLine(billing.getPatientInsurance());
        boolean hasInsurance = insuranceLine != null || hasNonZeroInsuranceCovered(billing.getInsuranceCoveredAmount());

        String normCopyType = copyType != null && !copyType.isBlank() ? copyType.trim().toUpperCase() : "DEFAULT";

        String invoiceTitle;
        String copySubtitle;
        String statusLabel;
        String patientPayableStr;
        String paidAmountStr;
        String outstandingStr;
        boolean hasOutstanding;

        if ("INSURANCE".equals(normCopyType)) {
            invoiceTitle = "INSURANCE CLAIM INVOICE";
            copySubtitle = "Insurer Copy";
            statusLabel = "CLAIMABLE";
            BigDecimal total = billing.getTotalAmount() != null ? billing.getTotalAmount() : BigDecimal.ZERO;
            BigDecimal ins = billing.getInsuranceCoveredAmount() != null ? billing.getInsuranceCoveredAmount() : BigDecimal.ZERO;
            patientPayableStr = fmtMoney(total.subtract(ins).max(BigDecimal.ZERO));
            paidAmountStr = "0.00";
            outstandingStr = "0.00";
            hasOutstanding = false;
        } else if ("PATIENT".equals(normCopyType)) {
            invoiceTitle = "PATIENT INVOICE & RECEIPT";
            copySubtitle = "Patient Copy & Statement";
            statusLabel = statusLabel(billing.getStatus());
            patientPayableStr = fmtMoney(billing.getPatientPayableAmount());
            paidAmountStr = fmtMoney(billing.getPaidAmount());
            outstandingStr = fmtMoney(billing.getOutstandingAmount());
            hasOutstanding = hasOutstanding(billing.getOutstandingAmount());
        } else {
            invoiceTitle = hasInsurance ? "INVOICE" : "INVOICE & RECEIPT";
            copySubtitle = null;
            statusLabel = statusLabel(billing.getStatus());
            patientPayableStr = fmtMoney(billing.getPatientPayableAmount());
            paidAmountStr = fmtMoney(billing.getPaidAmount());
            outstandingStr = fmtMoney(billing.getOutstandingAmount());
            hasOutstanding = hasOutstanding(billing.getOutstandingAmount());
        }

        return new InvoiceView(
            mapClinic(clinicProfile),
            formatPatientName(pat),
            pat != null ? pat.getPrimaryPhoneNumber() : null,
            // Encounter date of the billed visit department — the invoice belongs
            // to this department, not to the visit as a whole. Fall back to the visit
            // date when the department never recorded an encounter start.
            vd != null && vd.getStartedAt() != null
                    ? DATE_FMT.format(vd.getStartedAt())
                    : visit != null && visit.getVisitDate() != null
                            ? DATE_FMT.format(visit.getVisitDate()) : null,
            vd != null && vd.getDepartment() != null
                    ? vd.getDepartment().getName() : null,
            insuranceLine,
            abbrevId(billing.getId()),
            resolveInvoiceDate(billing),
            billing.getStatus() != null ? billing.getStatus().name() : "UNKNOWN",
            statusLabel,
            mapItems(items, normCopyType),
            fmtMoney(billing.getTotalAmount()),
            fmtMoney(billing.getInsuranceCoveredAmount()),
            patientPayableStr,
            paidAmountStr,
            outstandingStr,
            hasOutstanding,
            hasInsurance,
            invoiceTitle,
            normCopyType,
            copySubtitle
        );
    }

    /**
     * Convenience overload using the legacy 4-arg signature shape.
     */
    public InvoiceView map(
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile,
            @SuppressWarnings("unused") Object paperSizeIgnored
    ) {
        return map(billing, items, clinicProfile, "DEFAULT");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CLINIC
    // ─────────────────────────────────────────────────────────────────────────

    private InvoiceView.ClinicInfo mapClinic(ClinicProfile p) {
        if (p == null) {
            return new InvoiceView.ClinicInfo("Medical Clinic", null, null, null, List.of(), null);
        }
        // Extract phone and email as top-level fields for the template
        String phone = null;
        String email = null;
        List<InvoiceView.ContactInfo> otherContacts = new ArrayList<>();
        if (p.getContacts() != null) {
            for (ClinicContact c : p.getContacts()) {
                if (c == null || c.getValue() == null || c.getValue().isBlank()) continue;
                String label = contactLabel(c.getContactType());
                if (label == null) continue;
                if (c.getContactType() == ClinicContactType.PHONE) {
                    phone = c.getValue().trim();
                } else if (c.getContactType() == ClinicContactType.EMAIL) {
                    email = c.getValue().trim();
                } else {
                    otherContacts.add(new InvoiceView.ContactInfo(label, c.getValue().trim()));
                }
            }
        }
        return new InvoiceView.ClinicInfo(
            p.getName() != null && !p.getName().isBlank() ? p.getName().trim() : "Medical Clinic",
            p.getAddress() != null && !p.getAddress().isBlank() ? p.getAddress().trim() : null,
            phone,
            email,
            otherContacts,
            p.getTinNumber() != null && !p.getTinNumber().isBlank() ? p.getTinNumber().trim() : null
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PATIENT / INSURANCE
    // ─────────────────────────────────────────────────────────────────────────

    private String formatPatientName(Patient p) {
        if (p == null) return "Unknown";
        if (p.getFullName() != null && !p.getFullName().isBlank()) return p.getFullName().trim();
        String first  = p.getFirstName()  == null ? "" : p.getFirstName().trim();
        String middle = p.getMiddleName() == null ? "" : p.getMiddleName().trim();
        String last   = p.getLastName()   == null ? "" : p.getLastName().trim();
        String full   = (first + " " + middle + " " + last).replaceAll("\\s+", " ").trim();
        return full.isEmpty() ? "Unknown" : full;
    }

    private String buildInsuranceLine(PatientInsurance ins) {
        if (ins == null) return null;
        InsuranceProvider prov = ins.getInsuranceProvider();
        String name = (prov != null && prov.getInsuranceName() != null && !prov.getInsuranceName().isBlank())
                ? prov.getInsuranceName().trim()
                : (prov != null && prov.getAcronym() != null && !prov.getAcronym().isBlank())
                    ? prov.getAcronym().trim() : null;
        String card = ins.getInsuranceCardNumber() != null && !ins.getInsuranceCardNumber().isBlank()
                ? ins.getInsuranceCardNumber().trim() : null;
        if (name == null && card == null) return null;
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name);
        if (card != null) { if (!sb.isEmpty()) sb.append(" \u2014 "); sb.append(card); }
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INVOICE DETAILS
    // ─────────────────────────────────────────────────────────────────────────

    private String resolveInvoiceDate(DepartmentInsuranceBilling billing) {
        // Use the explicit billingDate if set (admin/manager override),
        // otherwise fall back to the VisitBilling creation timestamp.
        LocalDateTime dt = billing.getBillingDate();
        if (dt == null && billing.getVisitDepartmentBilling() != null
                && billing.getVisitDepartmentBilling().getVisitBilling() != null) {
            dt = billing.getVisitDepartmentBilling().getVisitBilling().getCreatedAt();
        }
        return dt != null ? DT_FMT.format(dt) : null;
    }

    private String abbrevId(java.util.UUID id) {
        if (id == null) return "-";
        String s = id.toString().toUpperCase().replaceAll("-", "");
        return s.length() > 16 ? "..." + s.substring(s.length() - 13) : s;
    }

    private String statusLabel(VisitBillingStatus s) {
        if (s == null) return "UNKNOWN";
        return switch (s) {
            case PAID           -> "PAID";
            case PARTIALLY_PAID -> "PARTIALLY PAID";
            case UNPAID         -> "UNPAID";
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ITEMS
    // ─────────────────────────────────────────────────────────────────────────

    private List<InvoiceView.InvoiceItem> mapItems(List<Map<String, Object>> items, String copyType) {
        if (items == null) return List.of();
        boolean isInsurance = "INSURANCE".equalsIgnoreCase(copyType);
        List<InvoiceView.InvoiceItem> result = new ArrayList<>(items.size());
        for (Map<String, Object> item : items) {
            BigDecimal lineTotal = toBigDecimal(item.get("lineTotal"));
            BigDecimal insCovered = toBigDecimal(item.get("insuranceCoveredAmount"));
            BigDecimal patientPayable = toBigDecimal(item.get("patientPayableAmount"));

            // For insurance claims, patient co-pay is lineTotal - insCovered (standard tariff split)
            String patientPayableStr = isInsurance
                ? fmtMoney(lineTotal.subtract(insCovered).max(BigDecimal.ZERO))
                : fmtMoney(patientPayable);

            result.add(new InvoiceView.InvoiceItem(
                str(item.get("productName")),
                fmtQty(item.get("quantitySnapshot")),
                fmtMoney(item.get("unitPriceSnapshot")),
                fmtMoney(insCovered),
                patientPayableStr,
                fmtMoney(lineTotal)
            ));
        }
        return result;
    }

    private static BigDecimal toBigDecimal(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal d) return d;
        try {
            return new BigDecimal(String.valueOf(val));
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FORMATTING HELPERS (ported from InvoicePdfGenerator)
    // ─────────────────────────────────────────────────────────────────────────

    static String fmtMoney(Object val) {
        if (val == null) return "0.00";
        if (val instanceof BigDecimal d) return d.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return String.valueOf(val);
    }

    private String fmtQty(Object val) {
        if (val == null) return "0";
        if (val instanceof BigDecimal d) return d.stripTrailingZeros().toPlainString();
        return String.valueOf(val);
    }

    private boolean hasOutstanding(BigDecimal outstanding) {
        return outstanding != null && outstanding.compareTo(BigDecimal.ZERO) > 0;
    }

    private boolean hasNonZeroInsuranceCovered(BigDecimal amount) {
        return amount != null && amount.compareTo(BigDecimal.ZERO) > 0;
    }

    private String str(Object o) {
        return o != null ? String.valueOf(o) : "";
    }

    private String contactLabel(ClinicContactType t) {
        if (t == null) return null;
        return switch (t) {
            case PHONE -> "Phone";
            case EMAIL -> "Email";
            case POBOX -> "P.O. Box";
        };
    }

    private Visit resolveVisit(DepartmentInsuranceBilling billing) {
        if (billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null) return null;
        return billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
    }
}
