package com.nexxserve.nexxclinic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup (after the context is ready), makes sure the Meilisearch indexes
 * exist with the right settings and seeds any index that is still empty, so
 * search works immediately even on pre-existing databases. Runs asynchronously
 * so a slow/absent Meilisearch never blocks application boot.
 */
@Component
public class MeilisearchStartupSync implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchStartupSync.class);

    private final MeilisearchIndexService indexService;

    public MeilisearchStartupSync(MeilisearchIndexService indexService) {
        this.indexService = indexService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!indexService.isEnabled()) {
            log.info("Meilisearch is disabled; search falls back to the database.");
            return;
        }
        Thread syncThread = new Thread(this::sync, "meilisearch-startup-sync");
        syncThread.setDaemon(true);
        syncThread.start();
    }

    private void sync() {
        try {
            indexService.ensureIndexes();
            // Only rebuild the indexes that are actually empty — never touch
            // healthy indexes (avoids a delete-all window on populated data).
            String[] uids = {
                    MeilisearchIndexService.PRODUCTS_INDEX,
                    MeilisearchIndexService.PATIENTS_INDEX,
                    MeilisearchIndexService.WORKERS_INDEX
            };
            for (String uid : uids) {
                if (!indexService.hasDocuments(uid)) {
                    log.info("Seeding empty Meilisearch index '{}' from the database…", uid);
                    indexService.reindex(uid);
                }
            }
        } catch (Exception e) {
            log.warn("Meilisearch startup sync skipped: {}", e.getMessage());
        }
    }
}
