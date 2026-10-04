# Multi-stage build for better caching and a small runtime image.
FROM gradle:9-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

# Pre-fetch dependencies to leverage Docker layer caching
RUN gradle dependencies --no-daemon || true

COPY src/ src/

RUN gradle bootJar --no-daemon -x test

# ── Runtime: Ubuntu Jammy with Chromium ──
FROM eclipse-temurin:21-jre-jammy

# Install Chromium runtime dependencies, fonts, and curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
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
    fonts-dejavu-core \
    fonts-liberation \
    fonts-noto-color-emoji \
    curl \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && mkdir -p /opt/playwright-browsers \
    && PLAYWRIGHT_BROWSERS_PATH=/opt/playwright-browsers npx -y playwright@1.44.0 install chromium \
    && ln -sf "$(ls -d /opt/playwright-browsers/chromium-*/chrome-linux/chrome)" /usr/local/bin/chromium \
    && apt-get purge -y nodejs \
    && apt-get autoremove -y \
    && apt-get clean \
    && rm -rf /var/lib/apt/lists/* /root/.npm /root/.cache /tmp/* /var/tmp/*

# Create non-root user
RUN groupadd -g 1001 spring && \
    useradd -u 1001 -g spring -s /bin/bash -m spring

WORKDIR /app

# Copy application JAR and entrypoint directly with proper ownership (no duplicate layers)
COPY --chown=spring:spring --from=build /app/build/libs/*.jar /app/app.jar
COPY --chown=spring:spring entrypoint.sh /app/entrypoint.sh

RUN mkdir -p /data/storage && \
    chown -R spring:spring /data/storage && \
    chmod +x /app/entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1

# Run as root so entrypoint.sh can fix Docker volume permissions,
# then drop to spring user via runuser in the script.
USER root

ENTRYPOINT ["/app/entrypoint.sh"]
