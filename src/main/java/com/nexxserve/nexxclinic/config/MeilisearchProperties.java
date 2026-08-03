package com.nexxserve.nexxclinic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds the {@code meilisearch.*} block from application.yaml.
 *
 * <pre>
 * meilisearch:
 *   url: http://localhost:7700
 *   api-key: nexxclinic_meili_master_key
 *   enabled: true
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "meilisearch")
public class MeilisearchProperties {

    /** Base URL of the Meilisearch instance (no trailing slash). */
    private String url = "http://localhost:7700";

    /** Master/search API key used to talk to Meilisearch. */
    private String apiKey = "nexxclinic_meili_master_key";

    /** When false, search queries fall back to the database entirely. */
    private boolean enabled = true;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
