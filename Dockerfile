# Multi-stage build for better caching and a small runtime image.
FROM gradle:9-jdk21-alpine AS build

WORKDIR /app

COPY build.gradle settings.gradle ./
COPY gradle/ gradle/

COPY src/ src/

RUN gradle build --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget su-exec && \
    addgroup -g 1001 -S spring && \
    adduser -u 1001 -S spring -G spring

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh

RUN mkdir -p /data/storage && \
    chown -R spring:spring /app /data/storage && \
    chmod +x /app/entrypoint.sh

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run as root so entrypoint.sh can fix Docker volume permissions,
# then drop to spring user via su-exec in the script.
USER root

ENTRYPOINT ["/app/entrypoint.sh"]
