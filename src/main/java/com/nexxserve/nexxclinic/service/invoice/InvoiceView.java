package com.nexxserve.nexxclinic.service.invoice;

import java.util.List;

/**
 * Flat, pre-formatted invoice view model for Thymeleaf template consumption.
 * <p>
 * All values are resolved and formatted — no lazy-loaded JPA entities are passed
 * into the template. The {@link InvoiceViewMapper} builds this from the existing
 * {@code DepartmentInsuranceBilling} / {@code ClinicProfile} entity graph.
 *
 * @param clinic            clinic header info
 * @param patientName       resolved patient full name
 * @param patientPhone      patient primary phone (may be null)
 * @param visitDate         formatted visit date (may be null)
 * @param departmentName    department name (may be null)
 * @param insuranceLine     formatted insurance line, e.g. "Santé Plus — SPR-2024-98765" (may be null)
 * @param invoiceNo         abbreviated billing UUID
 * @param invoiceDate       formatted billing date (may be null)
 * @param status            raw enum name: PAID / PARTIALLY_PAID / UNPAID
 * @param statusLabel       human-readable: "PAID" / "PARTIALLY PAID" / "UNPAID"
 * @param items             pre-formatted line items
 * @param totalAmount       formatted total
 * @param insuranceCoveredAmount  formatted (may be "0.00")
 * @param patientPayableAmount    formatted
 * @param paidAmount              formatted
 * @param outstandingAmount       formatted
 * @param hasOutstanding          true if outstanding > 0
 */
public record InvoiceView(
    ClinicInfo clinic,
    String patientName,
    String patientPhone,
    String visitDate,
    String departmentName,
    String insuranceLine,
    String invoiceNo,
    String invoiceDate,
    String status,
    String statusLabel,
    List<InvoiceItem> items,
    String totalAmount,
    String insuranceCoveredAmount,
    String patientPayableAmount,
    String paidAmount,
    String outstandingAmount,
    boolean hasOutstanding
) {

    /** Clinic header data — also a flat record for the template. */
    public record ClinicInfo(
        String name,
        String address,
        String phone,
        String email,
        List<ContactInfo> otherContacts,
        String tinNumber
    ) {}

    /** A single contact line (label + value). */
    public record ContactInfo(String label, String value) {}

    /** A single line item row. */
    public record InvoiceItem(
        String productName,
        String qty,
        String unitPrice,
        String insuranceCovered,
        String patientPayable,
        String lineTotal
    ) {}
}
