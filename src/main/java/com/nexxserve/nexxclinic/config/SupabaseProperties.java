package com.nexxserve.nexxclinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code supabase.*} block from application.yaml.
 *
 * <pre>
 * supabase:
 *   url: https://supa.med.rw
 *   service-key: YOUR_SERVICE_ROLE_KEY
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    /** Base URL of the self-hosted Supabase instance (no trailing slash). */
    private String url;

    /** Service-role key with full Storage access. */
    private String serviceKey;

    /** Bucket name for public uploads. */
    private String bucketPublic = "uploads-public";

    /** Bucket name for private uploads. */
    private String bucketPrivate = "uploads-private";

    /** Connect timeout (ms) for the Storage HTTP client. Default 5000. */
    private int connectTimeoutMs = 5000;

    /** Per-request timeout (ms). Default 15000 — invoices fail fast, never hang. */
    private int requestTimeoutMs = 15000;

    /**
     * Retries for transient failures (HTTP 429 / 5xx) on uploads and other storage
     * calls. 0 disables retry. Default 2.
     */
    private int retryMaxAttempts = 2;

    /** Base backoff (ms) between retries; doubles each attempt. Default 500. */
    private long retryBackoffMs = 500;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceKey() {
        return serviceKey;
    }

    public void setServiceKey(String serviceKey) {
        this.serviceKey = serviceKey;
    }

    public String getBucketPublic() {
        return bucketPublic;
    }

    public void setBucketPublic(String bucketPublic) {
        this.bucketPublic = bucketPublic;
    }

    public String getBucketPrivate() {
        return bucketPrivate;
    }

    public void setBucketPrivate(String bucketPrivate) {
        this.bucketPrivate = bucketPrivate;
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(int connectTimeoutMs) {
        this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(int requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public int getRetryMaxAttempts() {
        return retryMaxAttempts;
    }

    public void setRetryMaxAttempts(int retryMaxAttempts) {
        this.retryMaxAttempts = retryMaxAttempts;
    }

    public long getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(long retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }
}
