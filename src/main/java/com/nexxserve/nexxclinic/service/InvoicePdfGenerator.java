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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
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

/**
 * Generates a professionally structured invoice PDF.
 *
 * Layout:
 *  1. Clinic header  — name (left) + INVOICE badge (right), all non-null clinic fields
 *  2. Separator rule
 *  3. Meta block     — BILL TO (left) | Invoice Details + status badge (right)
 *  4. Items table    — dark-blue header, alternating rows, 7 columns
 *  5. Totals box     — right-aligned, conditional rows (insurance / paid / outstanding)
 *  6. Footer         — thank-you line + page number
 */
public final class InvoicePdfGenerator {

    // ─── FONTS ───────────────────────────────────────────────────────────────
    private static final String FONT_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String FONT_BOLD    = "/fonts/DejaVuSans-Bold.ttf";

    // ─── DATE FORMATS ────────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT    = DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");

    // ─── PAGE GEOMETRY ───────────────────────────────────────────────────────
    private static final float PW     = PDRectangle.LETTER.getWidth();   // 612
    private static final float PH     = PDRectangle.LETTER.getHeight();  // 792
    private static final float MARGIN = 40f;
    private static final float CW     = PW - MARGIN * 2;                 // 532 (content width)
    private static final float BOT    = 65f;                              // bottom margin (footer area)

    // ─── DESIGN COLOURS  (RGB, 0.0 – 1.0) ───────────────────────────────────
    private static final float[] C_PRIMARY   = {0.11f, 0.27f, 0.49f}; // dark navy
    private static final float[] C_WHITE     = {1.00f, 1.00f, 1.00f};
    private static final float[] C_DARK_TXT  = {0.12f, 0.12f, 0.16f};
    private static final float[] C_MID_TXT   = {0.42f, 0.43f, 0.47f};
    private static final float[] C_LIGHT_BG  = {0.94f, 0.95f, 0.97f}; // meta section bg
    private static final float[] C_ALT_ROW   = {0.97f, 0.97f, 0.98f}; // table alt row
    private static final float[] C_BORDER    = {0.73f, 0.74f, 0.77f};
    private static final float[] C_TOT_BG    = {0.95f, 0.96f, 0.97f}; // totals box bg
    private static final float[] C_GREEN     = {0.06f, 0.50f, 0.20f};
    private static final float[] C_ORANGE    = {0.77f, 0.38f, 0.03f};
    private static final float[] C_RED       = {0.68f, 0.08f, 0.08f};

    // ─── TABLE COLUMNS ───────────────────────────────────────────────────────
    // # | Description | Qty | Unit Price | Ins. Covered | Patient Payable | Total
    private static final String[] COL_HDR = {"#", "Description", "Qty", "Unit Price", "Ins. Covered", "Patient Payable", "Total"};
    private static final float[]  COL_W   = {22f, 178f, 38f, 70f, 70f, 86f, 68f}; // sum = 532

    private static final float ROW_H     = 20f;
    private static final float HDR_ROW_H = 22f;

    private InvoicePdfGenerator() {}

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC ENTRY POINT
    // ═══════════════════════════════════════════════════════════════════════════

    public static void createInvoicePdf(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile
    ) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDType0Font regular = loadFont(doc, FONT_REGULAR);
            PDType0Font bold    = loadFont(doc, FONT_BOLD);
            Ctx ctx = new Ctx(doc, regular, bold);

            drawClinicHeader(ctx, clinicProfile);
            thickLine(ctx, C_PRIMARY, 1.5f);
            ctx.y -= 12;

            drawMetaSection(ctx, billing);
            ctx.y -= 12;

            drawItemsTable(ctx, items);
            ctx.y -= 8;

            drawTotalsSection(ctx, billing);

            drawFooter(ctx);

            ctx.cs.close();
            doc.save(invoiceFile.toFile());
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 1 — CLINIC HEADER
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawClinicHeader(Ctx ctx, ClinicProfile p) throws IOException {
        float top = ctx.y;

        // ── Right: INVOICE badge ──────────────────────────────────────────────
        float bW = 180f, bH = 54f;
        float bX = PW - MARGIN - bW;
        float bY = top - bH;
        fillRgb(ctx, bX, bY, bW, bH, C_PRIMARY);
        // "INVOICE" centred in badge
        textCentredInRect(ctx, ctx.bold, 21, bX, bY, bW, bH, "INVOICE", C_WHITE);

        // ── Left: clinic info ─────────────────────────────────────────────────
        float leftMaxW = bX - MARGIN - 12f;

        String clinicName = (p != null && ok(p.getName())) ? p.getName().trim() : "Medical Clinic";
        ink(ctx, C_PRIMARY);
        putText(ctx, ctx.bold, 17, MARGIN, top, clip(ctx.bold, 17, clinicName, leftMaxW));
        ctx.y -= 22;

        if (p != null && ok(p.getAddress())) {
            ink(ctx, C_DARK_TXT);
            putText(ctx, ctx.regular, 10, MARGIN, ctx.y, clip(ctx.regular, 10, p.getAddress().trim(), leftMaxW));
            ctx.y -= 13;
        }

        if (p != null && p.getContacts() != null) {
            for (ClinicContact c : p.getContacts()) {
                if (c == null || !ok(c.getValue())) continue;
                String label = contactLabel(c.getContactType());
                if (label == null) continue;
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.regular, 10, MARGIN, ctx.y, clip(ctx.regular, 10, label + ": " + c.getValue().trim(), leftMaxW));
                ctx.y -= 13;
            }
        }

        if (p != null && ok(p.getTinNumber())) {
            ink(ctx, C_MID_TXT);
            putText(ctx, ctx.regular, 10, MARGIN, ctx.y, "TIN: " + p.getTinNumber().trim());
            ctx.y -= 13;
        }

        // Ensure y clears the badge + a small gap
        float badgeBottom = top - bH - 8f;
        if (ctx.y > badgeBottom) {
            ctx.y = badgeBottom;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 2 — META / BILL-TO
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawMetaSection(Ctx ctx, DepartmentInsuranceBilling billing) throws IOException {
        float sectionTop = ctx.y;
        // Two halves
        float leftX  = MARGIN + 4f;
        float rightX = MARGIN + CW / 2f + 16f;
        float colW   = CW / 2f - 20f;

        // ── Collect left-column lines (BILL TO) ──────────────────────────────
        Visit visit   = resolveVisit(billing);
        Patient pat   = visit == null ? null : visit.getPatient();
        VisitDepartment vd = billing.getVisitDepartmentBilling() == null ? null
                : billing.getVisitDepartmentBilling().getVisitDepartment();

        List<MetaLine> left = new ArrayList<>();
        left.add(MetaLine.label("BILL TO"));
        left.add(MetaLine.bold(formatPatientName(pat), 11));
        if (pat != null && ok(pat.getPrimaryPhoneNumber())) {
            left.add(MetaLine.kv("Phone", pat.getPrimaryPhoneNumber().trim()));
        }
        if (visit != null && visit.getVisitDate() != null) {
            left.add(MetaLine.kv("Visit Date", DATE_FMT.format(visit.getVisitDate())));
        }
        if (vd != null && vd.getDepartment() != null && ok(vd.getDepartment().getName())) {
            left.add(MetaLine.kv("Department", vd.getDepartment().getName().trim()));
        }
        String insLine = buildInsuranceLine(billing.getPatientInsurance());
        if (insLine != null) {
            left.add(MetaLine.kv("Insurance", insLine));
        }

        // ── Collect right-column lines (INVOICE DETAILS) ─────────────────────
        List<MetaLine> right = new ArrayList<>();
        right.add(MetaLine.label("INVOICE DETAILS"));
        String invNo = billing.getId() == null ? "-" : abbrevId(billing.getId().toString().toUpperCase());
        right.add(MetaLine.kv("Invoice No", invNo));

        LocalDateTime billingDate = (billing.getVisitDepartmentBilling() != null
                && billing.getVisitDepartmentBilling().getVisitBilling() != null)
                ? billing.getVisitDepartmentBilling().getVisitBilling().getCreatedAt() : null;
        if (billingDate != null) {
            right.add(MetaLine.kv("Invoice Date", DT_FMT.format(billingDate)));
        }
        right.add(MetaLine.status(billing.getStatus()));

        // ── Compute heights and draw background ───────────────────────────────
        float leftH  = computeMetaHeight(left);
        float rightH = computeMetaHeight(right);
        float sectionH = Math.max(leftH, rightH) + 18f;

        fillRgb(ctx, MARGIN - 2, sectionTop - sectionH, CW + 4, sectionH, C_LIGHT_BG);

        // ── Render left column ────────────────────────────────────────────────
        float ly = sectionTop - 8f;
        for (MetaLine ml : left) {
            ly = renderMetaLine(ctx, ml, leftX, ly, colW);
        }

        // ── Render right column ───────────────────────────────────────────────
        float ry = sectionTop - 8f;
        for (MetaLine ml : right) {
            ry = renderMetaLine(ctx, ml, rightX, ry, colW);
        }

        ctx.y = sectionTop - sectionH;
        strokeRgb(ctx, MARGIN - 2, ctx.y, CW + 4, sectionH, C_BORDER, 0.5f);
    }

    // ── MetaLine descriptor ───────────────────────────────────────────────────
    private record MetaLine(Kind kind, String text, String value, float size, VisitBillingStatus status) {
        enum Kind { LABEL, BOLD, KV, STATUS }

        static MetaLine label(String t)            { return new MetaLine(Kind.LABEL, t, null, 8f, null); }
        static MetaLine bold(String t, float sz)   { return new MetaLine(Kind.BOLD, t, null, sz, null); }
        static MetaLine kv(String k, String v)     { return new MetaLine(Kind.KV, k, v, 10f, null); }
        static MetaLine status(VisitBillingStatus s){ return new MetaLine(Kind.STATUS, null, null, 10f, s); }

        float advance() {
            return switch (kind) {
                case LABEL  -> 12f;
                case BOLD   -> size + 5f;
                case KV     -> 13f;
                case STATUS -> 16f;
            };
        }
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
                float kw = tw(ctx.bold, 10, combined);
                ink(ctx, C_MID_TXT);
                putText(ctx, ctx.bold, 10, x, y, combined);
                ink(ctx, C_DARK_TXT);
                putText(ctx, ctx.regular, 10, x + kw, y, clip(ctx.regular, 10, ml.value(), maxW - kw));
                yield y - ml.advance();
            }
            case STATUS -> {
                String label = statusLabel(ml.status());
                float[] color = statusColor(ml.status());
                ink(ctx, C_MID_TXT);
                putText(ctx, ctx.bold, 10, x, y, "Status: ");
                float sx = x + tw(ctx.bold, 10, "Status: ");
                float bW = tw(ctx.bold, 9, label) + 10f;
                float bH = 13f;
                fillRgb(ctx, sx, y - bH + 3f, bW, bH, color);
                ink(ctx, C_WHITE);
                putText(ctx, ctx.bold, 9, sx + 4f, y - 0.5f, label);
                ink(ctx, C_DARK_TXT);
                yield y - ml.advance();
            }
        };
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 3 — ITEMS TABLE
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawItemsTable(Ctx ctx, List<Map<String, Object>> items) throws IOException {
        space(ctx, HDR_ROW_H + ROW_H, true);
        drawTableHeader(ctx, ctx.y);
        ctx.y -= HDR_ROW_H;

        int row = 0;
        for (Map<String, Object> item : items) {
            space(ctx, ROW_H, false);
            drawTableRow(ctx, ctx.y, item, row + 1, row % 2 == 1);
            ctx.y -= ROW_H;
            row++;
        }
        // Bottom border of table
        lineRgb(ctx, MARGIN, ctx.y, MARGIN + CW, ctx.y, C_BORDER, 0.5f);
    }

    private static void drawTableHeader(Ctx ctx, float topY) throws IOException {
        fillRgb(ctx, MARGIN, topY - HDR_ROW_H, CW, HDR_ROW_H, C_PRIMARY);
        float x = MARGIN;
        for (int i = 0; i < COL_HDR.length; i++) {
            if (i > 0) lineRgb(ctx, x, topY, x, topY - HDR_ROW_H, C_WHITE, 0.4f);
            boolean numCol = i > 1;
            if (numCol) {
                ink(ctx, C_WHITE);
                putTextRight(ctx, ctx.bold, 9, x + COL_W[i] - 5, topY - 7, COL_HDR[i]);
            } else {
                ink(ctx, C_WHITE);
                putText(ctx, ctx.bold, 9, x + (i == 0 ? 5 : 4), topY - 7, COL_HDR[i]);
            }
            x += COL_W[i];
        }
    }

    private static void drawTableRow(Ctx ctx, float topY, Map<String, Object> item, int rowNum, boolean alt) throws IOException {
        if (alt) fillRgb(ctx, MARGIN, topY - ROW_H, CW, ROW_H, C_ALT_ROW);
        strokeRgb(ctx, MARGIN, topY - ROW_H, CW, ROW_H, C_BORDER, 0.3f);

        String[] vals = {
                String.valueOf(rowNum),
                clip(ctx.regular, 9, String.valueOf(item.getOrDefault("productName", "")), COL_W[1] - 8),
                fmtQty(item.get("quantitySnapshot")),
                fmtMoney(item.get("unitPriceSnapshot")),
                fmtMoney(item.get("insuranceCoveredAmount")),
                fmtMoney(item.get("patientPayableAmount")),
                fmtMoney(item.get("lineTotal"))
        };

        float x = MARGIN;
        for (int i = 0; i < vals.length; i++) {
            if (i > 0) lineRgb(ctx, x, topY, x, topY - ROW_H, C_BORDER, 0.3f);
            boolean numCol = i > 1;
            ink(ctx, C_DARK_TXT);
            if (numCol) {
                putTextRight(ctx, ctx.regular, 9, x + COL_W[i] - 5, topY - 13, vals[i]);
            } else {
                putText(ctx, ctx.regular, 9, x + (i == 0 ? 7 : 4), topY - 13, vals[i]);
            }
            x += COL_W[i];
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  SECTION 4 — TOTALS BOX
    // ═══════════════════════════════════════════════════════════════════════════

    private static void drawTotalsSection(Ctx ctx, DepartmentInsuranceBilling b) throws IOException {
        List<TotLine> lines = buildTotals(b);
        float boxW = 228f;
        float lineH = 17f;
        float boxH  = lines.size() * lineH + 16f;
        float boxX  = PW - MARGIN - boxW;

        space(ctx, boxH + 16, false);
        fillRgb(ctx, boxX, ctx.y - boxH, boxW, boxH, C_TOT_BG);
        strokeRgb(ctx, boxX, ctx.y - boxH, boxW, boxH, C_BORDER, 0.6f);

        float ly = ctx.y - 10f;
        for (int i = 0; i < lines.size(); i++) {
            TotLine tl = lines.get(i);
            // Separator before final "outstanding" line
            if (i == lines.size() - 1 && lines.size() > 1) {
                lineRgb(ctx, boxX + 6, ly + 4, boxX + boxW - 6, ly + 4, C_BORDER, 0.5f);
                ly -= 3;
            }
            PDType0Font lFont = tl.bold ? ctx.bold : ctx.regular;
            float lSize = tl.bold ? 10.5f : 10f;
            ink(ctx, tl.labelColor);
            putText(ctx, lFont, lSize, boxX + 8, ly, tl.label + ":");
            ink(ctx, tl.valueColor);
            putTextRight(ctx, ctx.bold, lSize, boxX + boxW - 7, ly, tl.value);
            ink(ctx, C_DARK_TXT);
            ly -= lineH;
        }

        ctx.y -= boxH + 8;
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

    private static void drawFooter(Ctx ctx) throws IOException {
        float fy = BOT - 12f;
        lineRgb(ctx, MARGIN, fy + 16, PW - MARGIN, fy + 16, C_BORDER, 0.8f);
        ink(ctx, C_MID_TXT);
        putText(ctx,   ctx.regular, 9, MARGIN,        fy, "Thank you for choosing our services.");
        putTextRight(ctx, ctx.regular, 9, PW - MARGIN, fy, "Page " + ctx.page);
        ink(ctx, C_DARK_TXT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PAGINATION — ensure space, add new page when needed
    // ═══════════════════════════════════════════════════════════════════════════

    private static void space(Ctx ctx, float needed, boolean isTableStart) throws IOException {
        if (ctx.y - needed >= BOT) return;

        ctx.cs.close();
        ctx.page++;
        PDPage pg = new PDPage(PDRectangle.LETTER);
        ctx.doc.addPage(pg);
        ctx.cs = new PDPageContentStream(ctx.doc, pg);
        ctx.y  = PH - MARGIN;

        // Compact continuation header
        ink(ctx, C_PRIMARY);
        putText(ctx, ctx.bold, 11, MARGIN, ctx.y, "Invoice (continued)");
        ink(ctx, C_MID_TXT);
        putTextRight(ctx, ctx.regular, 9, PW - MARGIN, ctx.y, "Page " + ctx.page);
        ctx.y -= 16;
        lineRgb(ctx, MARGIN, ctx.y, PW - MARGIN, ctx.y, C_PRIMARY, 1.2f);
        ctx.y -= 10;
        ink(ctx, C_DARK_TXT);

        // Redraw table header on continuation pages (unless this call IS the table start)
        if (!isTableStart) {
            drawTableHeader(ctx, ctx.y);
            ctx.y -= HDR_ROW_H;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  DRAWING PRIMITIVES
    // ═══════════════════════════════════════════════════════════════════════════

    /** Fill a rectangle. (x,y) = bottom-left in PDF coords. */
    private static void fillRgb(Ctx ctx, float x, float y, float w, float h, float[] rgb) throws IOException {
        ctx.cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.addRect(x, y, w, h);
        ctx.cs.fill();
        ctx.cs.setNonStrokingColor(0, 0, 0);
    }

    /** Stroke (outline) a rectangle. (x,y) = bottom-left. */
    private static void strokeRgb(Ctx ctx, float x, float y, float w, float h, float[] rgb, float lw) throws IOException {
        ctx.cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.setLineWidth(lw);
        ctx.cs.addRect(x, y, w, h);
        ctx.cs.stroke();
        ctx.cs.setStrokingColor(0, 0, 0);
    }

    /** Draw a straight line. */
    private static void lineRgb(Ctx ctx, float x1, float y1, float x2, float y2, float[] rgb, float lw) throws IOException {
        ctx.cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
        ctx.cs.setLineWidth(lw);
        ctx.cs.moveTo(x1, y1);
        ctx.cs.lineTo(x2, y2);
        ctx.cs.stroke();
        ctx.cs.setStrokingColor(0, 0, 0);
    }

    /** Full-width horizontal rule that also decrements ctx.y. */
    private static void thickLine(Ctx ctx, float[] rgb, float lw) throws IOException {
        lineRgb(ctx, MARGIN, ctx.y, PW - MARGIN, ctx.y, rgb, lw);
        ctx.y -= 2;
    }

    /** Set the current non-stroking (text-fill) colour. */
    private static void ink(Ctx ctx, float[] rgb) throws IOException {
        ctx.cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    }

    /** Write text at (x, y). Does NOT advance ctx.y. */
    private static void putText(Ctx ctx, PDType0Font font, float size, float x, float y, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        ctx.cs.beginText();
        ctx.cs.setFont(font, size);
        ctx.cs.newLineAtOffset(x, y);
        ctx.cs.showText(text);
        ctx.cs.endText();
    }

    /** Write text right-aligned to rightEdge. Does NOT advance ctx.y. */
    private static void putTextRight(Ctx ctx, PDType0Font font, float size, float rightEdge, float y, String text) throws IOException {
        if (text == null || text.isEmpty()) return;
        float w = tw(font, size, text);
        putText(ctx, font, size, rightEdge - w, y, text);
    }

    /** Centre text horizontally and vertically within a rectangle (rx, ry = bottom-left). */
    private static void textCentredInRect(Ctx ctx, PDType0Font font, float size,
                                          float rx, float ry, float rw, float rh,
                                          String text, float[] rgb) throws IOException {
        float tw = tw(font, size, text);
        float tx = rx + (rw - tw) / 2f;
        float ty = ry + (rh - size) / 2f;
        ink(ctx, rgb);
        putText(ctx, font, size, tx, ty, text);
        ink(ctx, C_DARK_TXT);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  TEXT / DATA HELPERS
    // ═══════════════════════════════════════════════════════════════════════════

    /** Measure rendered text width in points. */
    private static float tw(PDType0Font font, float size, String text) {
        if (text == null || text.isEmpty()) return 0;
        try {
            return font.getStringWidth(text) / 1000f * size;
        } catch (IOException e) {
            return text.length() * size * 0.5f;
        }
    }

    /** Clip text to maxWidth, appending "…" if truncated. */
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
    //  RENDERING CONTEXT
    // ═══════════════════════════════════════════════════════════════════════════

    private static final class Ctx {
        final PDDocument        doc;
        final PDType0Font       regular;
        final PDType0Font       bold;
        PDPageContentStream     cs;
        float                   y;
        int                     page = 1;

        Ctx(PDDocument doc, PDType0Font regular, PDType0Font bold) throws IOException {
            this.doc     = doc;
            this.regular = regular;
            this.bold    = bold;
            PDPage pg = new PDPage(PDRectangle.LETTER);
            doc.addPage(pg);
            this.cs = new PDPageContentStream(doc, pg);
            this.y  = PH - MARGIN;
        }
    }
}
