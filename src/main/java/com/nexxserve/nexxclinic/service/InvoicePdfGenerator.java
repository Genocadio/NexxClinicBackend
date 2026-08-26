package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.ClinicProfile;
import com.nexxserve.nexxclinic.entity.DepartmentInsuranceBilling;
import com.nexxserve.nexxclinic.service.invoice.InvoicePdfRenderer;
import com.nexxserve.nexxclinic.service.invoice.InvoiceView;
import com.nexxserve.nexxclinic.service.invoice.InvoiceViewMapper;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generates invoice PDFs by rendering Thymeleaf templates via Playwright's
 * headless Chromium.
 *
 * <p>Preserves the legacy static method signature used by {@link com.nexxserve.nexxclinic.service.billing.InvoiceGenerator}
 * and {@link com.nexxserve.nexxclinic.controller.InvoicePreviewController}.
 *
 * <p>Internally delegates to:
 * <ul>
 *   <li>{@link InvoiceViewMapper} — converts entities to a flat, pre-formatted view model</li>
 *   <li>{@link InvoicePdfRenderer} — renders the chosen Thymeleaf template to PDF via Chromium</li>
 * </ul>
 *
 * <p>Paper sizes:
 * <ul>
 *   <li>{@code letter}, {@code a4p} — A4 portrait (one invoice per sheet)</li>
 *   <li>{@code a4l} — A4 landscape (two invoices per sheet via flexbox halves)</li>
 *   <li>{@code pos} — 80mm thermal receipt roll</li>
 * </ul>
 */
public final class InvoicePdfGenerator {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfGenerator.class);

    // Injected via the static accessor — set once by Spring at startup
    private static volatile InvoicePdfRenderer renderer;
    private static volatile InvoiceViewMapper mapper;

    private InvoicePdfGenerator() {}

    // ═══════════════════════════════════════════════════════════════════════════
    //  SPRING INJECTOR (called once at startup)
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Called by a Spring {@code @Configuration} or {@code @Component} to wire
     * the renderer and mapper into this static accessor.
     */
    public static void init(InvoicePdfRenderer renderer, InvoiceViewMapper mapper) {
        InvoicePdfGenerator.renderer = renderer;
        InvoicePdfGenerator.mapper = mapper;
        log.info("InvoicePdfGenerator initialized — delegating to Thymeleaf + Playwright renderer");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API (legacy signature preserved)
    // ═══════════════════════════════════════════════════════════════════════════

    public static void createInvoicePdf(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile,
            PaperSize paperSize
    ) throws IOException {
        if (renderer == null || mapper == null) {
            throw new IllegalStateException(
                "InvoicePdfGenerator.init() has not been called. " +
                "Ensure the Spring wiring bean is active."
            );
        }

        String paperKey = paperSize != null ? paperSize.key() : "letter";

        InvoiceView view = mapper.map(billing, items, clinicProfile);

        byte[] pdfBytes;
        if (paperSize == PaperSize.A4L) {
            // A4L renders a list; single-invoice wraps in a one-element list
            pdfBytes = renderer.renderA4L(List.of(view));
        } else {
            pdfBytes = renderer.renderSingle(view, paperKey);
        }

        try (OutputStream out = Files.newOutputStream(invoiceFile)) {
            out.write(pdfBytes);
        }

        log.debug("Invoice PDF written to {} ({} bytes, paper={})", invoiceFile, pdfBytes.length, paperKey);
    }

    /** Legacy overload — defaults to US Letter. */
    public static void createInvoicePdf(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile
    ) throws IOException {
        createInvoicePdf(invoiceFile, billing, items, clinicProfile, PaperSize.LETTER);
    }
}
