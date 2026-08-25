package com.nexxserve.nexxclinic.service;

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
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;

/**
 * Generates a professionally structured invoice PDF with configurable paper size.
 *
 * <p>Paper sizes:
 * <ul>
 *   <li>{@code letter}, {@code a4p}, {@code pos} — single invoice per page (portrait-scale)</li>
 *   <li>{@code a4l} — A4 landscape with the invoice rendered at A4 portrait scale,
 *       then each portrait page stamped onto a landscape half.
 *       1-page invoice → left half of 1 landscape sheet (right half empty).
 *       2-page invoice → left + right halves on 1 landscape sheet.
 *       N pages → ceil(N/2) landscape sheets.</li>
 * </ul>
 *
 * <p>Layout:
 *  1. Clinic header  — name (left) + INVOICE badge (right)
 *  2. Separator rule
 *  3. Meta block     — BILL TO (left) | Invoice Details + status (right)
 *  4. Items table    — dark-blue header, alternating rows, 7 columns
 *  5. Totals box     — right-aligned, conditional rows
 *  6. Footer         — thank-you line + page number
 */
public final class InvoicePdfGenerator {

    // ─── FONTS ───────────────────────────────────────────────────────────────
    private static final String FONT_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String FONT_BOLD    = "/fonts/DejaVuSans-Bold.ttf";

    // ─── DATE FORMATS ────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT    = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ─── REFERENCE LAYOUT (US Letter) — all values scale by PaperSize.scale() ─
    private static final float REF_PW     = 612f;
    private static final float REF_PH     = 792f;
    private static final float REF_MARGIN = 40f;
    private static final float REF_CW     = REF_PW - REF_MARGIN * 2;  // 532
    private static final float REF_BOT    = 65f;
    private static final float REF_ROW_H     = 20f;
    private static final float REF_HDR_ROW_H = 22f;

    // Reference column widths (sum = 532)
    private static final float[] REF_COL_W = {22f, 178f, 38f, 70f, 70f, 86f, 68f};

    // ─── DESIGN COLOURS  (RGB, 0.0 – 1.0) ───────────────────────────────────
    private static final float[] C_PRIMARY   = {0.11f, 0.27f, 0.49f};
    private static final float[] C_WHITE     = {1.00f, 1.00f, 1.00f};
    private static final float[] C_DARK_TXT  = {0.12f, 0.12f, 0.16f};
    private static final float[] C_MID_TXT   = {0.42f, 0.43f, 0.47f};
    private static final float[] C_LIGHT_BG  = {0.94f, 0.95f, 0.97f};
    private static final float[] C_ALT_ROW   = {0.97f, 0.97f, 0.98f};
    private static final float[] C_BORDER    = {0.73f, 0.74f, 0.77f};
    private static final float[] C_TOT_BG    = {0.95f, 0.96f, 0.97f};
    private static final float[] C_GREEN     = {0.06f, 0.50f, 0.20f};
    private static final float[] C_ORANGE    = {0.77f, 0.38f, 0.03f};
    private static final float[] C_RED       = {0.68f, 0.08f, 0.08f};

    // ─── TABLE COLUMNS (reference — scaled at runtime) ───────────────────────
    private static final String[] COL_HDR = {"#", "Description", "Qty", "Unit Price", "Ins. Covered", "Patient Payable", "Total"};

    // A4L rendering DPI — high enough for crisp text
    private static final int A4L_RENDER_DPI = 200;

    private InvoicePdfGenerator() {}

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════════

    public static void createInvoicePdf(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile,
            PaperSize paperSize
    ) throws IOException {
        if (paperSize == null) paperSize = PaperSize.LETTER;

        if (paperSize == PaperSize.A4L) {
            createA4LInvoice(invoiceFile, billing, items, clinicProfile);
        } else {
            createSingleInvoice(invoiceFile, billing, items, clinicProfile, paperSize);
        }
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

    // ═══════════════════════════════════════════════════════════════════════════
    //  A4 LANDSCAPE — TWO-PASS RENDER
    //
    //  1. Render invoice at A4 PORTRAIT scale → temp PDDocument (one or more pages)
    //  2. Render each portrait page as a high-DPI image
    //  3. Stamp images onto landscape halves:
    //     - 1 page  → left half only
    //     - 2 pages → left + right halves on same sheet
    //     - 3 pages → 2 sheets: (p1 left, p2 right), (p3 left)
    //     - N pages → ceil(N/2) landscape sheets
    // ═══════════════════════════════════════════════════════════════════════════

    private static void createA4LInvoice(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile
    ) throws IOException {
        // Step 1: Render invoice at A4 PORTRAIT scale to temp document
        PaperSize portraitSize = PaperSize.A4P;
        byte[] portraitBytes;
        int portraitPageCount;

        try (PDDocument portraitDoc = new PDDocument()) {
            PDType0Font regular = loadFont(portraitDoc, FONT_REGULAR);
            PDType0Font bold    = loadFont(portraitDoc, FONT_BOLD);
            Layout L = new Layout(portraitSize);
            Ctx ctx = new Ctx(portraitDoc, regular, bold, L);
            renderInvoiceContent(ctx, billing, items, clinicProfile, L);
            ctx.cs.close();

            portraitPageCount = portraitDoc.getNumberOfPages();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            portraitDoc.save(baos);
            portraitBytes = baos.toByteArray();
        }

        // Step 2: Build final A4L document with stamped images
        try (PDDocument portraitDoc = org.apache.pdfbox.Loader.loadPDF(portraitBytes);
             PDDocument finalDoc = new PDDocument()) {

            PDRectangle landscapeRect = PaperSize.A4L.rectangle();
            float pageW = landscapeRect.getWidth();   // 842 pt
            float pageH = landscapeRect.getHeight();  // 595 pt

            PDFRenderer renderer = new PDFRenderer(portraitDoc);

            int landscapeSheets = (portraitPageCount + 1) / 2;

            for (int sheet = 0; sheet < landscapeSheets; sheet++) {
                PDPage landscapePage = new PDPage(landscapeRect);
                finalDoc.addPage(landscapePage);

                // Render left half portrait page (index sheet*2)
                int leftIdx = sheet * 2;
                if (leftIdx < portraitPageCount) {
                    BufferedImage img = renderer.renderImageWithDPI(leftIdx, A4L_RENDER_DPI);
                    PDImageXObject pdImg = PDImageXObject.createFromByteArray(finalDoc,
                            imageToBytes(img, "png"), "portrait-left");

                    // Scale image to fit left half of landscape page
                    float imgW = img.getWidth();
                    float imgH = img.getHeight();
                    float halfPageW = pageW / 2f;

                    // Scale to fit within left half, maintaining aspect ratio
                    float scaleX = halfPageW / (imgW * 72f / A4L_RENDER_DPI);
                    float scaleY = pageH / (imgH * 72f / A4L_RENDER_DPI);
                    float scale = Math.min(scaleX, scaleY);

                    float drawW = imgW * 72f / A4L_RENDER_DPI * scale;
                    float drawH = imgH * 72f / A4L_RENDER_DPI * scale;

                    // Centre vertically within the page, left-aligned
                    float drawX = 0;
                    float drawY = (pageH - drawH) / 2f;

                    try (PDPageContentStream cs = new PDPageContentStream(finalDoc, landscapePage)) {
                        cs.drawImage(pdImg, drawX, drawY, drawW, drawH);
                    }
                }

                // Render right half portrait page (index sheet*2+1)
                int rightIdx = sheet * 2 + 1;
                if (rightIdx < portraitPageCount) {
                    BufferedImage img = renderer.renderImageWithDPI(rightIdx, A4L_RENDER_DPI);
                    PDImageXObject pdImg = PDImageXObject.createFromByteArray(finalDoc,
                            imageToBytes(img, "png"), "portrait-right");

                    float imgW = img.getWidth();
                    float imgH = img.getHeight();
                    float halfPageW = pageW / 2f;

                    float scaleX = halfPageW / (imgW * 72f / A4L_RENDER_DPI);
                    float scaleY = pageH / (imgH * 72f / A4L_RENDER_DPI);
                    float scale = Math.min(scaleX, scaleY);

                    float drawW = imgW * 72f / A4L_RENDER_DPI * scale;
                    float drawH = imgH * 72f / A4L_RENDER_DPI * scale;

                    float drawX = halfPageW;  // right half
                    float drawY = (pageH - drawH) / 2f;

                    try (PDPageContentStream cs = new PDPageContentStream(finalDoc, landscapePage)) {
                        cs.drawImage(pdImg, drawX, drawY, drawW, drawH);
                    }
                }
            }

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(invoiceFile.toFile()))) {
                finalDoc.save(out);
            }
        }
    }

    /** Convert BufferedImage to byte array in the given format. */
    private static byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SINGLE INVOICE (letter / a4p / pos)
    // ═══════════════════════════════════════════════════════════════════════════

    private static void createSingleInvoice(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile,
            PaperSize paperSize
    ) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font regular = loadFont(doc, FONT_REGULAR);
            PDType0Font bold    = loadFont(doc, FONT_BOLD);
            Layout L = new Layout(paperSize);
            Ctx ctx = new Ctx(doc, regular, bold, L);
            renderInvoiceContent(ctx, billing, items, clinicProfile, L);
            ctx.cs.close();

            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(invoiceFile.toFile()))) {
                doc.save(out);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SHARED INVOICE RENDERING
    // ═══════════════════════════════════════════════════════════════════════════

    private static void renderInvoiceContent(
            Ctx ctx, DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items, ClinicProfile clinicProfile,
            Layout L
    ) throws IOException {
        drawClinicHeader(ctx, clinicProfile, L);
        thickLine(ctx, C_PRIMARY, L.scale(1.5f));
        ctx.y -= L.scale(12);

        drawMetaSection(ctx, billing, L);
        ctx.y -= L.scale(12);

        drawItemsTable(ctx, items, L);
        ctx.y -= L.scale(8);

        drawTotalsSection(ctx, billing, L);

        drawFooter(ctx, L);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 1 — CLINIC HEADER
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawClinicHeader(Ctx ctx, ClinicProfile p, Layout L) throws IOException {
        float top = ctx.y;
        float rightEdge = L.rightEdge();

        // ── Right: INVOICE badge ──────────────────────────────────────────────
        float bW = L.scale(180f), bH = L.scale(54f);
        float bX = rightEdge - bW;
        float bY = top - bH;
        fillRgb(ctx, bX, bY, bW, bH, C_PRIMARY);
        textCentredInRect(ctx, ctx.bold, L.scaleFont(21), bX, bY, bW, bH, "INVOICE", C_WHITE);

        // ── Left: clinic info ─────────────────────────────────────────────────
        float leftMaxW = bX - L.margin - L.scale(12f);

        String clinicName = (p != null && ok(p.getName())) ? p.getName().trim() : "Medical Clinic";
        ink(ctx, C_PRIMARY);
        putText(ctx, ctx.bold, L.scaleFont(17), L.margin, top, clip(ctx.bold, L.scaleFont(17), clinicName, leftMaxW));
        ctx.y -= L.scale(22);

        if (p != null && ok(p.getAddress())) {
            ink(ctx, C_DARK_TXT);
            putText(ctx, ctx.regular, L.scaleFont(10), L.margin, ctx.y, clip(ctx.regular, L.scaleFont(10), p.getAddress().trim(), leftMaxW));
            ctx.y -= L.scale(13);
        }

        if (p != null && p.getContacts() != null) {
            for (ClinicContact c : p.getContacts()) {
                if (c == null || !ok(c.getValue())) continue;
                String label = contactLabel(c.getContactType());
                if (label == null) continue;
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.regular, L.scaleFont(10), L.margin, ctx.y, clip(ctx.regular, L.scaleFont(10), label + ": " + c.getValue().trim(), leftMaxW));
                ctx.y -= L.scale(13);
            }
        }

        if (p != null && ok(p.getTinNumber())) {
            ink(ctx, C_MID_TXT);
            putText(ctx, ctx.regular, L.scaleFont(10), L.margin, ctx.y, "TIN: " + p.getTinNumber().trim());
            ctx.y -= L.scale(13);
        }

        // Ensure y clears the badge + a small gap
        float badgeBottom = top - bH - L.scale(8f);
        if (ctx.y > badgeBottom) {
            ctx.y = badgeBottom;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 2 — META / BILL-TO
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawMetaSection(Ctx ctx, DepartmentInsuranceBilling billing, Layout L) throws IOException {
        float sectionTop = ctx.y;
        float rightEdge = L.rightEdge();
        float contentW = rightEdge - L.margin;
        float leftX  = L.margin + L.scale(4f);
        float rightX = L.margin + contentW / 2f + L.scale(8f);
        float colW   = contentW / 2f - L.scale(12f);

        // ── Collect left-column lines (BILL TO) ──────────────────────────────
        Visit visit   = resolveVisit(billing);
        Patient pat   = visit == null ? null : visit.getPatient();
        VisitDepartment vd = billing.getVisitDepartmentBilling() == null ? null
                : billing.getVisitDepartmentBilling().getVisitDepartment();

        List<MetaLine> left = new ArrayList<>();
        left.add(MetaLine.label("BILL TO", L));
        left.add(MetaLine.bold(formatPatientName(pat), L.scaleFont(11), L));
        if (pat != null && ok(pat.getPrimaryPhoneNumber())) {
            left.add(MetaLine.kv("Phone", pat.getPrimaryPhoneNumber().trim(), L));
        }
        if (visit != null && visit.getVisitDate() != null) {
            left.add(MetaLine.kv("Visit Date", DATE_FMT.format(visit.getVisitDate()), L));
        }
        if (vd != null && vd.getDepartment() != null && ok(vd.getDepartment().getName())) {
            left.add(MetaLine.kv("Department", vd.getDepartment().getName().trim(), L));
        }
        String insLine = buildInsuranceLine(billing.getPatientInsurance());
        if (insLine != null) {
            left.add(MetaLine.kv("Insurance", insLine, L));
        }

        // ── Collect right-column lines (INVOICE DETAILS) ─────────────────────
        List<MetaLine> right = new ArrayList<>();
        right.add(MetaLine.label("INVOICE DETAILS", L));
        String invNo = billing.getId() == null ? "-" : abbrevId(billing.getId().toString().toUpperCase());
        right.add(MetaLine.kv("Invoice No", invNo, L));

        LocalDateTime billingDate = (billing.getVisitDepartmentBilling() != null
                && billing.getVisitDepartmentBilling().getVisitBilling() != null)
                ? billing.getVisitDepartmentBilling().getVisitBilling().getCreatedAt() : null;
        if (billingDate != null) {
            right.add(MetaLine.kv("Invoice Date", DT_FMT.format(billingDate), L));
        }
        right.add(MetaLine.status(billing.getStatus(), L));

        // ── Compute heights and draw background ───────────────────────────────
        float leftH  = computeMetaHeight(left);
        float rightH = computeMetaHeight(right);
        float sectionH = Math.max(leftH, rightH) + L.scale(18f);

        fillRgb(ctx, L.margin - L.scale(2), sectionTop - sectionH, contentW + L.scale(4), sectionH, C_LIGHT_BG);

        // ── Render left column ────────────────────────────────────────────────
        float ly = sectionTop - L.scale(8f);
        for (MetaLine ml : left) {
            ly = renderMetaLine(ctx, ml, leftX, ly, colW);
        }

        // ── Render right column ───────────────────────────────────────────────
        float ry = sectionTop - L.scale(8f);
        for (MetaLine ml : right) {
            ry = renderMetaLine(ctx, ml, rightX, ry, colW);
        }

        ctx.y = sectionTop - sectionH;
        strokeRgb(ctx, L.margin - L.scale(2), ctx.y, contentW + L.scale(4), sectionH, C_BORDER, L.scale(0.5f));
    }

    // ── MetaLine descriptor ───────────────────────────────────────────────────
    private record MetaLine(Kind kind, String text, String value, float size, VisitBillingStatus status, float advanceHeight) {
        enum Kind { LABEL, BOLD, KV, STATUS }

        static MetaLine label(String t, Layout L) {
            return new MetaLine(Kind.LABEL, t, null, L.scaleFont(8), null, L.scale(12f));
        }
        static MetaLine bold(String t, float sz, Layout L) {
            return new MetaLine(Kind.BOLD, t, null, sz, null, sz + L.scale(5f));
        }
        static MetaLine kv(String k, String v, Layout L) {
            return new MetaLine(Kind.KV, k, v, L.scaleFont(10), null, L.scale(13f));
        }
        static MetaLine status(VisitBillingStatus s, Layout L) {
            return new MetaLine(Kind.STATUS, null, null, L.scaleFont(10), s, L.scale(16f));
        }

        float advance() { return advanceHeight; }
    }

    private static float computeMetaHeight(List<MetaLine> lines) {
        float h = 0;
        for (MetaLine ml : lines) h += ml.advance();
        return h;
    }

    private static float renderMetaLine(Ctx ctx, MetaLine ml, float x, float y, float maxW) throws IOException {
        return switch (ml.kind()) {
            case LABEL -> {
                ink(ctx, C_MID_TXT);
                putText(ctx, ctx.bold, ml.size(), x, y, ml.text().toUpperCase());
                yield y - ml.advance();
            }
            case BOLD -> {
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.bold, ml.size(), x, y, clip(ctx.bold, ml.size(), ml.text(), maxW));
                yield y - ml.advance();
            }
            case KV -> {
                String combined = ml.text() + ": ";
                float kw = tw(ctx.bold, ml.size(), combined);
                ink(ctx, C_MID_TXT);
                putText(ctx, ctx.bold, ml.size(), x, y, combined);
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.regular, ml.size(), x + kw, y, clip(ctx.regular, ml.size(), ml.value(), maxW - kw));
                yield y - ml.advance();
            }
            case STATUS -> {
                String label = statusLabel(ml.status());
                // "Status: " prefix — bold, no colored background
                String statusPrefix = "Status: ";
                ink(ctx, C_MID_TXT);
                putText(ctx, ctx.bold, ml.size(), x, y, statusPrefix);
                float sx = x + tw(ctx.bold, ml.size(), statusPrefix);
                // Status value — bold, dark text
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.bold, ml.size(), sx, y, label);
                yield y - ml.advance();
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 3 — ITEMS TABLE
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawItemsTable(Ctx ctx, List<Map<String, Object>> items, Layout L) throws IOException {
        space(ctx, L.hdrRowH + L.rowH, true, L);
        drawTableHeader(ctx, ctx.y, L);
        ctx.y -= L.hdrRowH;

        int row = 0;
        for (Map<String, Object> item : items) {
            space(ctx, L.rowH, false, L);
            drawTableRow(ctx, ctx.y, item, row + 1, row % 2 == 1, L);
            ctx.y -= L.rowH;
            row++;
        }
        // Bottom border of table
        lineRgb(ctx, L.margin, ctx.y, L.rightEdge(), ctx.y, C_BORDER, L.scale(0.5f));
    }

    private static void drawTableHeader(Ctx ctx, float topY, Layout L) throws IOException {
        float rightEdge = L.rightEdge();
        float contentW = rightEdge - L.margin;
        fillRgb(ctx, L.margin, topY - L.hdrRowH, contentW, L.hdrRowH, C_PRIMARY);
        float x = L.margin;
        for (int i = 0; i < COL_HDR.length; i++) {
            if (i > 0) lineRgb(ctx, x, topY, x, topY - L.hdrRowH, C_WHITE, L.scale(0.4f));
            boolean numCol = i > 1;
            if (numCol) {
                ink(ctx, C_WHITE);
                putTextRight(ctx, ctx.bold, L.scaleFont(9), x + L.colW[i] - L.scale(5), topY - L.scale(7), COL_HDR[i]);
            } else {
                ink(ctx, C_WHITE);
                putText(ctx, ctx.bold, L.scaleFont(9), x + (i == 0 ? L.scale(5) : L.scale(4)), topY - L.scale(7), COL_HDR[i]);
            }
            x += L.colW[i];
        }
    }

    private static void drawTableRow(Ctx ctx, float topY, Map<String, Object> item, int rowNum, boolean alt, Layout L) throws IOException {
        float rightEdge = L.rightEdge();
        float contentW = rightEdge - L.margin;
        if (alt) fillRgb(ctx, L.margin, topY - L.rowH, contentW, L.rowH, C_ALT_ROW);
        strokeRgb(ctx, L.margin, topY - L.rowH, contentW, L.rowH, C_BORDER, L.scale(0.3f));

        String[] vals = {
                String.valueOf(rowNum),
                clip(ctx.regular, L.scaleFont(9), String.valueOf(item.getOrDefault("productName", "")), L.colW[1] - L.scale(8)),
                fmtQty(item.get("quantitySnapshot")),
                fmtMoney(item.get("unitPriceSnapshot")),
                fmtMoney(item.get("insuranceCoveredAmount")),
                fmtMoney(item.get("patientPayableAmount")),
                fmtMoney(item.get("lineTotal"))
        };

        float x = L.margin;
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) lineRgb(ctx, x, topY, x, topY - L.rowH, C_BORDER, L.scale(0.3f));
            boolean numCol = i > 1;
            ink(ctx, C_DARK_TXT);
            if (numCol) {
                putTextRight(ctx, ctx.regular, L.scaleFont(9), x + L.colW[i] - L.scale(5), topY - L.scale(13), vals[i]);
            } else {
                putText(ctx, ctx.regular, L.scaleFont(9), x + (i == 0 ? L.scale(7) : L.scale(4)), topY - L.scale(13), vals[i]);
            }
            x += L.colW[i];
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 4 — TOTALS BOX
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawTotalsSection(Ctx ctx, DepartmentInsuranceBilling b, Layout L) throws IOException {
        List<TotLine> lines = buildTotals(b);
        float rightEdge = L.rightEdge();
        float boxW = L.scale(228f);
        float lineH = L.scale(17f);
        float boxH  = lines.size() * lineH + L.scale(16f);
        float boxX  = rightEdge - boxW;

        space(ctx, boxH + L.scale(16), false, L);
        fillRgb(ctx, boxX, ctx.y - boxH, boxW, boxH, C_TOT_BG);
        strokeRgb(ctx, boxX, ctx.y - boxH, boxW, boxH, C_BORDER, L.scale(0.6f));

        float ly = ctx.y - L.scale(10f);
        for (int i = 0; i < lines.size(); i++) {
            TotLine tl = lines.get(i);
            if (i == lines.size() - 1 && lines.size() > 1) {
                lineRgb(ctx, boxX + L.scale(6), ly + L.scale(4), boxX + boxW - L.scale(6), ly + L.scale(4), C_BORDER, L.scale(0.5f));
                ly -= L.scale(3);
            }
            PDType0Font lFont = tl.bold ? ctx.bold : ctx.regular;
            float lSize = tl.bold ? L.scaleFont(10.5f) : L.scaleFont(10f);
            ink(ctx, tl.labelColor);
            putText(ctx, lFont, lSize, boxX + L.scale(8), ly, tl.label + ":");
            ink(ctx, tl.valueColor);
            putTextRight(ctx, ctx.bold, lSize, boxX + boxW - L.scale(7), ly, tl.value);
            ink(ctx, C_DARK_TXT);
            ly -= lineH;
        }

        ctx.y -= boxH + L.scale(8);
    }

    private record TotLine(String label, String value, boolean bold, float[] labelColor, float[] valueColor) {}

    private static List<TotLine> buildTotals(DepartmentInsuranceBilling b) {
        List<TotLine> list = new ArrayList<>();
        list.add(new TotLine("Total Amount",     fmtMoney(b.getTotalAmount()),            false, C_MID_TXT, C_DARK_TXT));
        BigDecimal ins = b.getInsuranceCoveredAmount();
        if (ins != null && ins.compareTo(BigDecimal.ZERO) > 0) {
            list.add(new TotLine("Insurance Covered", fmtMoney(ins),                      false, C_MID_TXT, C_MID_TXT));
        }
        list.add(new TotLine("Patient Payable",  fmtMoney(b.getPatientPayableAmount()),   false, C_MID_TXT, C_DARK_TXT));
        BigDecimal paid = b.getPaidAmount();
        if (paid != null && paid.compareTo(BigDecimal.ZERO) > 0) {
            list.add(new TotLine("Paid",         fmtMoney(paid),                          false, C_MID_TXT, C_GREEN));
        }
        BigDecimal out = b.getOutstandingAmount();
        boolean hasOut = out != null && out.compareTo(BigDecimal.ZERO) > 0;
        list.add(new TotLine("Outstanding",      fmtMoney(out),                           true,  C_PRIMARY,  hasOut ? C_RED : C_GREEN));
        return list;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FOOTER
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawFooter(Ctx ctx, Layout L) throws IOException {
        float rightEdge = L.rightEdge();
        float fy = L.bot - L.scale(12f);
        lineRgb(ctx, L.margin, fy + L.scale(16), rightEdge, fy + L.scale(16), C_BORDER, L.scale(0.8f));
        ink(ctx, C_MID_TXT);
        putText(ctx,   ctx.regular, L.scaleFont(9), L.margin,    fy, "Thank you for choosing our services.");
        putTextRight(ctx, ctx.regular, L.scaleFont(9), rightEdge, fy, "Page " + ctx.page);
        ink(ctx, C_DARK_TXT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAGINATION — ensure space, add new page when needed
    // ═══════════════════════════════════════════════════════════════════════════

    private static void space(Ctx ctx, float needed, boolean isTableStart, Layout L) throws IOException {
        if (ctx.y - needed >= L.bot) return;

        ctx.cs.close();
        ctx.page++;
        PDPage pg = new PDPage(L.paperSize.rectangle());
        ctx.doc.addPage(pg);
        ctx.cs = new PDPageContentStream(ctx.doc, pg);
        ctx.y  = L.PH - L.margin;

        float rightEdge = L.rightEdge();

        // Compact continuation header
        ink(ctx, C_PRIMARY);
        putText(ctx, ctx.bold, L.scaleFont(11), L.margin, ctx.y, "Invoice (continued)");
        ink(ctx, C_MID_TXT);
        putTextRight(ctx, ctx.regular, L.scaleFont(9), rightEdge, ctx.y, "Page " + ctx.page);
        ctx.y -= L.scale(16);
        lineRgb(ctx, L.margin, ctx.y, rightEdge, ctx.y, C_PRIMARY, L.scale(1.2f));
        ctx.y -= L.scale(10);
        ink(ctx, C_DARK_TXT);

        // Redraw table header on continuation pages (unless this call IS the table start)
        if (!isTableStart) {
            drawTableHeader(ctx, ctx.y, L);
            ctx.y -= L.hdrRowH;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DRAWING PRIMITIVES
    // ═══════════════════════════════════════════════════════════════════════════

    private static void fillRgb(Ctx ctx, float x, float y, float w, float h, float[] rgb) throws IOException {
        ctx.cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.addRect(x, y, w, h);
        ctx.cs.fill();
        ctx.cs.setNonStrokingColor(0, 0, 0);
    }

    private static void strokeRgb(Ctx ctx, float x, float y, float w, float h, float[] rgb, float lw) throws IOException {
        ctx.cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.setLineWidth(lw);
        ctx.cs.addRect(x, y, w, h);
        ctx.cs.stroke();
        ctx.cs.setStrokingColor(0, 0, 0);
    }

    private static void lineRgb(Ctx ctx, float x1, float y1, float x2, float y2, float[] rgb, float lw) throws IOException {
        ctx.cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.setLineWidth(lw);
        ctx.cs.moveTo(x1, y1);
        ctx.cs.lineTo(x2, y2);
        ctx.cs.stroke();
        ctx.cs.setStrokingColor(0, 0, 0);
    }

    private static void thickLine(Ctx ctx, float[] rgb, float lw) throws IOException {
        lineRgb(ctx, ctx.L.margin, ctx.y, ctx.L.rightEdge(), ctx.y, rgb, lw);
        ctx.y -= 2;
    }

    private static void ink(Ctx ctx, float[] rgb) throws IOException {
        ctx.cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    private static void putText(Ctx ctx, PDType0Font font, float size, float x, float y, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        ctx.cs.beginText();
        ctx.cs.setFont(font, size);
        ctx.cs.newLineAtOffset(x, y);
        ctx.cs.showText(text);
        ctx.cs.endText();
    }

    private static void putTextRight(Ctx ctx, PDType0Font font, float size, float rightEdge, float y, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        float w = tw(font, size, text);
        putText(ctx, font, size, rightEdge - w, y, text);
    }

    private static void textCentredInRect(Ctx ctx, PDType0Font font, float size,
                                          float rx, float ry, float rw, float rh,
                                          String text, float[] rgb) throws IOException {
        float tW = tw(font, size, text);
        float tx = rx + (rw - tW) / 2f;
        float ty = ry + (rh - size) / 2f;
        ink(ctx, rgb);
        putText(ctx, font, size, tx, ty, text);
        ink(ctx, C_DARK_TXT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TEXT / DATA HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    private static float tw(PDType0Font font, float size, String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return font.getStringWidth(text) / 1000f * size;
        } catch (IOException e) {
            return text.length() * size * 0.5f;
        }
    }

    private static String clip(PDType0Font font, float size, String text, float maxWidth) {
        if (text == null) return "";
        try {
            if (font.getStringWidth(text) / 1000f * size <= maxWidth) return text;
            String ellipsis = "...";
            String t = text;
            while (!t.isEmpty() && font.getStringWidth(t + ellipsis) / 1000f * size > maxWidth) {
                t = t.substring(0, t.length() - 1);
            }
            return t.isEmpty() ? ellipsis : t + ellipsis;
        } catch (IOException e) {
            int max = Math.max(3, (int) (maxWidth / (size * 0.5f)));
            return text.length() > max ? text.substring(0, max - 3) + "..." : text;
        }
    }

    private static String abbrevId(String id) {
        if (id == null) return "-";
        String s = id.replaceAll("-", "");
        return s.length() > 16 ? "..." + s.substring(s.length() - 13) : s;
    }

    private static String contactLabel(ClinicContactType t) {
        if (t == null) return null;
        return switch (t) {
            case PHONE -> "Phone";
            case EMAIL -> "Email";
            case POBOX -> "P.O. Box";
        };
    }

    private static String statusLabel(VisitBillingStatus s) {
        if (s == null) return "UNKNOWN";
        return switch (s) {
            case PAID          -> "PAID";
            case PARTIALLY_PAID -> "PARTIALLY PAID";
            case UNPAID        -> "UNPAID";
        };
    }

    private static float[] statusColor(VisitBillingStatus s) {
        if (s == null) return C_MID_TXT;
        return switch (s) {
            case PAID          -> C_GREEN;
            case PARTIALLY_PAID -> C_ORANGE;
            case UNPAID        -> C_RED;
        };
    }

    private static Visit resolveVisit(DepartmentInsuranceBilling billing) {
        if (billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null) return null;
        return billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
    }

    private static String formatPatientName(Patient p) {
        if (p == null) return "Unknown";
        if (ok(p.getFullName())) return p.getFullName().trim();
        String first  = p.getFirstName()  == null ? "" : p.getFirstName().trim();
        String middle = p.getMiddleName() == null ? "" : p.getMiddleName().trim();
        String last   = p.getLastName()   == null ? "" : p.getLastName().trim();
        String full   = (first + " " + middle + " " + last).replaceAll("\\s+", " ").trim();
        return full.isEmpty() ? "Unknown" : full;
    }

    private static String buildInsuranceLine(PatientInsurance ins) {
        if (ins == null) return null;
        InsuranceProvider prov = ins.getInsuranceProvider();
        String name = (prov != null && ok(prov.getInsuranceName())) ? prov.getInsuranceName().trim()
                : (prov != null && ok(prov.getAcronym())) ? prov.getAcronym().trim() : null;
        String card = ok(ins.getInsuranceCardNumber()) ? ins.getInsuranceCardNumber().trim() : null;
        if (name == null && card == null) return null;
        StringBuilder sb = new StringBuilder();
        if (name != null) sb.append(name);
        if (card != null) { if (sb.length() > 0) sb.append(" \u2014 "); sb.append(card); }
        return sb.toString();
    }

    private static String fmtMoney(Object val) {
        if (val == null) return "0.00";
        if (val instanceof BigDecimal d) return d.setScale(2, RoundingMode.HALF_UP).toPlainString();
        return String.valueOf(val);
    }

    private static String fmtQty(Object val) {
        if (val == null) return "0";
        if (val instanceof BigDecimal d) return d.stripTrailingZeros().toPlainString();
        return String.valueOf(val);
    }

    private static boolean ok(String s) {
        return s != null && !s.isBlank();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  FONT LOADING
    // ═══════════════════════════════════════════════════════════════════════════

    private static PDType0Font loadFont(PDDocument doc, String path) throws IOException {
        try (InputStream is = InvoicePdfGenerator.class.getResourceAsStream(path)) {
            if (is == null) throw new IOException("Missing bundled font: " + path);
            byte[] bytes = is.readAllBytes();
            return PDType0Font.load(doc, new ByteArrayInputStream(bytes));
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SCALING LAYOUT — holds all paper-size-dependent dimensions
    // ═══════════════════════════════════════════════════════════════════════════

    static final class Layout {
        final PaperSize paperSize;
        final float PW, PH, margin, bot;
        final float rowH, hdrRowH;
        final float[] colW;

        Layout(PaperSize ps) {
            this.paperSize = ps;
            this.PW     = ps.pageWidth();
            this.PH     = ps.pageHeight();
            this.margin = ps.margin();
            this.bot    = ps.bottomMargin();
            this.rowH     = ps.scale(REF_ROW_H);
            this.hdrRowH  = ps.scale(REF_HDR_ROW_H);
            this.colW     = new float[REF_COL_W.length];
            for (int i = 0; i < REF_COL_W.length; i++) {
                colW[i] = ps.scale(REF_COL_W[i]);
            }
            // Fix rounding: adjust Description column so total = contentWidth
            float cw = contentWidth();
            float sum = 0;
            for (float w : colW) sum += w;
            colW[1] += cw - sum;
        }

        float rightEdge() {
            return PW - margin;
        }

        float contentWidth() {
            return PW - margin * 2;
        }

        float scale(float v) { return paperSize.scale(v); }
        float scaleFont(float v) { return paperSize.scaleFont(v); }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  RENDERING CONTEXT
    // ═══════════════════════════════════════════════════════════════════════════

    private static final class Ctx {
        final PDDocument        doc;
        final PDType0Font       regular;
        final PDType0Font       bold;
        final Layout            L;
        PDPageContentStream     cs;
        float                   y;
        int                     page = 1;

        Ctx(PDDocument doc, PDType0Font regular, PDType0Font bold, Layout L) throws IOException {
            this.doc     = doc;
            this.regular = regular;
            this.bold    = bold;
            this.L       = L;
            PDPage pg = new PDPage(L.paperSize.rectangle());
            doc.addPage(pg);
            this.cs = new PDPageContentStream(doc, pg);
            this.y  = L.PH - L.margin;
        }
    }
}
