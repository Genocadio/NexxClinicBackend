package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.VisitBilling;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter BILLING_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private InvoicePdfGenerator() {
        // utility class
    }

    public static void createInvoicePdf(Path invoiceFile, VisitBilling billing, List<Map<String, Object>> items)
            throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType0Font regular = loadFont(document, FONT_REGULAR);
            PDType0Font bold = loadFont(document, FONT_BOLD);

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            float margin = 50;
            float leading = 16;
            float y = page.getMediaBox().getHeight() - margin;

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                y = writeLine(content, bold, 20, margin, y, "Invoice");
                y -= leading;

                y = writeLine(content, regular, 12, margin, y, "Invoice ID: " + billing.getId());
                y = writeLine(content, regular, 12, margin, y, "Visit ID: " + billing.getVisit().getId());

                String billingDate = billing.getBillingDate() == null
                        ? ""
                        : BILLING_DATE_FORMAT.format(billing.getBillingDate());
                y = writeLine(content, regular, 12, margin, y, "Billing Date: " + billingDate);
                y = writeLine(content, regular, 12, margin, y, "Patient: " + formatPatientName(billing));

                y -= leading;
                y = writeLine(
                        content,
                        bold,
                        12,
                        margin,
                        y,
                        String.format("%-40s %8s %12s %12s", "Product", "Qty", "Unit", "Total"));

                for (Map<String, Object> item : items) {
                    if (y < margin + leading * 5) {
                        break;
                    }
                    String productName = String.valueOf(item.get("productName"));
                    String line = String.format(
                            "%-40.40s %8s %12s %12s",
                            productName,
                            item.get("quantitySnapshot"),
                            item.get("unitPriceSnapshot"),
                            item.get("lineTotal"));
                    y = writeLine(content, regular, 12, margin, y, line);
                }

                y -= leading;
                y = writeLine(content, regular, 12, margin, y, "Total: " + billing.getTotalAmount());
                y = writeLine(content, regular, 12, margin, y, "Insurance Covered: " + billing.getInsuranceCoveredAmount());
                writeLine(content, regular, 12, margin, y, "Patient Payable: " + billing.getPatientPayableAmount());
            }

            document.save(invoiceFile.toFile());
        }
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

    private static float writeLine(
            PDPageContentStream content,
            PDType0Font font,
            float fontSize,
            float x,
            float y,
            String text)
            throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
        return y - 16;
    }

    private static String formatPatientName(VisitBilling billing) {
        if (billing.getVisit() == null || billing.getVisit().getPatient() == null) {
            return "Unknown";
        }
        var patient = billing.getVisit().getPatient();
        String lastName = patient.getLastName() == null ? "" : " " + patient.getLastName();
        return patient.getFirstName() + lastName;
    }
}
