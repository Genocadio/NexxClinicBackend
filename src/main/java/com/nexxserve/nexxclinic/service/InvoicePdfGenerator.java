package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.entity.VisitBilling;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

public final class InvoicePdfGenerator {

    private InvoicePdfGenerator() {
        // utility class
    }

    public static void createInvoicePdf(Path invoiceFile, VisitBilling billing, List<Map<String, Object>> items) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;
                float leading = 16;
                PDType0Font fallbackFont = null;
                try (var fontStream = InvoicePdfGenerator.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {
                    if (fontStream != null) {
                        fallbackFont = PDType0Font.load(document, fontStream);
                    }
                } catch (Exception ignore) {
                }

                content.beginText();
                if (fallbackFont != null) {
                    content.setFont(fallbackFont, 20);
                } else {
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
                }
                content.newLineAtOffset(margin, y);
                content.showText("Invoice");
                content.endText();

                y -= leading * 2;
                content.beginText();
                if (fallbackFont != null) {
                    content.setFont(fallbackFont, 12);
                } else {
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                }
                content.newLineAtOffset(margin, y);
                content.showText("Invoice ID: " + billing.getId());
                content.endText();

                y -= leading;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Visit ID: " + billing.getVisit().getId());
                content.endText();

                y -= leading;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Billing Date: " + billing.getBillingDate());
                content.endText();

                y -= leading;
                String patientName = billing.getVisit().getPatient() == null ? "Unknown" : (
                        billing.getVisit().getPatient().getFirstName() +
                                (billing.getVisit().getPatient().getLastName() == null ? "" : " " + billing.getVisit().getPatient().getLastName())
                );
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Patient: " + patientName);
                content.endText();

                y -= leading * 2;
                content.beginText();
                if (fallbackFont != null) {
                    content.setFont(fallbackFont, 12);
                } else {
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                }
                content.newLineAtOffset(margin, y);
                content.showText(String.format("%-40s %8s %12s %12s", "Product", "Qty", "Unit", "Total"));
                content.endText();

                y -= leading;
                if (fallbackFont != null) {
                    content.setFont(fallbackFont, 12);
                } else {
                    content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                }
                for (Map<String, Object> item : items) {
                    if (y < margin + leading * 5) {
                        break;
                    }
                    content.beginText();
                    content.newLineAtOffset(margin, y);
                    String productName = item.get("productName").toString();
                    String line = String.format("%-40.40s %8s %12s %12s",
                            productName,
                            item.get("quantitySnapshot"),
                            item.get("unitPriceSnapshot"),
                            item.get("lineTotal"));
                    content.showText(line);
                    content.endText();
                    y -= leading;
                }

                y -= leading;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Total: " + billing.getTotalAmount());
                content.endText();

                y -= leading;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Insurance Covered: " + billing.getInsuranceCoveredAmount());
                content.endText();

                y -= leading;
                content.beginText();
                content.newLineAtOffset(margin, y);
                content.showText("Patient Payable: " + billing.getPatientPayableAmount());
                content.endText();
            }
            document.save(invoiceFile.toFile());
        }
    }
}
