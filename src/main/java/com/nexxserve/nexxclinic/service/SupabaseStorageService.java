package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "supabase.storage-type", havingValue = "SUPABASE")
public class SupabaseStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(
        SupabaseStorageService.class
    );

    private static final String BASE_PATH = "invoices";

    /** HTTP 429 Too Many Requests — transient, retryable. */
    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    /** HTTP 408 Request Timeout — transient, retryable. */
    private static final int HTTP_REQUEST_TIMEOUT = 408;

    /**
     * Cap on a single backoff sleep (30s). Prevents `1L << attempt` overflow for a
     * misconfigured large retry-max-attempts and bounds worst-case wait time.
     */
    private static final long MAX_BACKOFF_MS = 30_000L;

    private final SupabaseProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper;

    @org.springframework.beans.factory.annotation.Autowired
    public SupabaseStorageService(SupabaseProperties props) {
        this.props = props;
        this.http = buildHttpClient();
        this.mapper = new ObjectMapper();
    }

    /**
     * Package-private constructor for tests: inject a controllable {@link HttpClient}
     * so timeout/retry behaviour can be verified without a real Supabase instance.
     */
    SupabaseStorageService(SupabaseProperties props, HttpClient http) {
        this.props = props;
        this.http = http;
        this.mapper = new ObjectMapper();
    }

    private HttpClient buildHttpClient() {
        // Connect timeout: fail fast when Supabase is unreachable instead of waiting
        // on the OS default (which can be minutes). Request timeouts are applied
        // per-request below, so a slow/hung response never blocks the caller.
        return HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(props.getConnectTimeoutMs()))
            .build();
    }

    // ─── PATH HELPER ─────────────────────────────────────────────────────────

    public String buildObjectPath(String clinicName, String billingId) {
        String filename = "invoice-" + billingId + ".pdf";
        if (clinicName != null && !clinicName.isBlank()) {
            String safe = clinicName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            return BASE_PATH + "/" + safe + "/" + filename;
        }
        return BASE_PATH + "/" + filename;
    }

    // ─── UPLOAD (invoice-specific) ───────────────────────────────────────────

    public void upload(byte[] pdfBytes, String objectPath) throws IOException {
        try {
            doUploadInvoice(pdfBytes, objectPath);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Bucket not found")) {
                log.warn(
                    "Invoice bucket '{}' not found, attempting to create it",
                    invoiceBucket()
                );
                createBucket(invoiceBucket(), false);
                doUploadInvoice(pdfBytes, objectPath);
            } else {
                throw e;
            }
        }
    }

    private void doUploadInvoice(byte[] pdfBytes, String objectPath) throws IOException {
        String endpoint =
            base() + "/storage/v1/object/" + invoiceBucket() + "/" + objectPath;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/pdf")
            .header("x-upsert", "true")
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .POST(HttpRequest.BodyPublishers.ofByteArray(pdfBytes))
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase upload failed  path={} status={} body={}",
                objectPath,
                res.statusCode(),
                res.body()
            );
            String msg = "Supabase upload failed with HTTP " + res.statusCode();
            if (res.body() != null && res.body().contains("Bucket not found")) {
                msg = "Bucket not found";
            }
            throw new IOException(msg);
        }
        log.debug(
            "Supabase upload ok  path={} status={}",
            objectPath,
            res.statusCode()
        );
    }

    // ─── GENERIC UPLOAD (any bucket) ─────────────────────────────────────────

    public void upload(byte[] data, String bucket, String objectPath, String contentType)
            throws IOException {
        try {
            doUpload(data, bucket, objectPath, contentType);
        } catch (IOException e) {
            if (e.getMessage() != null && e.getMessage().contains("Bucket not found")) {
                log.warn("Bucket '{}' not found, attempting to create it", bucket);
                boolean isPublic = bucket.equals(props.getBucketPublic());
                createBucket(bucket, isPublic);
                doUpload(data, bucket, objectPath, contentType);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void ensureDirectory(String bucket, String directoryPath) throws IOException {
        String prefix = trimSlashes(directoryPath);
        if (prefix.isBlank()) {
            return;
        }
        upload(new byte[0], bucket, prefix + "/.keep", "application/octet-stream");
    }

    @Override
    public void movePrefix(String bucket, String sourcePrefix, String destinationPrefix)
            throws IOException {
        String source = trimSlashes(sourcePrefix);
        String destination = trimSlashes(destinationPrefix);
        if (source.isBlank() || destination.isBlank() || source.equals(destination)) {
            return;
        }
        if (destination.startsWith(source + "/")) {
            throw new IOException("Cannot move a storage prefix inside itself");
        }

        List<StorageObject> objects = new ArrayList<>();
        collectObjects(bucket, source, objects);
        for (StorageObject object : objects) {
            String suffix = object.path().substring(source.length());
            String newObjectPath = destination + suffix;
            byte[] contents = download(bucket, object.path());
            upload(contents, bucket, newObjectPath, object.contentType());
            delete(bucket, object.path());
        }
        log.info("Moved Supabase storage prefix {}/{} to {}", bucket, source, destination);
    }

    private void collectObjects(String bucket, String prefix, List<StorageObject> objects) throws IOException {
        int offset = 0;
        while (true) {
            String body = mapper.writeValueAsString(Map.of(
                "prefix", prefix,
                "limit", 1000,
                "offset", offset,
                "sortBy", Map.of("column", "name", "order", "asc")
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(base() + "/storage/v1/object/list/" + bucket))
                .header("Authorization", "Bearer " + props.getServiceKey())
                .header("Content-Type", "application/json")
                .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = sendWithRetry(request);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Supabase object listing failed with HTTP " + response.statusCode());
            }

            com.fasterxml.jackson.databind.JsonNode entries = mapper.readTree(response.body());
            if (!entries.isArray()) {
                throw new IOException("Supabase returned an invalid object listing");
            }
            int resultCount = entries.size();
            for (com.fasterxml.jackson.databind.JsonNode entry : entries) {
                String name = entry.path("name").asText(null);
                if (name == null || name.isBlank()) {
                    continue;
                }
                String objectPath = prefix + "/" + name;
                if (entry.path("id").isMissingNode() || entry.path("id").isNull()) {
                    collectObjects(bucket, objectPath, objects);
                } else {
                    String contentType = entry.path("metadata").path("mimetype").asText("application/octet-stream");
                    objects.add(new StorageObject(objectPath, contentType));
                }
            }
            if (resultCount < 1000) {
                break;
            }
            offset += resultCount;
        }
    }

    @Override
    public byte[] download(String bucket, String objectPath) throws IOException {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(base() + "/storage/v1/object/" + bucket + "/" + objectPath))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .GET()
            .build();
        int retries = Math.max(0, props.getRetryMaxAttempts());
        for (int attempt = 0; ; attempt++) {
            HttpResponse<byte[]> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Supabase object download interrupted", e);
            } catch (IOException e) {
                if (attempt >= retries) {
                    throw e;
                }
                sleepBeforeRetry(attempt);
                continue;
            }
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response.body();
            }
            if ((response.statusCode() == HTTP_TOO_MANY_REQUESTS || response.statusCode() >= 500)
                    && attempt < retries) {
                sleepBeforeRetry(attempt);
                continue;
            }
            throw new IOException("Supabase object download failed with HTTP " + response.statusCode());
        }
    }

    private void sleepBeforeRetry(int attempt) throws IOException {
        try {
            Thread.sleep(backoffFor(attempt));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase storage retry interrupted", e);
        }
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') start++;
        while (end > start && value.charAt(end - 1) == '/') end--;
        return value.substring(start, end);
    }

    private record StorageObject(String path, String contentType) {}

    private void doUpload(byte[] data, String bucket, String objectPath, String contentType)
            throws IOException {
        String endpoint =
            base() + "/storage/v1/object/" + bucket + "/" + objectPath;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", contentType)
            .header("x-upsert", "true")
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .POST(HttpRequest.BodyPublishers.ofByteArray(data))
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase upload failed  bucket={} path={} status={} body={}",
                bucket, objectPath, res.statusCode(), res.body()
            );
            String msg = "Supabase upload failed with HTTP " + res.statusCode();
            if (res.body() != null && res.body().contains("Bucket not found")) {
                msg = "Bucket not found";
            }
            throw new IOException(msg);
        }
        log.debug(
            "Supabase upload ok  bucket={} path={} status={}",
            bucket, objectPath, res.statusCode()
        );
    }

    public void createBucket(String bucketName, boolean isPublic) throws IOException {
        // Fail fast on a null/blank bucket name (e.g. an unset property) instead of
        // sending a malformed request that can hang or 400 confusingly. This is a
        // configuration error, not a transient storage failure — so it is NOT retried.
        if (bucketName == null || bucketName.isBlank()) {
            log.error(
                "createBucket rejected: bucketName is null or blank (is supabase.bucket-public/private configured?)"
            );
            throw new IllegalArgumentException(
                "bucketName is required to create a Supabase bucket"
            );
        }
        String endpoint = base() + "/storage/v1/bucket";
        // S8 fix: Map.of throws NullPointerException on a null bucketName (e.g. an
        // unset property). Build the JSON body null-safely.
        Map<String, Object> bodyPayload = new LinkedHashMap<>();
        bodyPayload.put("name", bucketName);
        bodyPayload.put("public", isPublic);
        String body = mapper.writeValueAsString(bodyPayload);

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase create-bucket failed  bucket={} status={} body={}",
                bucketName, res.statusCode(), res.body()
            );
            throw new IOException(
                "Supabase create-bucket failed with HTTP " + res.statusCode()
            );
        }
        log.info("Supabase bucket '{}' created (public={})", bucketName, isPublic);
    }

    // ─── DELETE ──────────────────────────────────────────────────────────────

    public void delete(String bucket, String objectPath) throws IOException {
        String endpoint =
            base() + "/storage/v1/object/" + bucket + "/" + objectPath;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .DELETE()
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase delete failed  bucket={} path={} status={} body={}",
                bucket, objectPath, res.statusCode(), res.body()
            );
            throw new IOException(
                "Supabase delete failed with HTTP " + res.statusCode()
            );
        }
        log.debug(
            "Supabase delete ok  bucket={} path={} status={}",
            bucket, objectPath, res.statusCode()
        );
    }

    // ─── PUBLIC URL ──────────────────────────────────────────────────────────

    public String publicUrl(String bucket, String objectPath) {
        return "/storage/v1/object/public/" + bucket + "/" + objectPath;
    }

    public String fullPublicUrl(String bucket, String objectPath) {
        return base() + publicUrl(bucket, objectPath);
    }

    // ─── FileStorageService interface ────────────────────────────────────────

    @Override
    public String getPublicUrl(String bucket, String objectPath) {
        return publicUrl(bucket, objectPath);
    }

    @Override
    public String getSignedUrl(String bucket, String objectPath, int expiresInSeconds) throws IOException {
        return signedUrl(bucket, objectPath, expiresInSeconds);
    }

    @Override
    public String getStoragePath(String bucket, String objectPath) {
        return objectPath;
    }

    // ─── SIGNED URL ──────────────────────────────────────────────────────────

    public String signedUrl(String objectPath, int expiresInSeconds)
        throws IOException {
        String endpoint =
            base() + "/storage/v1/object/sign/" + invoiceBucket() + "/" + objectPath;
        String body = mapper.writeValueAsString(
            Map.of("expiresIn", expiresInSeconds)
        );

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase sign failed  path={} status={} body={}",
                objectPath,
                res.statusCode(),
                res.body()
            );
            throw new IOException(
                "Supabase signed-URL request failed with HTTP " +
                    res.statusCode()
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(res.body(), Map.class);
        Object signedPath = json.get("signedURL");
        if (signedPath == null || signedPath.toString().isBlank()) {
            throw new IOException("Supabase returned an empty signedURL field");
        }
        log.debug(
            "Supabase sign ok  path={} rawSignedURL={}",
            objectPath,
            signedPath
        );

        return buildSignedPath(invoiceBucket(), objectPath, signedPath.toString());
    }

    public String signedUrl(String bucket, String objectPath, int expiresInSeconds)
            throws IOException {
        String endpoint =
            base() + "/storage/v1/object/sign/" + bucket + "/" + objectPath;
        String body = mapper.writeValueAsString(
            Map.of("expiresIn", expiresInSeconds)
        );

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/json")
            .timeout(Duration.ofMillis(props.getRequestTimeoutMs()))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = sendWithRetry(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase sign failed  bucket={} path={} status={} body={}",
                bucket, objectPath, res.statusCode(), res.body()
            );
            throw new IOException(
                "Supabase signed-URL request failed with HTTP " + res.statusCode()
            );
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(res.body(), Map.class);
        Object signedPath = json.get("signedURL");
        if (signedPath == null || signedPath.toString().isBlank()) {
            throw new IOException("Supabase returned an empty signedURL field");
        }
        log.debug(
            "Supabase sign ok  bucket={} path={} rawSignedURL={}",
            bucket, objectPath, signedPath
        );

        return buildSignedPath(bucket, objectPath, signedPath.toString());
    }

    // ─── INTERNAL ────────────────────────────────────────────────────────────

    /**
     * Bucket used for generated invoices. Configurable via
     * {@code supabase.bucket-invoices} (default {@code "clinic-files"}); created on first
     * use if it does not exist yet.
     */
    public String invoiceBucket() {
        String bucket = props.getBucketInvoices();
        return (bucket == null || bucket.isBlank()) ? "clinic-files" : bucket;
    }

    private String base() {
        if (props.getUrl() == null) return "";
        return props.getUrl().replaceAll("/+$", "");
    }

    /**
     * Builds the relative public path for a signed URL, always keeping the
     * {@code /storage/v1} prefix. Some self-hosted Supabase storage-API responses
     * omit it and only return {@code .../object/sign/{bucket}/{path}?token=...};
     * rebuilding the path from the bucket/object guarantees a URL the frontend can
     * resolve by prepending its own base host.
     */
    private String buildSignedPath(String bucket, String objectPath, String rawSignedUrl) {
        String query = "";
        int q = rawSignedUrl.indexOf('?');
        if (q >= 0) {
            query = rawSignedUrl.substring(q);
        }
        String path =
            "/storage/v1/object/sign/" + bucket + "/" + objectPath + query;
        log.debug(
            "buildSignedPath bucket={} path={} raw={} -> {}",
            bucket, objectPath, rawSignedUrl, path
        );
        return path;
    }

    /**
     * Sends a request, retrying transient failures (HTTP 429 and 5xx) with an
     * exponential backoff capped by {@code supabase.retry-max-attempts} and
     * {@code supabase.retry-backoff-ms}. Retries make the invoice upload resilient
     * to throttling / short-lived Supabase outages; the per-request timeout ensures
     * each attempt fails fast instead of hanging.
     */
    private HttpResponse<String> sendWithRetry(HttpRequest req) throws IOException {
        // retryMaxAttempts counts retries AFTER the initial attempt (0 = no retry),
        // so the total number of HTTP sends is retryMaxAttempts + 1.
        int retries = Math.max(0, props.getRetryMaxAttempts());
        int attempt = 0;
        while (true) {
            HttpResponse<String> res = null;
            IOException io = null;
            try {
                res = http.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Supabase HTTP call interrupted", e);
            } catch (IOException e) {
                io = e;
            }

            if (io != null) {
                if (attempt >= retries) {
                    throw io;
                }
                long backoff = backoffFor(attempt);
                log.warn(
                    "Supabase request failed on attempt {}/{} for {}: {}; retrying in {}ms",
                    attempt + 1,
                    retries + 1,
                    req.uri(),
                    io.getMessage(),
                    backoff
                );
                sleepQuietly(backoff);
                attempt++;
                continue;
            }

            int code = res.statusCode();
            if (!isTransient(code) || attempt >= retries) {
                return res;
            }
            long backoff = backoffFor(attempt);
            log.warn(
                "Supabase transient HTTP {} on attempt {}/{} for {}; retrying in {}ms",
                code,
                attempt + 1,
                retries + 1,
                req.uri(),
                backoff
            );
            sleepQuietly(backoff);
            attempt++;
        }
    }

    private static boolean isTransient(int statusCode) {
        return (
            statusCode == HTTP_TOO_MANY_REQUESTS ||
            statusCode == HTTP_REQUEST_TIMEOUT ||
            statusCode >= 500
        );
    }

    /** Exponential backoff with an overflow-safe cap (base * 2^attempt, max 30s). */
    private long backoffFor(int attempt) {
        long base = Math.max(0, props.getRetryBackoffMs());
        if (attempt >= 63) {
            return MAX_BACKOFF_MS;
        }
        return Math.min(base * (1L << attempt), MAX_BACKOFF_MS);
    }

    /**
     * Sleeps between retries. An interrupt aborts the retry loop promptly instead of
     * letting a cancelled thread hammer the service again.
     */
    private static void sleepQuietly(long millis) throws IOException {
        if (millis <= 0) return;
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase retry interrupted", e);
        }
    }
}
