package com.nexxserve.nexxclinic.config;

import com.nexxserve.nexxclinic.model.StorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code supabase.*} block from application.yaml.
 *
 * <pre>
 * supabase:
 *   url: https://supa.med.rw
 *   service-key: YOUR_SERVICE_ROLE_KEY
 *   storage-type: LOCAL  # or SUPABASE
 *   local-storage-path: ./storage
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "supabase")
public class SupabaseProperties {

    /**
     * Storage backend type. When {@code LOCAL}, files are saved to the filesystem
     * and served by the backend. When {@code SUPABASE}, files are uploaded to
     * Supabase Storage via HTTP. Defaults to {@code LOCAL} so the system works
     * out of the box without Supabase configured.
     */
    private StorageType storageType = StorageType.LOCAL;

    /** Base URL of the self-hosted Supabase instance (no trailing slash). Ignored when storageType=LOCAL. */
    private String url;

    /** Service-role key with full Storage access. Ignored when storageType=LOCAL. */
    private String serviceKey;

    /**
     * Root directory for local file storage (relative to working dir or absolute).
     * Only used when storageType=LOCAL. Subdirectories: uploads/, invoices/.
     */
    private String localStoragePath = "./storage";

    /** Bucket name for public uploads. */
    private String bucketPublic = "uploads-public";

    /** Bucket name for private uploads. */
    private String bucketPrivate = "uploads-private";

    /** Bucket name for generated invoices. Default "data". */
    private String bucketInvoices = "data";

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

    public StorageType getStorageType() {
        return storageType;
    }

    public void setStorageType(StorageType storageType) {
        this.storageType = storageType;
    }

    public String getLocalStoragePath() {
        return localStoragePath;
    }

    public void setLocalStoragePath(String localStoragePath) {
        this.localStoragePath = localStoragePath;
    }

    public boolean isLocalMode() {
        return storageType == StorageType.LOCAL;
    }

    public boolean isSupabaseMode() {
        return storageType == StorageType.SUPABASE;
    }

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

    public String getBucketInvoices() {
        return bucketInvoices;
    }

    public void setBucketInvoices(String bucketInvoices) {
        this.bucketInvoices = bucketInvoices;
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
