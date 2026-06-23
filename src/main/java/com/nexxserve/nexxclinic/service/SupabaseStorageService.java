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

/**
 * Handles PDF invoice storage via the Supabase Storage v1 REST API.
 *
 * <p>Bucket: {@code data} (private).
 *
 * <p>Object paths follow the convention:
 * <ul>
 *   <li>{@code invoices/{clinicName}/invoice-{billingId}.pdf} – when a clinic name is available</li>
 *   <li>{@code invoices/invoice-{billingId}.pdf} – when no clinic name is configured</li>
 * </ul>
 *
 * <p>The stored path (not a URL) is persisted in {@code DepartmentInsuranceBilling.invoiceUrl}.
 * Downloadable signed URLs are generated on demand via {@link #signedUrl}.
 */
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

    /**
     * Build the object path inside the {@code data} bucket.
     *
     * @param clinicName  optional clinic name used as a sub-folder (may be null/blank)
     * @param billingId   the UUID of the {@code DepartmentInsuranceBilling}
     * @return e.g. {@code invoices/ClinicName/invoice-uuid.pdf}
     */
    public String buildObjectPath(String clinicName, String billingId) {
        String filename = "invoice-" + billingId + ".pdf";
        if (clinicName != null && !clinicName.isBlank()) {
            // Sanitise: keep letters, digits, hyphens, underscores; replace everything else with _
            String safe = clinicName.trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
            return BASE_PATH + "/" + safe + "/" + filename;
        }
        return BASE_PATH + "/" + filename;
    }

    // ─── UPLOAD ──────────────────────────────────────────────────────────────

    /**
     * Upload raw PDF bytes to Supabase Storage.
     * Uses {@code x-upsert: true} so that re-generating an invoice always overwrites.
     *
     * @param pdfBytes   content of the PDF file
     * @param objectPath path inside the bucket, e.g. {@code invoices/Name/invoice-xxx.pdf}
     * @throws IOException if the HTTP call fails or Supabase returns a non-2xx status
     */
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

    // ─── SIGNED URL ──────────────────────────────────────────────────────────

    /**
     * Request a short-lived signed URL for downloading an invoice.
     *
     * @param objectPath      the stored object path (not a URL)
     * @param expiresInSeconds how long the URL should remain valid
     * @return absolute HTTPS URL that can be used directly by the caller
     * @throws IOException if the Supabase call fails or returns an unexpected response
     */
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

        // Response: {"signedURL": "/storage/v1/object/sign/data/.../...?token=..."}
        @SuppressWarnings("unchecked")
        Map<String, Object> json = mapper.readValue(res.body(), Map.class);
        Object signedPath = json.get("signedURL");
        if (signedPath == null || signedPath.toString().isBlank()) {
            throw new IOException("Supabase returned an empty signedURL field");
        }

        String path = signedPath.toString();
        // Make it absolute if it's relative
        return path.startsWith("http") ? path : base() + path;
    }

    // ─── INTERNAL ────────────────────────────────────────────────────────────

    private String base() {
        if (props.getUrl() == null) return "";
        return props.getUrl().replaceAll("/+$", "");
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
