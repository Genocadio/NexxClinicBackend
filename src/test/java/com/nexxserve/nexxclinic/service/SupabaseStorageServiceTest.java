package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SupabaseStorageService} hardening:
 * <ul>
 *   <li>the HTTP client gets a configurable connect timeout and every request a
 *       request timeout, so a hanging Supabase never blocks the caller;</li>
 *   <li>transient failures (HTTP 429 / 5xx) are retried with exponential backoff up
 *       to {@code retry-max-attempts}; client errors (4xx) are NOT retried;</li>
 *   <li>{@code createBucket} rejects a null/blank bucket name explicitly.</li>
 * </ul>
 *
 * <p>The tests inject a mock {@link HttpClient} through the package-private
 * constructor so no real network call ever happens.
 */
class SupabaseStorageServiceTest {

    private SupabaseProperties props() {
        SupabaseProperties p = new SupabaseProperties();
        p.setUrl("https://supa.med.rw");
        p.setServiceKey("test-key");
        return p;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> response(int statusCode) {
        HttpResponse<String> res = mock(HttpResponse.class);
        when(res.statusCode()).thenReturn(statusCode);
        when(res.body()).thenReturn("{\"ok\":true}");
        return res;
    }

    @SuppressWarnings("unchecked")
    private HttpResponse<String> responseWithBody(int statusCode, String body) {
        HttpResponse<String> res = mock(HttpResponse.class);
        when(res.statusCode()).thenReturn(statusCode);
        when(res.body()).thenReturn(body);
        return res;
    }

    // ─────────────────────────────────────────────────────────────
    // Timeouts
    // ─────────────────────────────────────────────────────────────

    @Test
    void requestTimeoutIsAppliedToUpload() throws Exception {
        SupabaseProperties props = props();
        props.setRequestTimeoutMs(2500);
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        service.upload(new byte[]{1, 2}, "data", "invoices/x.pdf", "application/pdf");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(http).send(captor.capture(), any());
        assertTrue(captor.getValue().timeout().isPresent());
        assertEquals(
            Duration.ofMillis(2500),
            captor.getValue().timeout().orElseThrow()
        );
    }

    @Test
    void connectTimeoutIsConfiguredOnHttpClient() throws Exception {
        SupabaseProperties props = props();
        props.setConnectTimeoutMs(750);
        SupabaseStorageService service = new SupabaseStorageService(props);

        Field httpField = SupabaseStorageService.class.getDeclaredField("http");
        httpField.setAccessible(true);
        HttpClient http = (HttpClient) httpField.get(service);

        assertEquals(Optional.of(Duration.ofMillis(750)), http.connectTimeout());
    }

    // ─────────────────────────────────────────────────────────────
    // Retry: transient 429/5xx
    // ─────────────────────────────────────────────────────────────

    @Test
    void uploadRetriesOnceOn429ThenSucceeds() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(2);
        props.setRetryBackoffMs(1); // keep the test fast
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any()))
            .thenAnswer(inv -> response(429))
            .thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        service.upload(new byte[]{1}, "data", "invoices/x.pdf", "application/pdf");

        verify(http, times(2)).send(any(), any());
    }

    @Test
    void uploadRetriesTwiceOn503ThenSucceeds() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(2);
        props.setRetryBackoffMs(1);
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any()))
            .thenAnswer(inv -> response(503))
            .thenAnswer(inv -> response(502))
            .thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        service.upload(new byte[]{1}, "data", "invoices/x.pdf", "application/pdf");

        verify(http, times(3)).send(any(), any());
    }

    @Test
    void uploadFailsAfterRetriesExhaustedOnPersistent5xx() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(2);
        props.setRetryBackoffMs(1);
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv -> response(500));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        IOException ex = assertThrows(
            IOException.class,
            () -> service.upload(new byte[]{1}, "data", "invoices/x.pdf", "application/pdf")
        );

        assertTrue(ex.getMessage().contains("HTTP 500"));
        verify(http, times(3)).send(any(), any());
    }

    @Test
    void uploadDoesNotRetryClientError400() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(2);
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv -> response(400));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        IOException ex = assertThrows(
            IOException.class,
            () -> service.upload(new byte[]{1}, "data", "invoices/x.pdf", "application/pdf")
        );

        assertTrue(ex.getMessage().contains("HTTP 400"));
        verify(http, times(1)).send(any(), any());
    }

    @Test
    void invoiceUploadRetriesOn429WithBackoff() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(1);
        props.setRetryBackoffMs(1);
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any()))
            .thenAnswer(inv -> response(429))
            .thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        // The invoice-specific upload (2-arg) must benefit from the same retry logic.
        service.upload(new byte[]{1}, "invoices/clinic/invoice-x.pdf");

        verify(http, times(2)).send(any(), any());
    }

    // ─────────────────────────────────────────────────────────────
    // createBucket null guard
    // ─────────────────────────────────────────────────────────────

    @Test
    void createBucketRejectsNullAndBlankNames() {
        SupabaseProperties props = props();
        SupabaseStorageService service = new SupabaseStorageService(
            props,
            mock(HttpClient.class)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> service.createBucket(null, true)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> service.createBucket("  ", true)
        );
    }

    @Test
    void createBucketSucceedsWithValidName() throws Exception {
        SupabaseProperties props = props();
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        service.createBucket("uploads-public", true);

        verify(http, times(1)).send(any(), any());
    }

    // ─────────────────────────────────────────────────────────────
    // Bucket-not-found fallback still works
    // ─────────────────────────────────────────────────────────────

    @Test
    void genericUploadCreatesBucketOnBucketNotFoundThenRetries() throws Exception {
        SupabaseProperties props = props();
        props.setRetryMaxAttempts(0); // the bucket fallback is its own retry
        props.setBucketPublic("uploads-public");
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any()))
            .thenAnswer(inv -> responseWithBody(400, "Bucket not found"))
            .thenAnswer(inv -> response(200))
            .thenAnswer(inv -> response(200));
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        service.upload(
            new byte[]{1},
            "uploads-public",
            "files/a.txt",
            "text/plain"
        );

        // 1 failing upload + 1 bucket creation + 1 successful re-upload
        verify(http, times(3)).send(any(), any());
    }

    // ─────────────────────────────────────────────────────────────
    // Signed URL keeps the /storage/v1 prefix
    // ─────────────────────────────────────────────────────────────

    @Test
    void signedUrlRestoresStorageV1PrefixWhenSupabaseOmitsIt() throws Exception {
        SupabaseProperties props = props();
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv ->
            responseWithBody(
                200,
                "{\"signedURL\":\"https://supa.med.rw/object/sign/data/invoices/clinic/invoice-x.pdf?token=abc&expires=123\"}"
            )
        );
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        String url = service.signedUrl("invoices/clinic/invoice-x.pdf", 300);

        assertEquals(
            "/storage/v1/object/sign/data/invoices/clinic/invoice-x.pdf?token=abc&expires=123",
            url
        );
    }

    @Test
    void signedUrlDoesNotDuplicatePrefixWhenSupabaseAlreadyIncludesIt() throws Exception {
        SupabaseProperties props = props();
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any())).thenAnswer(inv ->
            responseWithBody(
                200,
                "{\"signedURL\":\"https://supa.med.rw/storage/v1/object/sign/data/invoices/clinic/invoice-x.pdf?token=abc\"}"
            )
        );
        SupabaseStorageService service = new SupabaseStorageService(props, http);

        String url = service.signedUrl("data", "invoices/clinic/invoice-x.pdf", 300);

        assertEquals(
            "/storage/v1/object/sign/data/invoices/clinic/invoice-x.pdf?token=abc",
            url
        );
    }
}
