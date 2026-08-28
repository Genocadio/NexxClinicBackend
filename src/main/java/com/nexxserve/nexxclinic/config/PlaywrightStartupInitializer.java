package com.nexxserve.nexxclinic.config;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Eagerly downloads Playwright Chromium on application startup so that the
 * first invoice generation call doesn't block while downloading ~85 MiB of
 * browser binaries.
 *
 * <p>The download runs asynchronously so it doesn't delay application startup.
 * If the download is already complete (e.g. in Docker where the Dockerfile
 * pre-installs Chromium), this is a no-op that completes instantly.
 *
 * <p>The {@code PLAYWRIGHT_BROWSERS_PATH} environment variable must be set
 * (by {@code dev.sh} or the Dockerfile) so Playwright knows where to find
 * / install browser binaries.
 */
@Component
@Order(50) // after SupabaseConfigValidator (order 10) but before MeilisearchStartupSync
public class PlaywrightStartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightStartupInitializer.class);

    @Override
    public void run(ApplicationArguments args) {
        String browsersPath = System.getenv("PLAYWRIGHT_BROWSERS_PATH");
        log.info("Playwright browsers path: {}", browsersPath != null ? browsersPath : "(not set — using default)");

        CompletableFuture.runAsync(this::ensurePlaywrightReady);
    }

    private void ensurePlaywrightReady() {
        long start = System.currentTimeMillis();
        try {
            log.info("Pre-downloading Playwright Chromium (async)...");

            try (Playwright playwright = Playwright.create()) {
                BrowserType chromium = playwright.chromium();
                try (Browser browser = chromium.launch(
                    new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setArgs(java.util.List.of(
                            "--no-sandbox",
                            "--disable-setuid-sandbox",
                            "--disable-dev-shm-usage",
                            "--disable-gpu"
                        ))
                )) {
                    log.info("Playwright Chromium launched successfully during pre-download.");
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            log.info("Playwright Chromium ready (took {}ms)", elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.warn(
                "Playwright Chromium pre-download failed after {}ms: {}. " +
                "Invoice generation will attempt to download on first use.",
                elapsed, e.getMessage()
            );
            // Don't throw — the application must still start.
        }
    }
}
