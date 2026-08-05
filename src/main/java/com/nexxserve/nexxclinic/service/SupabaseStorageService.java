package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageService {

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

        return stripBase(signedPath.toString());
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

        return stripBase(signedPath.toString());
    }

    // ─── INTERNAL ────────────────────────────────────────────────────────────

    /**
     * Bucket used for generated invoices. Configurable via
     * {@code supabase.bucket-invoices} (default {@code "data"}); created on first
     * use if it does not exist yet.
     */
    public String invoiceBucket() {
        String bucket = props.getBucketInvoices();
        return (bucket == null || bucket.isBlank()) ? "data" : bucket;
    }

    private String base() {
        if (props.getUrl() == null) return "";
        return props.getUrl().replaceAll("/+$", "");
    }

    private String stripBase(String url) {
        String b = base();
        if (!b.isEmpty() && url.startsWith(b)) {
            return url.substring(b.length());
        }
        return url;
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
