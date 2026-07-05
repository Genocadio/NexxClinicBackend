package com.nexxserve.nexxclinic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(
        SupabaseStorageService.class
    );

    private static final String BUCKET = "data";
    private static final String BASE_PATH = "invoices";

    private final SupabaseProperties props;
    private final HttpClient http;
    private final ObjectMapper mapper;

    public SupabaseStorageService(SupabaseProperties props) {
        this.props = props;
        this.http = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
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
        String endpoint =
            base() + "/storage/v1/object/" + BUCKET + "/" + objectPath;

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/pdf")
            .header("x-upsert", "true")
            .POST(HttpRequest.BodyPublishers.ofByteArray(pdfBytes))
            .build();

        HttpResponse<String> res = send(req);
        if (res.statusCode() < 200 || res.statusCode() >= 300) {
            log.error(
                "Supabase upload failed  path={} status={} body={}",
                objectPath,
                res.statusCode(),
                res.body()
            );
            throw new IOException(
                "Supabase upload failed with HTTP " + res.statusCode()
            );
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
            .POST(HttpRequest.BodyPublishers.ofByteArray(data))
            .build();

        HttpResponse<String> res = send(req);
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
        String endpoint = base() + "/storage/v1/bucket";
        String body = mapper.writeValueAsString(Map.of(
            "name", bucketName,
            "public", isPublic
        ));

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = send(req);
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
            .DELETE()
            .build();

        HttpResponse<String> res = send(req);
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
            base() + "/storage/v1/object/sign/" + BUCKET + "/" + objectPath;
        String body = mapper.writeValueAsString(
            Map.of("expiresIn", expiresInSeconds)
        );

        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Authorization", "Bearer " + props.getServiceKey())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = send(req);
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
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> res = send(req);
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

    private HttpResponse<String> send(HttpRequest req) throws IOException {
        try {
            return http.send(req, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Supabase HTTP call interrupted", e);
        }
    }
}
