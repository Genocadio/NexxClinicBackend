package com.nexxserve.nexxclinic.controller;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import com.nexxserve.nexxclinic.service.FileStorageService;
import com.nexxserve.nexxclinic.service.LocalStorageService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves files stored locally when {@code supabase.storage-type=LOCAL}.
 * <p>
 * The frontend accesses files at {@code /api/media/{bucket}/{path}}, which maps
 * to {@code storage/{bucket}/{path}} on disk. This endpoint is only active in
 * LOCAL mode — in SUPABASE mode the Next.js proxy handles file serving.
 * <p>
 * Paths are normalized to prevent directory traversal attacks.
 */
@RestController
@RequestMapping("/api/media")
public class MediaController {

    private final FileStorageService storageService;
    private final SupabaseProperties props;

    public MediaController(FileStorageService storageService, SupabaseProperties props) {
        this.storageService = storageService;
        this.props = props;
    }

    @GetMapping("/{bucket}/{path:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String bucket,
            @PathVariable String path
    ) {
        // Only serve in LOCAL mode
        if (!props.isLocalMode()) {
            return ResponseEntity.notFound().build();
        }

        // Only LocalStorageService has the resolve() method
        if (!(storageService instanceof LocalStorageService local)) {
            return ResponseEntity.notFound().build();
        }

        Path file = local.resolve(bucket, path);
        if (file == null) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = determineContentType(path);

        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
            .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
            .body(resource);
    }

    private String determineContentType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".pdf")) return "application/pdf";
        if (lower.endsWith(".mp4")) return "video/mp4";
        if (lower.endsWith(".webm")) return "video/webm";
        if (lower.endsWith(".mp3")) return "audio/mpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }
}
