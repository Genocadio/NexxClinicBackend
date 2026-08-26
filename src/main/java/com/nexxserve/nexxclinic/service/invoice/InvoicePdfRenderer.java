package com.nexxserve.nexxclinic.service.invoice;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.PlaywrightException;
import com.microsoft.playwright.options.Margin;
import jakarta.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Renders invoice PDFs from Thymeleaf templates using Playwright's headless Chromium.
 *
 * <p>Three paper layouts are supported via the same shared invoice fragment:
 * <ul>
 *   <li>{@code pos} — 80mm thermal receipt roll</li>
 *   <li>{@code a4p} — A4 portrait, one invoice per sheet</li>
 *   <li>{@code a4l} — A4 landscape, two invoices per sheet (left/right halves)</li>
 * </ul>
 *
 * <p>The renderer lazily opens a single Chromium instance on first use and
 * reuses it across calls. Callers should invoke {@link #close()} (or rely on
 * the {@link PreDestroy} hook) to release the browser process.
 */
@Service
public class InvoicePdfRenderer implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfRenderer.class);

    private final TemplateEngine templateEngine;
    private Playwright playwright;
    private Browser browser;

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
            case "a4l" -> "invoice/a4l"; // a4l with a single-item list falls back to left-half only
            default    -> "invoice/a4p";
        };

        // a4l needs a list; pos/a4p need a single billing
        Map<String, Object> model = switch (paperSize) {
            case "a4l" -> Map.of("billings", List.of(billing));
            default    -> Map.of("billing", billing);
        };

        // POS uses the invoicePos fragment (slim layout), which is
        // embedded inside pos.html — no separate template needed.
        String html = templateEngine.process(template, new Context(java.util.Locale.getDefault(), model));
        return renderHtmlToPdf(html, paperSize);
    }

    /**
     * Render a multi-invoice A4 landscape layout (two invoices per sheet).
     *
     * @param billings  list of invoice view models (paired into left/right halves)
     * @return the rendered PDF bytes
     */
    public byte[] renderA4L(List<InvoiceView> billings) {
        String html = templateEngine.process("invoice/a4l",
                new Context(java.util.Locale.getDefault(), Map.of("billings", billings)));
        return renderHtmlToPdf(html, "a4l");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  INTERNAL — Playwright rendering
    // ═══════════════════════════════════════════════════════════════════════════

    private byte[] renderHtmlToPdf(String html, String paperSize) {
        try {
            return renderHtmlToPdfInternal(html, paperSize);
        } catch (PlaywrightException e) {
            // Browser may have crashed — force relaunch on next call, then retry once
            log.warn("First render attempt failed for paper={}: {}", paperSize, e.getMessage());
            close();
            try {
                return renderHtmlToPdfInternal(html, paperSize);
            } catch (PlaywrightException retry) {
                log.error("Retry render also failed for paper={}: {}", paperSize, retry.getMessage(), retry);
                throw new RuntimeException("Invoice PDF rendering failed: " + retry.getMessage(), retry);
            }
        }
    }

    private byte[] renderHtmlToPdfInternal(String html, String paperSize) {
        ensureBrowser();
        try (Page page = browser.newPage()) {
            page.setContent(html, new Page.SetContentOptions().setWaitUntil(
                    com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));

            // Build PDF options matching the template's @page CSS
            Page.PdfOptions pdfOptions = buildPdfOptions(paperSize);

            byte[] pdfBytes = page.pdf(pdfOptions);
            log.debug("Rendered invoice PDF: {} bytes (paper={})", pdfBytes.length, paperSize);
            return pdfBytes;
        } catch (PlaywrightException e) {
            // Mark browser as potentially dead so next ensureBrowser() relaunches
            log.error("Playwright PDF rendering failed for paper={}: {}", paperSize, e.getMessage());
            close();
            throw e;
        }
    }

    private Page.PdfOptions buildPdfOptions(String paperSize) {
        Page.PdfOptions opts = new Page.PdfOptions();
        opts.setPrintBackground(true);  // render backgrounds (colored headers, alternating rows)

        switch (paperSize) {
            case "pos" -> {
                // 80mm thermal roll — auto height, narrow width
                opts.setWidth("80mm");
                // height is omitted → Playwright auto-sizes to content
                opts.setMargin(new Margin()
                    .setTop("4mm").setRight("4mm").setBottom("4mm").setLeft("4mm"));
            }
            case "a4l" -> {
                // A4 landscape — two invoices side by side
                opts.setLandscape(true);
                opts.setFormat("A4");
                opts.setMargin(new Margin()
                    .setTop("10mm").setRight("10mm").setBottom("10mm").setLeft("10mm"));
            }
            default -> {
                // A4 portrait (default)
                opts.setFormat("A4");
                opts.setMargin(new Margin()
                    .setTop("14mm").setRight("14mm").setBottom("14mm").setLeft("14mm"));
            }
        }

        return opts;
    }

    // ═══════════════════════════════════════════════════════════════════════════
    //  BROWSER LIFECYCLE
    // ═══════════════════════════════════════════════════════════════════════════

    private synchronized void ensureBrowser() {
        if (browser != null && browser.isConnected()) return;

        // Try system-installed Chromium first (via Dockerfile pre-installation)
        String[] chromiumPaths = {
            "/usr/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "chromium",
            "chromium-browser"
        };

        try {
            playwright = Playwright.create();

            BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setArgs(List.of(
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-gpu"
                ));

            // Check for system-installed Chromium
            boolean foundSystem = false;
            for (String path : chromiumPaths) {
                try {
                    var process = Runtime.getRuntime().exec(new String[]{"which", path});
                    int exitCode = process.waitFor();
                    if (exitCode == 0) {
                        String resolved = new String(process.getInputStream().readAllBytes()).trim();
                        if (!resolved.isEmpty()) {
                            log.info("Using system Chromium at: {}", resolved);
                            launchOpts.setExecutablePath(Path.of(resolved));
                            foundSystem = true;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            browser = playwright.chromium().launch(launchOpts);
            log.info("Playwright Chromium launched for invoice rendering{}",
                    foundSystem ? " (system binary)" : " (Playwright-managed)");
        } catch (Exception e) {
            close(); // clean up partial state
            log.error("Failed to launch Playwright Chromium: {}", e.getMessage(), e);
            throw new RuntimeException(
                "Cannot start headless browser for invoice rendering. " +
                "Ensure Chromium is installed in the Docker image or Playwright drivers are available.",
                e);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (browser != null) {
            try { browser.close(); } catch (Exception ignored) {}
            browser = null;
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
            playwright = null;
        }
        log.info("Playwright Chromium closed");
    }
}
