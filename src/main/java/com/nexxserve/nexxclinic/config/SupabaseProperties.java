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
}
