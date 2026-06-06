package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.dto.out.FileInfoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Set;
import java.util.UUID;

@Service
public class UploadService {

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private static final Set<String> DOC_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    private static final Set<String> ARCHIVE_TYPES = Set.of(
            "application/zip",
            "application/x-rar-compressed",
            "application/x-7z-compressed",
            "application/gzip"
    );

    private final Path rootPath;

    public UploadService(
            @Value("${app.upload-dir:uploads}") String uploadDir
    ) {
        this.rootPath = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public FileInfoResponse upload(MultipartFile file) throws IOException {

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        String folder = resolveFolder(contentType);

        Path uploadPath = rootPath.resolve(folder);
        Files.createDirectories(uploadPath);

        String extension = getExtension(file.getOriginalFilename());

        String storedName = UUID.randomUUID() +
                (extension.isBlank() ? "" : "." + extension);

        Path target = uploadPath.resolve(storedName);

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        return new FileInfoResponse(
                storedName,
                file.getOriginalFilename(),
                "/api/files/" + folder + "/" + storedName,
                contentType,
                file.getSize()
        );
    }

    public Resource load(String folder, String filename)
            throws MalformedURLException {

        Path file = rootPath
                .resolve(folder)
                .resolve(filename)
                .normalize();

        Resource resource = new UrlResource(file.toUri());

        if (!resource.exists()) {
            throw new RuntimeException("File not found");
        }

        return resource;
    }

    private String resolveFolder(String contentType) {

        if (contentType == null) {
            return "others";
        }

        if (IMAGE_TYPES.contains(contentType)) {
            return "images";
        }

        if (DOC_TYPES.contains(contentType)) {
            return "docs";
        }

        if (ARCHIVE_TYPES.contains(contentType)) {
            return "archives";
        }

        return "others";
    }

    private String getExtension(String filename) {

        if (!StringUtils.hasText(filename)) {
            return "";
        }

        int index = filename.lastIndexOf('.');

        if (index < 0) {
            return "";
        }

        return filename.substring(index + 1);
    }
}