package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Stores files on the local filesystem. Used when {@code supabase.storage-type=LOCAL}.
 * <p>
 * Bucket directories are created under {@code supabase.local-storage-path}
 * (default {@code ./storage}):
 * <pre>
 *   storage/
 *     uploads-public/    ← public files (profile photos, logos, insurance docs)
 *     uploads-private/   ← private files
 *     clinic-files/      ← private visit archives and generated invoice PDFs
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
        Path target = resolveSafe(bucket, objectPath);
        Files.createDirectories(target.getParent());
        Files.write(target, data);
        log.debug("Local storage write: {}/{} ({} bytes)", bucket, objectPath, data.length);
    }

    @Override
    public byte[] download(String bucket, String objectPath) throws IOException {
        return Files.readAllBytes(resolveSafe(bucket, objectPath));
    }

    @Override
    public void ensureDirectory(String bucket, String directoryPath) throws IOException {
        Path directory = resolveSafe(bucket, directoryPath);
        Files.createDirectories(directory);
    }

    @Override
    public void movePrefix(String bucket, String sourcePrefix, String destinationPrefix)
            throws IOException {
        Path source = resolveSafe(bucket, sourcePrefix);
        Path destination = resolveSafe(bucket, destinationPrefix);
        if (!Files.exists(source)) {
            return;
        }
        if (source.equals(destination)) {
            return;
        }

        Files.createDirectories(destination.getParent());
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.naturalOrder()).toList()) {
                Path target = destination.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        try (var paths = Files.walk(source)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
        log.info("Moved local storage prefix {}/{} to {}", bucket, sourcePrefix, destinationPrefix);
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
        Path bucketRoot = storageRoot.resolve(bucket).normalize();
        Path resolved = bucketRoot.resolve(relativePath).normalize();
        // Keep resolved media within its requested bucket.
        if (!resolved.startsWith(bucketRoot)) {
            log.warn("Path traversal attempt blocked: {}/{}", bucket, relativePath);
            return null;
        }
        return Files.exists(resolved) ? resolved : null;
    }

    private Path resolveSafe(String bucket, String relativePath) throws IOException {
        Path bucketRoot = storageRoot.resolve(bucket).normalize();
        Path resolved = bucketRoot.resolve(relativePath).normalize();
        if (!bucketRoot.startsWith(storageRoot) || !resolved.startsWith(bucketRoot)) {
            throw new IOException("Storage path escapes the configured root");
        }
        return resolved;
    }
}
