package com.nexxserve.nexxclinic.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Validates Supabase storage configuration at startup.
 * If {@code supabase.storage-type=SUPABASE}, the app must have a valid
 * {@code supabase.url} and {@code supabase.service-key} — otherwise uploads
 * and signed-URL generation will fail at runtime with cryptic HTTP errors.
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
}
