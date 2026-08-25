package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Stores files on the local filesystem. Used when {@code supabase.storage-type=LOCAL}.
 * <p>
 * Directory layout under {@code supabase.local-storage-path} (default {@code ./storage}):
 * <pre>
 *   storage/
 *     uploads-public/    ← public files (profile photos, logos, insurance docs)
 *     uploads-private/   ← private files
 *     invoices/          ← generated invoice PDFs
 * </pre>
 * Files are served by the backend via {@code /api/media/} and the frontend resolves
 * these URLs through {@code getMediaUrl()} → {@code /api/media/...}.
 */
@Service
@ConditionalOnProperty(name = "supabase.storage-type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);

    private final SupabaseProperties props;
    private final Path storageRoot;

    public LocalStorageService(SupabaseProperties props) {
        this.props = props;
        this.storageRoot = Path.of(props.getLocalStoragePath()).toAbsolutePath();
        initDirectories();
    }

    private void initDirectories() {
        try {
            // Legacy paths (without bucket prefix) — kept for backward compat.
            Files.createDirectories(storageRoot.resolve("uploads-public"));
            Files.createDirectories(storageRoot.resolve("uploads-private"));
            Files.createDirectories(storageRoot.resolve("invoices"));

            // Bucket-prefixed paths — matches how upload() resolves:
            //   storageRoot.resolve(bucket).resolve(objectPath)
            String bucketPublic = props.getBucketPublic();
            String bucketPrivate = props.getBucketPrivate();
            String bucketInvoices = props.getBucketInvoices();
            if (bucketPublic != null && !bucketPublic.isBlank()) {
                Files.createDirectories(storageRoot.resolve(bucketPublic));
            }
            if (bucketPrivate != null && !bucketPrivate.isBlank()) {
                Files.createDirectories(storageRoot.resolve(bucketPrivate));
            }
            if (bucketInvoices != null && !bucketInvoices.isBlank()) {
                Files.createDirectories(storageRoot.resolve(bucketInvoices).resolve("invoices"));
            }
            log.info("Local storage initialized at {}", storageRoot);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Cannot create local storage directories under " + storageRoot, e
            );
        }
    }

    @Override
    public void upload(byte[] data, String bucket, String objectPath, String contentType)
            throws IOException {
        Path target = storageRoot.resolve(bucket).resolve(objectPath);
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        log.debug("Local storage write: {}/{} ({} bytes)", bucket, objectPath, data.length);
    }

    @Override
    public void delete(String bucket, String objectPath) {
        try {
            Path target = storageRoot.resolve(bucket).resolve(objectPath);
            Files.deleteIfExists(target);
            log.debug("Local storage delete: {}/{}", bucket, objectPath);
        } catch (IOException e) {
            log.warn("Failed to delete local file {}/{}: {}", bucket, objectPath, e.getMessage());
        }
    }

    @Override
    public String getPublicUrl(String bucket, String objectPath) {
        // Return a relative URL that the backend serves via /api/media/
        return "/api/media/" + bucket + "/" + objectPath;
    }

    @Override
    public String getSignedUrl(String bucket, String objectPath, int expiresInSeconds) {
        // Local files don't need signed URLs — served directly
        return getPublicUrl(bucket, objectPath);
    }

    @Override
    public String getStoragePath(String bucket, String objectPath) {
        // Store as relative path within the bucket
        return objectPath;
    }

    /**
     * Resolves a bucket + relative path to an absolute Path on disk.
     * Returns null if the file does not exist.
     */
    public Path resolve(String bucket, String relativePath) {
        Path resolved = storageRoot.resolve(bucket).resolve(relativePath).normalize();
        // Security: ensure the resolved path is still under storageRoot
        if (!resolved.startsWith(storageRoot)) {
            log.warn("Path traversal attempt blocked: {}/{}", bucket, relativePath);
            return null;
        }
        return Files.exists(resolved) ? resolved : null;
    }
}
