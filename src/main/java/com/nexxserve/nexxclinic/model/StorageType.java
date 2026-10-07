package com.nexxserve.nexxclinic.model;

/**
 * Determines where uploaded files (profile photos, insurance docs, invoices) are stored.
 * <ul>
 *   <li>{@code LOCAL} — files saved to the local filesystem under a configurable directory;
 *       served by the backend via {@code /api/media/}.</li>
 *   <li>{@code SUPABASE} — files uploaded to a self-hosted Supabase Storage instance;
 *       served by the Next.js proxy at {@code /supa/}.</li>
 *   <li>{@code MINIO} — files stored in an S3-compatible MinIO bucket.</li>
 * </ul>
 */
public enum StorageType {
    LOCAL,
    SUPABASE,
    MINIO
}
