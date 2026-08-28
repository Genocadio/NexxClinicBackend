# Multi-stage build for better caching and a small runtime image.
FROM gradle:9-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

COPY src/ src/

RUN gradle build --no-daemon -x test

# ── Runtime: Debian bookworm (well-tested with Playwright/Chromium) ──
FROM eclipse-temurin:21-jre-jammy

# Install system dependencies required by Playwright Chromium + curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    # Playwright Chromium runtime dependencies
    libnss3 \
    libnspr4 \
    libatk1.0-0 \
    libatk-bridge2.0-0 \
    libcups2 \
    libdrm2 \
    libdbus-1-3 \
    libxkbcommon0 \
    libxcomposite1 \
    libxdamage1 \
    libxfixes3 \
    libxrandr2 \
    libgbm1 \
    libpango-1.0-0 \
    libcairo2 \
    libasound2 \
    libatspi2.0-0 \
    libwayland-client0 \
    libxshmfence1 \
    # Fonts for proper text rendering (especially non-Latin characters)
    fonts-dejavu-core \
    fonts-liberation \
    fonts-noto-color-emoji \
    # Utilities
    curl \
    && rm -rf /var/lib/apt/lists/*

# Install Node.js 20.x for Playwright driver (fallback if system Chromium unavailable)
RUN curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -g 1001 spring && \
    useradd -u 1001 -g spring -s /bin/bash -m spring

# Playwright browser + driver cache directories.
# The Java Playwright SDK needs a writable cache for its driver JAR and
# browser binaries. The pre-installed browsers are at PLAYWRIGHT_BROWSERS_PATH,
# but the SDK also writes driver metadata to ~/.cache/ms-playwright/.
ENV PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers
RUN mkdir -p /opt/playwright-browsers \
    && mkdir -p /home/spring/.cache/ms-playwright \
    && chown -R spring:spring /opt/playwright-browsers /home/spring/.cache/ms-playwright

# Pre-install Playwright Chromium at build time (avoids runtime download).
# The Java SDK 1.44.0 bundles a Node.js driver that also needs pre-cached
# browsers to avoid a network download on first use.
RUN su -s /bin/bash -c "PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers npx -y playwright@1.44.0 install chromium"
# Verify the Chromium binary was actually downloaded
RUN ls /opt/playwright-browsers/chromium-*/chrome-linux/chrome >/dev/null 2>&1 || \
    ls /opt/playwright-browsers/chromium-*/chrome-linux/chromium >/dev/null 2>&1 || \
    (echo 'ERROR: Playwright Chromium binary not found after install' && exit 1)

# Symlink Playwright-installed Chromium to a system path so InvoicePdfRenderer
# can find it via `which chromium` without triggering Playwright's driver install.
RUN ln -sf "$(ls -d /opt/playwright-browsers/chromium-*/chrome-linux/chrome)" /usr/local/bin/chromium

# Also pre-cache the Playwright driver's Node.js dependencies so the Java SDK's
# Playwright.create() doesn't try to download anything at runtime.
# The Java SDK extracts its own driver to ~/.cache/ms-playwright/ and runs
# `node install.js`. By pre-populating the driver cache, we skip this step.
RUN su -s /bin/bash -c "PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers npx -y playwright@1.44.0 install-deps chromium 2>/dev/null || true"

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN mkdir -p /data/storage && \
    chown -R spring:spring /app /data/storage && \
    chmod +x /app/entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

# Run as root so entrypoint.sh can fix Docker volume permissions,
# then drop to spring user via runuser in the script.
USER root

ENTRYPOINT ["/app/entrypoint.sh"]
