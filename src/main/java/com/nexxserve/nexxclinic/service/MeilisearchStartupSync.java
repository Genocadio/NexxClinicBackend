package com.nexxserve.nexxclinic.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * On startup (after the context is ready), makes sure the Meilisearch indexes
 * exist with the right settings and rebuilds any index that is empty or stale
 * (DB row count ≠ Meilisearch document count), so search works immediately even
 * after direct DB changes (seeds, psql edits, migrations).
 *
 * <p>Runs asynchronously so a slow/absent Meilisearch never blocks application boot.</p>
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

            String[] uids = {
                    MeilisearchIndexService.PRODUCTS_INDEX,
                    MeilisearchIndexService.PATIENTS_INDEX,
                    MeilisearchIndexService.WORKERS_INDEX
            };

            for (String uid : uids) {
                long meiliCount = indexService.getDocumentCount(uid);
                long dbCount = indexService.getDbCount(uid);

                if (meiliCount < 0) {
                    // Index doesn't exist or Meilisearch is unreachable — full rebuild
                    log.info("Rebuilding Meilisearch index '{}' (index unavailable)…", uid);
                    indexService.reindex(uid);
                } else if (meiliCount == 0 && dbCount > 0) {
                    // Index is empty but DB has data — seed it
                    log.info("Seeding empty Meilisearch index '{}' ({} DB rows)…", uid, dbCount);
                    indexService.reindex(uid);
                } else if (dbCount > 0 && meiliCount != dbCount) {
                    // Counts differ — index is stale (direct DB inserts/deletes happened)
                    log.info("Rebuilding stale Meilisearch index '{}': {} Meilisearch vs {} DB rows…",
                            uid, meiliCount, dbCount);
                    indexService.reindex(uid);
                } else {
                    log.debug("Meilisearch index '{}' is up to date ({} documents)", uid, meiliCount);
                }
            }
        } catch (Exception e) {
            log.warn("Meilisearch startup sync skipped: {}", e.getMessage());
        }
    }
}
