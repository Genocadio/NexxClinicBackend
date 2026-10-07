package com.nexxserve.nexxclinic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates the selected storage provider configuration at startup.
 * If {@code supabase.storage-type=SUPABASE}, the app must have a valid
 * {@code supabase.url} and {@code supabase.service-key} — otherwise uploads
 * and signed-URL generation will fail at runtime with cryptic HTTP errors.
 * MinIO mode similarly requires its endpoint and credentials.
 */
@Component
@Order(0)
public class SupabaseConfigValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SupabaseConfigValidator.class);

    private final SupabaseProperties props;

    public SupabaseConfigValidator(SupabaseProperties props) {
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (props.isMinioMode()) {
            validateMinioConfiguration();
            log.info("Storage mode: MINIO (endpoint={})", props.getMinioEndpoint());
            return;
        }

        if (!props.isSupabaseMode()) {
            log.info("Storage mode: LOCAL (files served via /api/media/)");
            return;
        }

        // Validate required Supabase properties
        boolean valid = true;

        if (props.getUrl() == null || props.getUrl().isBlank()
                || props.getUrl().contains("localhost:8000")) {
            log.error("STORAGE_TYPE=SUPABASE but supabase.url is not set (or still points to localhost). "
                    + "Set SUPABASE_URL env var to your self-hosted Supabase instance URL.");
            valid = false;
        }

        if (props.getServiceKey() == null || props.getServiceKey().isBlank()
                || props.getServiceKey().equals("YOUR_SERVICE_ROLE_KEY")) {
            log.error("STORAGE_TYPE=SUPABASE but supabase.service-key is not set. "
                    + "Set SUPABASE_SERVICE_KEY env var to your Supabase service-role key.");
            valid = false;
        }

        if (!valid) {
            throw new IllegalStateException(
                    "Supabase storage is configured (STORAGE_TYPE=SUPABASE) but required properties are missing. "
                    + "Set SUPABASE_URL and SUPABASE_SERVICE_KEY environment variables, "
                    + "or switch to STORAGE_TYPE=LOCAL for development.");
        }

        log.info("Storage mode: SUPABASE (url={})", props.getUrl());
    }

    private void validateMinioConfiguration() {
        boolean valid = true;
        if (props.getMinioEndpoint() == null || props.getMinioEndpoint().isBlank()) {
            log.error("STORAGE_TYPE=MINIO but supabase.minio-endpoint is not set.");
            valid = false;
        }
        if (props.getMinioAccessKey() == null || props.getMinioAccessKey().isBlank()) {
            log.error("STORAGE_TYPE=MINIO but supabase.minio-access-key is not set.");
            valid = false;
        }
        if (props.getMinioSecretKey() == null || props.getMinioSecretKey().isBlank()) {
            log.error("STORAGE_TYPE=MINIO but supabase.minio-secret-key is not set.");
            valid = false;
        }
        if (props.getBucketInvoices() != null
                && props.getBucketInvoices().equals(props.getBucketPublic())) {
            log.error("The invoice bucket must not be the public MinIO uploads bucket.");
            valid = false;
        }
        if (!valid) {
            throw new IllegalStateException(
                "MinIO storage configuration is invalid. Check the logged errors; "
                    + "required connection settings are MINIO_ENDPOINT, MINIO_ACCESS_KEY, "
                    + "and MINIO_SECRET_KEY."
            );
        }
    }
}
