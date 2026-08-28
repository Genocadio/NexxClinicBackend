package com.nexxserve.nexxclinic.service.invoice;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders invoice PDFs from Thymeleaf templates using Chromium's built-in
 * {@code --print-to-pdf} capability via ProcessBuilder.
 *
 * <p>This approach avoids the Playwright Java SDK driver installation issues
 * in Docker. Chromium is pre-installed in the Docker image at
 * {@code /usr/local/bin/chromium} and launched directly.
 *
 * <p>Three paper layouts are supported via the same shared invoice fragment:
 * <ul>
 *   <li>{@code pos} — 80mm thermal receipt roll</li>
 *   <li>{@code a4p} — A4 portrait, one invoice per sheet</li>
 *   <li>{@code a4l} — A4 landscape, two invoices per sheet (left/right halves)</li>
 * </ul>
 */
@Service
public class InvoicePdfRenderer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfRenderer.class);

    private final TemplateEngine templateEngine;

    /** Resolved path to the system Chromium binary (lazily detected). */
    private String chromiumPath;

    public InvoicePdfRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  PUBLIC API
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Render a single-invoice layout (POS or A4 portrait).
     *
     * @param billing   the invoice view model
     * @param paperSize "pos" or "a4p"
     * @return the rendered PDF bytes
     */
    public byte[] renderSingle(InvoiceView billing, String paperSize) {
        String template = switch (paperSize) {
            case "pos" -> "invoice/pos";
            case "a4l" -> "invoice/a4l";
            default    -> "invoice/a4p";
        };

        Map<String, Object> model = switch (paperSize) {
            case "a4l" -> Map.of("billings", List.of(billing));
            default    -> Map.of("billing", billing);
        };

        String html = templateEngine.process(template, new Context(java.util.Locale.getDefault(), model));
        return renderHtmlToPdf(html, paperSize);
    }

    /**
     * Render a multi-invoice A4 landscape layout (two invoices per sheet).
     *
     * @param billings  list of invoice view models
     * @return the rendered PDF bytes
     */
    public byte[] renderA4L(List<InvoiceView> billings) {
        String html = templateEngine.process("invoice/a4l",
                new Context(java.util.Locale.getDefault(), Map.of("billings", billings)));
        return renderHtmlToPdf(html, "a4l");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  INTERNAL — Chromium --print-to-pdf rendering
    // ═══════════════════════════════════════════════════════════════════════════

    private byte[] renderHtmlToPdf(String html, String paperSize) {
        Path tempHtml = null;
        Path tempPdf = null;
        try {
            // Write HTML to a temp file
            tempHtml = Files.createTempFile("invoice-", ".html");
            Files.writeString(tempHtml, html, StandardCharsets.UTF_8);

            // Determine output PDF path
            tempPdf = Files.createTempFile("invoice-", ".pdf");

            // Build Chromium arguments
            List<String> command = new ArrayList<>();
            command.add(resolveChromiumPath());
            command.add("--headless");
            command.add("--no-sandbox");
            command.add("--disable-setuid-sandbox");
            command.add("--disable-dev-shm-usage");
            command.add("--disable-gpu");
            command.add("--run-all-compositor-stages-before-draw");
            command.add("--print-to-pdf=" + tempPdf.toAbsolutePath());

            // Paper size arguments
            switch (paperSize) {
                case "pos" -> {
                    command.add("--print-to-pdf-no-header");
                    // 80mm width in pixels at 96dpi ≈ 302px; Chromium uses --paper-width
                    command.add("--paper-width=3.15");   // ~80mm in inches
                    command.add("--paper-height=11.69");  // auto (A4 height as max)
                }
                case "a4l" -> {
                    command.add("--print-to-pdf-no-header");
                    // A4 landscape: 297mm x 210mm
                    command.add("--paper-width=11.69");   // 297mm in inches
                    command.add("--paper-height=8.27");   // 210mm in inches
                }
                default -> {
                    // A4 portrait: 210mm x 297mm (Chromium default)
                    command.add("--print-to-pdf-no-header");
                }
            }

            // Add the HTML file URL
            command.add(tempHtml.toUri().toString());

            log.debug("Running Chromium for PDF rendering: paper={}", paperSize);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read output for debugging
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                throw new RuntimeException("Chromium PDF rendering timed out after 30s");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                log.error("Chromium exited with code {} for paper={}: {}", exitCode, paperSize, output);
                throw new RuntimeException("Chromium PDF rendering failed with exit code " + exitCode);
            }

            byte[] pdfBytes = Files.readAllBytes(tempPdf);
            log.debug("Rendered invoice PDF: {} bytes (paper={})", pdfBytes.length, paperSize);
            return pdfBytes;

        } catch (Exception e) {
            log.error("Invoice PDF rendering failed for paper={}: {}", paperSize, e.getMessage(), e);
            throw new RuntimeException("Invoice PDF rendering failed: " + e.getMessage(), e);
        } finally {
            // Clean up temp files
            safeDelete(tempHtml);
            safeDelete(tempPdf);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CHROMIUM PATH DETECTION
    // ═══════════════════════════════════════════════════════════════════════════

    private synchronized String resolveChromiumPath() {
        if (chromiumPath != null) return chromiumPath;

        String[] candidates = {
            "/usr/local/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
        };

        for (String candidate : candidates) {
            Path path = Path.of(candidate);
            if (Files.isExecutable(path)) {
                chromiumPath = candidate;
                log.info("Using system Chromium at: {}", candidate);
                return chromiumPath;
            }
        }

        // Fallback: try `which` command
        for (String name : new String[]{"chromium", "chromium-browser", "google-chrome"}) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"which", name});
                int exit = p.waitFor();
                if (exit == 0) {
                    String resolved = new String(p.getInputStream().readAllBytes()).trim();
                    if (!resolved.isEmpty() && Files.isExecutable(Path.of(resolved))) {
                        chromiumPath = resolved;
                        log.info("Resolved Chromium via 'which {}': {}", name, resolved);
                        return chromiumPath;
                    }
                }
            } catch (Exception ignored) {}
        }

        throw new RuntimeException(
            "No Chromium binary found. Ensure Chromium is installed in the Docker image " +
            "(check /usr/local/bin/chromium or install via apt-get)."
        );
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  CLEANUP
    // ═══════════════════════════════════════════════════════════════════════════

    private void safeDelete(Path path) {
        if (path != null) {
            try { Files.deleteIfExists(path); } catch (Exception ignored) {}
        }
    }

    @Override
    public void close() {
        // No persistent browser process to clean up — each render spawns
        // a fresh Chromium process that exits on its own.
        log.info("InvoicePdfRenderer closed (no persistent resources)");
    }
}
