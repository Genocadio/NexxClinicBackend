package com.nexxserve.nexxclinic.service;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.util.Locale;
import java.util.Map;

/**
 * Predefined paper sizes for invoice PDF generation.
 *
 * <p>Each variant carries its own {@link PDRectangle}, margins, and a scale factor
 * relative to the reference layout (US Letter, 612 × 792 pt). The scale factor is
 * used by {@link InvoicePdfGenerator} to proportionally resize every visual element
 * — fonts, column widths, row heights, margins, badges, and the totals box — so the
 * invoice always renders cleanly without overlap or clipping.
 *
 * <p>Configuration via {@code billing.invoice.paper-size} in application.yaml:
 * <ul>
 *   <li>{@code letter}  — US Letter portrait (8.5″ × 11″), the default</li>
 *   <li>{@code a4p}     — A4 portrait (210 × 297 mm)</li>
 *   <li>{@code a4l}     — A4 landscape (297 × 210 mm)</li>
 *   <li>{@code pos}     — 80 mm thermal receipt paper</li>
 * </ul>
 */
public enum PaperSize {

    /** US Letter portrait — 8.5″ × 11″ (reference layout, scale = 1.0). */
    LETTER("letter", PDRectangle.LETTER, 40f, 65f),

    /** A4 portrait — 210 × 297 mm. Slightly wider & taller than Letter. */
    A4P("a4p", PDRectangle.A4, 38f, 62f),

    /** A4 landscape — 297 × 210 mm. Much wider, generous horizontal space. */
    A4L("a4l",
        new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth()),
        44f, 58f),

    /** 80 mm thermal receipt paper (POS). Narrow, single-column friendly. */
    POS("pos", new PDRectangle(226.77f, 841.89f), 12f, 40f);

    private static final float REF_CW = 532f; // reference content width (LETTER)

    private static final Map<String, PaperSize> LOOKUP = Map.ofEntries(
        Map.entry(LETTER.key, LETTER),
        Map.entry(A4P.key, A4P),
        Map.entry(A4L.key, A4L),
        Map.entry(POS.key, POS)
    );

    private final String key;
    private final PDRectangle rectangle;
    private final float margin;
    private final float bottomMargin;
    private final float scale; // content-width ratio vs. reference LETTER

    PaperSize(String key, PDRectangle rectangle, float margin, float bottomMargin) {
        this.key          = key;
        this.rectangle    = rectangle;
        this.margin       = margin;
        this.bottomMargin = bottomMargin;
        float cw = rectangle.getWidth() - margin * 2;
        this.scale = cw / REF_CW;
    }

    /** Case-insensitive lookup; falls back to {@link #LETTER} for unknown keys. */
    public static PaperSize from(String value) {
        if (value == null || value.isBlank()) return LETTER;
        return LOOKUP.getOrDefault(value.trim().toLowerCase(Locale.ROOT), LETTER);
    }

    public String       key()          { return key; }
    public PDRectangle  rectangle()    { return rectangle; }
    public float        pageWidth()    { return rectangle.getWidth(); }
    public float        pageHeight()   { return rectangle.getHeight(); }
    public float        margin()       { return margin; }
    public float        contentWidth() { return pageWidth() - margin * 2; }
    public float        bottomMargin() { return bottomMargin; }
    public float        scale()        { return scale; }

    /** Scale a dimension that was designed for the LETTER reference layout. */
    public float scale(float value) {
        return Math.round(value * scale * 10f) / 10f; // round to 1 decimal
    }

    /** Scale a font size, enforcing a minimum of 5pt for readability. */
    public float scaleFont(float size) {
        return Math.max(5f, scale(size));
    }
}
