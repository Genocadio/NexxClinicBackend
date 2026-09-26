package com.nexxserve.nexxclinic.config;

import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Verifies that a system Chromium binary is available for invoice PDF rendering
 * at application startup. Logs a clear warning if not found so operators can
 * fix the Docker image before the first invoice request.
 *
 * <p>This replaced the previous Playwright-based pre-download which failed
 * because the Java Playwright SDK's driver installation couldn't complete
 * in Docker. InvoicePdfRenderer now uses Chromium directly via
 * {@code --print-to-pdf}.
 */
@Component
@Order(50)
public class PlaywrightStartupInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightStartupInitializer.class);

    @Override
    public void run(ApplicationArguments args) {
        String envPath = System.getenv("CHROMIUM_PATH");
        if (envPath != null && !envPath.isBlank() && Files.isExecutable(Path.of(envPath))) {
            log.info("Invoice PDF rendering: Chromium found from CHROMIUM_PATH at {}", envPath);
            return;
        }

        java.util.List<String> candidates = new java.util.ArrayList<>(java.util.List.of(
            "/usr/local/bin/chromium",
            "/usr/bin/chromium-browser",
            "/usr/bin/chromium",
            "/usr/bin/google-chrome",
            "/usr/bin/google-chrome-stable",
            "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome",
            "/Applications/Chromium.app/Contents/MacOS/Chromium",
            "/Applications/Brave Browser.app/Contents/MacOS/Brave Browser",
            "/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge"
        ));

        String userHome = System.getProperty("user.home", "");
        if (!userHome.isBlank()) {
            Path[] playwrightRoots = {
                Path.of(userHome, "Library", "Caches", "ms-playwright"),
                Path.of(userHome, ".cache", "ms-playwright")
            };
            for (Path pwRoot : playwrightRoots) {
                if (Files.isDirectory(pwRoot)) {
                    try (var stream = Files.walk(pwRoot, 10)) {
                        stream.filter(p -> {
                            String name = p.getFileName().toString();
                            return (name.equals("chrome") || name.equals("chromium") ||
                                    name.equals("Google Chrome for Testing") || name.equals("Chromium") ||
                                    name.equals("headless_shell"))
                                    && Files.isExecutable(p) && !Files.isDirectory(p);
                        }).findFirst().ifPresent(p -> candidates.add(0, p.toAbsolutePath().toString()));
                    } catch (Exception ignored) {}
                }
            }
        }

        for (String candidate : candidates) {
            if (Files.isExecutable(Path.of(candidate))) {
                log.info("Invoice PDF rendering: Chromium found at {}", candidate);
                return;
            }
        }

        // Try `which` as fallback
        for (String name : new String[]{"chromium", "chromium-browser", "google-chrome", "chrome"}) {
            try {
                Process p = Runtime.getRuntime().exec(new String[]{"which", name});
                int exit = p.waitFor();
                if (exit == 0) {
                    String resolved = new String(p.getInputStream().readAllBytes()).trim();
                    if (!resolved.isEmpty() && Files.isExecutable(Path.of(resolved))) {
                        log.info("Invoice PDF rendering: Chromium resolved via 'which {}': {}", name, resolved);
                        return;
                    }
                }
            } catch (Exception ignored) {}
        }

        log.warn(
            "No system Chromium binary found. Invoice PDF rendering will fail at runtime. " +
            "Install Chromium in the Docker image (check /usr/local/bin/chromium or install via apt-get or npx playwright install chromium)."
        );
    }
}
