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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
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

public final class InvoicePdfGenerator {

    private static final String FONT_REGULAR = "/fonts/DejaVuSans.ttf";
    private static final String FONT_BOLD = "/fonts/DejaVuSans-Bold.ttf";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");

    private static final float MARGIN = 40f;
    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float CONTENT_WIDTH = PAGE_WIDTH - (MARGIN * 2);
    private static final float ROW_HEIGHT = 20f;
    private static final float HEADER_ROW_HEIGHT = 24f;
    private static final float BOTTOM_MARGIN = 60f;

    private static final String[] TABLE_HEADERS = {
            "Product", "Qty", "Unit Price", "Insurance", "Patient", "Total"
    };
    private static final float[] COLUMN_WIDTHS = {190f, 42f, 68f, 68f, 68f, 68f};

    private InvoicePdfGenerator() {
    }

    public static void createInvoicePdf(
            Path invoiceFile,
            DepartmentInsuranceBilling billing,
            List<Map<String, Object>> items,
            ClinicProfile clinicProfile
    ) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font regular = loadFont(document, FONT_REGULAR);
            PDType0Font bold = loadFont(document, FONT_BOLD);

            PageContext context = new PageContext(document, regular, bold, addPage(document));
            drawHeader(context, clinicProfile);
            drawMetaSection(context, billing);
            drawItemsTable(context, items);
            drawTotalsSection(context, billing);

            context.content.close();
            document.save(invoiceFile.toFile());
        }
    }

    private static void drawHeader(PageContext context, ClinicProfile clinicProfile) throws IOException {
        String clinicName = clinicProfile == null ? null : clinicProfile.getName();
        if (!hasText(clinicName)) {
            clinicName = "Clinic Invoice";
        }

        context.y = writeText(context, context.bold, 18, MARGIN, context.y, clinicName);
        writeTextRight(context, context.bold, 18, PAGE_WIDTH - MARGIN, context.y + 18, "INVOICE");
        context.y -= 6;

        if (clinicProfile != null && hasText(clinicProfile.getAddress())) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, clinicProfile.getAddress());
        }

        List<String> detailLines = buildClinicDetailLines(clinicProfile);
        for (String line : detailLines) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, line);
        }

        context.y -= 8;
        drawHorizontalRule(context, context.y);
        context.y -= 14;
    }

    private static List<String> buildClinicDetailLines(ClinicProfile clinicProfile) {
        List<String> lines = new ArrayList<>();
        if (clinicProfile == null) {
            return lines;
        }

        if (hasText(clinicProfile.getTinNumber())) {
            lines.add("TIN: " + clinicProfile.getTinNumber().trim());
        }

        List<String> contactParts = new ArrayList<>();
        if (clinicProfile.getContacts() != null) {
            for (ClinicContact contact : clinicProfile.getContacts()) {
                if (contact == null || !hasText(contact.getValue())) {
                    continue;
                }
                String label = formatContactLabel(contact.getContactType());
                if (label == null) {
                    continue;
                }
                contactParts.add(label + ": " + contact.getValue().trim());
            }
        }

        if (!contactParts.isEmpty()) {
            lines.add(String.join("   |   ", contactParts));
        }

        return lines;
    }

    private static String formatContactLabel(ClinicContactType contactType) {
        if (contactType == null) {
            return null;
        }
        return switch (contactType) {
            case PHONE -> "Phone";
            case EMAIL -> "Email";
            case POBOX -> "P.O. Box";
        };
    }

    private static void drawMetaSection(PageContext context, DepartmentInsuranceBilling billing) throws IOException {
        Visit visit = resolveVisit(billing);
        Patient patient = visit == null ? null : visit.getPatient();
        VisitDepartment visitDepartment = billing.getVisitDepartmentBilling() == null
                ? null
                : billing.getVisitDepartmentBilling().getVisitDepartment();

        context.y = writeText(context, context.bold, 11, MARGIN, context.y, "Bill To");
        context.y = writeText(context, context.regular, 10, MARGIN, context.y, "Patient: " + formatPatientName(patient));

        if (visit != null && visit.getVisitDate() != null) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, "Visit Date: " + DATE_FORMAT.format(visit.getVisitDate()));
        }

        LocalDateTime billingDate = billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null
                ? null
                : billing.getVisitDepartmentBilling().getVisitBilling().getCreatedAt();
        if (billingDate != null) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, "Invoice Date: " + DATETIME_FORMAT.format(billingDate));
        }

        context.y = writeText(context, context.regular, 10, MARGIN, context.y, "Invoice No: " + billing.getId());

        if (visitDepartment != null && visitDepartment.getDepartment() != null && hasText(visitDepartment.getDepartment().getName())) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, "Department: " + visitDepartment.getDepartment().getName());
        }

        String insuranceLine = formatInsuranceLine(billing.getPatientInsurance());
        if (insuranceLine != null) {
            context.y = writeText(context, context.regular, 10, MARGIN, context.y, insuranceLine);
        }

        context.y -= 10;
    }

    private static String formatInsuranceLine(PatientInsurance insurance) {
        if (insurance == null) {
            return null;
        }

        InsuranceProvider provider = insurance.getInsuranceProvider();
        String providerName = provider == null ? null : provider.getInsuranceName();
        if (!hasText(providerName) && provider != null && hasText(provider.getAcronym())) {
            providerName = provider.getAcronym();
        }

        if (!hasText(providerName) && !hasText(insurance.getInsuranceCardNumber())) {
            return null;
        }

        StringBuilder builder = new StringBuilder("Insurance: ");
        if (hasText(providerName)) {
            builder.append(providerName.trim());
        }
        if (hasText(insurance.getInsuranceCardNumber())) {
            if (builder.length() > "Insurance: ".length()) {
                builder.append(" (");
                builder.append(insurance.getInsuranceCardNumber().trim());
                builder.append(")");
            } else {
                builder.append(insurance.getInsuranceCardNumber().trim());
            }
        }
        return builder.toString();
    }

    private static void drawItemsTable(PageContext context, List<Map<String, Object>> items) throws IOException {
        ensureSpace(context, HEADER_ROW_HEIGHT + ROW_HEIGHT);
        float tableTop = context.y;
        float tableLeft = MARGIN;

        drawTableHeader(context, tableTop, tableLeft);
        context.y = tableTop - HEADER_ROW_HEIGHT;

        int rowIndex = 0;
        for (Map<String, Object> item : items) {
            ensureSpace(context, ROW_HEIGHT);
            drawTableRow(context, tableLeft, context.y, item, rowIndex % 2 == 1);
            context.y -= ROW_HEIGHT;
            rowIndex++;
        }

        drawTableOuterBorder(context, tableTop, tableLeft, tableTop - context.y);
        context.y -= 12;
    }

    private static void drawTableHeader(PageContext context, float topY, float leftX) throws IOException {
        float x = leftX;
        fillRect(context, leftX, topY - HEADER_ROW_HEIGHT, CONTENT_WIDTH, HEADER_ROW_HEIGHT, 0.93f);
        strokeRect(context, leftX, topY - HEADER_ROW_HEIGHT, CONTENT_WIDTH, HEADER_ROW_HEIGHT);

        for (int i = 0; i < TABLE_HEADERS.length; i++) {
            writeTextInCell(context, context.bold, 9, x + 4, topY - 16, TABLE_HEADERS[i], COLUMN_WIDTHS[i] - 8);
            if (i > 0) {
                drawVerticalLine(context, x, topY, topY - HEADER_ROW_HEIGHT);
            }
            x += COLUMN_WIDTHS[i];
        }
    }

    private static void drawTableRow(
            PageContext context,
            float leftX,
            float topY,
            Map<String, Object> item,
            boolean shaded
    ) throws IOException {
        if (shaded) {
            fillRect(context, leftX, topY - ROW_HEIGHT, CONTENT_WIDTH, ROW_HEIGHT, 0.97f);
        }
        strokeRect(context, leftX, topY - ROW_HEIGHT, CONTENT_WIDTH, ROW_HEIGHT);

        String[] values = {
                truncate(String.valueOf(item.get("productName")), 34),
                formatNumber(item.get("quantitySnapshot")),
                formatMoney(item.get("unitPriceSnapshot")),
                formatMoney(item.get("insuranceCoveredAmount")),
                formatMoney(item.get("patientPayableAmount")),
                formatMoney(item.get("lineTotal"))
        };

        float x = leftX;
        for (int i = 0; i < values.length; i++) {
            boolean rightAlign = i > 0;
            if (rightAlign) {
                writeTextRightInCell(context, context.regular, 9, x, topY - 14, values[i], COLUMN_WIDTHS[i] - 6);
            } else {
                writeTextInCell(context, context.regular, 9, x + 4, topY - 14, values[i], COLUMN_WIDTHS[i] - 8);
            }
            if (i > 0) {
                drawVerticalLine(context, x, topY, topY - ROW_HEIGHT);
            }
            x += COLUMN_WIDTHS[i];
        }
    }

    private static void drawTableOuterBorder(PageContext context, float topY, float leftX, float height) throws IOException {
        strokeRect(context, leftX, topY - height, CONTENT_WIDTH, height);
    }

    private static void drawTotalsSection(PageContext context, DepartmentInsuranceBilling billing) throws IOException {
        float labelX = PAGE_WIDTH - MARGIN - 180f;
        float valueX = PAGE_WIDTH - MARGIN;

        context.y = writeTotalLine(context, labelX, valueX, context.y, "Total Amount", billing.getTotalAmount());
        context.y = writeTotalLine(context, labelX, valueX, context.y, "Insurance Covered", billing.getInsuranceCoveredAmount());
        context.y = writeTotalLine(context, labelX, valueX, context.y, "Patient Payable", billing.getPatientPayableAmount());
        context.y = writeTotalLine(context, labelX, valueX, context.y, "Paid", billing.getPaidAmount());
        context.y = writeTotalLine(context, labelX, valueX, context.y, "Outstanding", billing.getOutstandingAmount());

        context.y -= 6;
        drawHorizontalRule(context, context.y);
        context.y -= 12;
        writeText(context, context.regular, 9, MARGIN, context.y, "Thank you for choosing our services.");
    }

    private static float writeTotalLine(
            PageContext context,
            float labelX,
            float valueX,
            float y,
            String label,
            BigDecimal amount
    ) throws IOException {
        writeText(context, context.regular, 10, labelX, y, label + ":");
        writeTextRight(context, context.bold, 10, valueX, y, formatMoney(amount));
        return y - 16;
    }

    private static void ensureSpace(PageContext context, float requiredHeight) throws IOException {
        if (context.y - requiredHeight >= BOTTOM_MARGIN) {
            return;
        }

        context.content.close();
        context.page = addPage(context.document);
        context.content = new PDPageContentStream(context.document, context.page);
        context.y = PAGE_HEIGHT - MARGIN - 20;
        drawTableHeader(context, context.y, MARGIN);
        context.y -= HEADER_ROW_HEIGHT;
    }

    private static PDPage addPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.LETTER);
        document.addPage(page);
        return page;
    }

    private static void drawHorizontalRule(PageContext context, float y) throws IOException {
        context.content.setLineWidth(0.5f);
        context.content.moveTo(MARGIN, y);
        context.content.lineTo(PAGE_WIDTH - MARGIN, y);
        context.content.stroke();
    }

    private static void fillRect(PageContext context, float x, float y, float width, float height, float gray) throws IOException {
        context.content.setNonStrokingColor(gray, gray, gray);
        context.content.addRect(x, y, width, height);
        context.content.fill();
        context.content.setNonStrokingColor(0, 0, 0);
    }

    private static void strokeRect(PageContext context, float x, float y, float width, float height) throws IOException {
        context.content.setLineWidth(0.5f);
        context.content.addRect(x, y, width, height);
        context.content.stroke();
    }

    private static void drawVerticalLine(PageContext context, float x, float topY, float bottomY) throws IOException {
        context.content.setLineWidth(0.5f);
        context.content.moveTo(x, topY);
        context.content.lineTo(x, bottomY);
        context.content.stroke();
    }

    private static float writeText(PageContext context, PDType0Font font, float size, float x, float y, String text)
            throws IOException {
        context.content.beginText();
        context.content.setFont(font, size);
        context.content.newLineAtOffset(x, y);
        context.content.showText(text == null ? "" : text);
        context.content.endText();
        return y - (size + 4);
    }

    private static void writeTextRight(PageContext context, PDType0Font font, float size, float rightX, float y, String text)
            throws IOException {
        String value = text == null ? "" : text;
        float textWidth = font.getStringWidth(value) / 1000f * size;
        writeText(context, font, size, rightX - textWidth, y, value);
    }

    private static void writeTextInCell(
            PageContext context,
            PDType0Font font,
            float size,
            float x,
            float y,
            String text,
            float maxWidth
    ) throws IOException {
        writeText(context, font, size, x, y, truncateToWidth(font, size, text, maxWidth));
    }

    private static void writeTextRightInCell(
            PageContext context,
            PDType0Font font,
            float size,
            float cellX,
            float y,
            String text,
            float cellWidth
    ) throws IOException {
        String value = text == null ? "" : text;
        float textWidth = font.getStringWidth(value) / 1000f * size;
        float x = cellX + cellWidth - textWidth;
        writeText(context, font, size, x, y, value);
    }

    private static String truncateToWidth(PDType0Font font, float size, String text, float maxWidth) throws IOException {
        if (text == null) {
            return "";
        }
        if (font.getStringWidth(text) / 1000f * size <= maxWidth) {
            return text;
        }
        String ellipsis = "...";
        String trimmed = text;
        while (!trimmed.isEmpty()
                && font.getStringWidth(trimmed + ellipsis) / 1000f * size > maxWidth) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? ellipsis : trimmed + ellipsis;
    }

    private static String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, Math.max(0, maxChars - 3)) + "...";
    }

    private static String formatMoney(Object value) {
        if (value == null) {
            return "0.00";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
        }
        return String.valueOf(value);
    }

    private static String formatNumber(Object value) {
        if (value == null) {
            return "0";
        }
        if (value instanceof BigDecimal decimal) {
            return decimal.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private static Visit resolveVisit(DepartmentInsuranceBilling billing) {
        if (billing.getVisitDepartmentBilling() == null
                || billing.getVisitDepartmentBilling().getVisitBilling() == null) {
            return null;
        }
        return billing.getVisitDepartmentBilling().getVisitBilling().getVisit();
    }

    private static String formatPatientName(Patient patient) {
        if (patient == null) {
            return "Unknown";
        }
        if (hasText(patient.getFullName())) {
            return patient.getFullName().trim();
        }
        String firstName = patient.getFirstName() == null ? "" : patient.getFirstName().trim();
        String lastName = patient.getLastName() == null ? "" : patient.getLastName().trim();
        String combined = (firstName + " " + lastName).trim();
        return combined.isEmpty() ? "Unknown" : combined;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static PDType0Font loadFont(PDDocument document, String resourcePath) throws IOException {
        try (InputStream fontStream = InvoicePdfGenerator.class.getResourceAsStream(resourcePath)) {
            if (fontStream == null) {
                throw new IOException("Missing bundled invoice font: " + resourcePath);
            }
            byte[] fontBytes = fontStream.readAllBytes();
            return PDType0Font.load(document, new ByteArrayInputStream(fontBytes));
        }
    }

    private static final class PageContext {
        private final PDDocument document;
        private final PDType0Font regular;
        private final PDType0Font bold;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        private PageContext(PDDocument document, PDType0Font regular, PDType0Font bold, PDPage page) throws IOException {
            this.document = document;
            this.regular = regular;
            this.bold = bold;
            this.page = page;
            this.content = new PDPageContentStream(document, page);
            this.y = PAGE_HEIGHT - MARGIN;
        }
    }
}
