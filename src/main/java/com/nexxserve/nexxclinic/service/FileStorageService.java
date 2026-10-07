package com.nexxserve.nexxclinic.service;

import java.io.IOException;

/**
 * Strategy interface for file storage. The system supports three backends:
 * <ul>
 *   <li>{@link LocalStorageService} — saves files to the local filesystem</li>
 *   <li>{@link SupabaseStorageService} — uploads to a self-hosted Supabase instance</li>
 *   <li>{@link MinioStorageService} — uploads to an S3-compatible MinIO instance</li>
 * </ul>
 * The active implementation is selected via {@code supabase.storage-type} in
 * application.yaml ({@code LOCAL}, {@code SUPABASE}, or {@code MINIO}).
 */
public interface FileStorageService {

    /**
     * Uploads data to the configured storage backend.
     *
     * @param data        the file bytes
     * @param bucket      logical bucket/folder (e.g. "uploads-public", "invoices")
     * @param objectPath  the storage path within the bucket (e.g. "uuid.png")
     * @param contentType MIME type of the file
     */
    void upload(byte[] data, String bucket, String objectPath, String contentType) throws IOException;

    /**
     * Downloads the full object contents from the configured storage backend.
     */
    byte[] download(String bucket, String objectPath) throws IOException;

    /**
     * Ensures a directory-like prefix exists for manual browsing. Object stores
     * may represent this with a marker object.
     */
    void ensureDirectory(String bucket, String directoryPath) throws IOException;

    /**
     * Moves all stored objects below a directory-like prefix, preserving its
     * contents. Used when mutable visit-date components of archive paths change.
     */
    void movePrefix(String bucket, String sourcePrefix, String destinationPrefix) throws IOException;

    /**
     * Deletes a file from storage. Best-effort: may throw IOException for Supabase errors.
     */
    void delete(String bucket, String objectPath) throws IOException;

    /**
     * Returns a URL that can be used to access the file.
     * For LOCAL mode this is a relative path like {@code /api/media/uploads-public/uuid.png}.
     * Object-storage modes return the provider's public URL.
     */
    String getPublicUrl(String bucket, String objectPath);

    /**
     * Returns a signed/temporary URL for accessing a private file.
     * For LOCAL mode this is the same as getPublicUrl (local files are served directly).
     * Object-storage modes generate a time-limited signed URL.
     */
    String getSignedUrl(String bucket, String objectPath, int expiresInSeconds) throws IOException;

    /**
     * Returns the raw storage path as stored in the DB.
     * For LOCAL mode this is the relative path within localStoragePath.
     * Object-storage modes return the provider's object path.
     */
    String getStoragePath(String bucket, String objectPath);
}
