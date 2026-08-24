package com.nexxserve.nexxclinic.service;

import java.io.IOException;

/**
 * Strategy interface for file storage. The system supports two backends:
 * <ul>
 *   <li>{@link LocalStorageService} — saves files to the local filesystem</li>
 *   <li>{@link SupabaseStorageService} — uploads to a self-hosted Supabase instance</li>
 * </ul>
 * The active implementation is selected via {@code supabase.storage-type} in
 * application.yaml ({@code LOCAL} or {@code SUPABASE}).
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
     * Deletes a file from storage. Best-effort: may throw IOException for Supabase errors.
     */
    void delete(String bucket, String objectPath) throws IOException;

    /**
     * Returns a URL that can be used to access the file.
     * For LOCAL mode this is a relative path like {@code /api/media/uploads-public/uuid.png}.
     * For SUPABASE mode this is the Supabase public URL.
     */
    String getPublicUrl(String bucket, String objectPath);

    /**
     * Returns a signed/temporary URL for accessing a private file.
     * For LOCAL mode this is the same as getPublicUrl (local files are served directly).
     * For SUPABASE mode this generates a time-limited signed URL.
     */
    String getSignedUrl(String bucket, String objectPath, int expiresInSeconds) throws IOException;

    /**
     * Returns the raw storage path as stored in the DB.
     * For LOCAL mode this is the relative path within localStoragePath.
     * For SUPABASE mode this is the Supabase object path.
     */
    String getStoragePath(String bucket, String objectPath);
}
