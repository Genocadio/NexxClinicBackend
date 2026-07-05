package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.dto.out.UploadData;
import com.nexxserve.nexxclinic.entity.Upload;
import com.nexxserve.nexxclinic.model.UploadVisibility;
import com.nexxserve.nexxclinic.repository.UploadRepository;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadService.class);

    private final SupabaseStorageService storageService;
    private final SupabaseProperties supabaseProperties;
    private final UploadRepository uploadRepository;

    public UploadService(
            SupabaseStorageService storageService,
            SupabaseProperties supabaseProperties,
            UploadRepository uploadRepository
    ) {
        this.storageService = storageService;
        this.supabaseProperties = supabaseProperties;
        this.uploadRepository = uploadRepository;
    }

    @Transactional
    public ApiResponse uploadFile(MultipartFile file, UploadVisibility visibility) {
        if (file.isEmpty()) {
            return ApiResponse.error("File is empty");
        }

        try {
            String originalFileName = file.getOriginalFilename();
            String extension = getExtension(originalFileName);
            String storedName = UUID.randomUUID() + (extension.isBlank() ? "" : "." + extension);
            String contentType = file.getContentType();
            if (contentType == null || contentType.isBlank()) {
                contentType = "application/octet-stream";
            }

            String bucket = visibility == UploadVisibility.PUBLIC
                    ? supabaseProperties.getBucketPublic()
                    : supabaseProperties.getBucketPrivate();

            String storagePath = storedName;

            storageService.upload(file.getBytes(), bucket, storagePath, contentType);

            String url = null;
            if (visibility == UploadVisibility.PUBLIC) {
                url = storageService.publicUrl(bucket, storagePath);
            }

            Upload upload = new Upload();
            upload.setFileName(storedName);
            upload.setOriginalFileName(originalFileName);
            upload.setContentType(contentType);
            upload.setSize(file.getSize());
            upload.setBucket(bucket);
            upload.setVisibility(visibility);
            upload.setUrl(url);
            upload.setStoragePath(storagePath);

            upload = uploadRepository.save(upload);

            return ApiResponse.success("File uploaded successfully",
                    new UploadData(upload.getId(), upload.getUrl()));
        } catch (IOException e) {
            log.error("Failed to upload file to Supabase", e);
            return ApiResponse.error("Failed to upload file: " + e.getMessage());
        }
    }

    public ApiResponse findByUrl(String url) {
        return uploadRepository.findByUrl(url)
                .map(u -> ApiResponse.success("Upload found",
                        new UploadData(u.getId(), u.getUrl())))
                .orElse(ApiResponse.error("Upload not found for URL: " + url));
    }

    @Transactional
    public ApiResponse deleteUpload(UUID id) {
        return deleteByUpload(uploadRepository.findById(id).orElse(null));
    }

    @Transactional
    public ApiResponse deleteUploadByUrl(String url) {
        return deleteByUpload(uploadRepository.findByUrl(url).orElse(null));
    }

    private ApiResponse deleteByUpload(Upload upload) {
        if (upload == null) {
            return ApiResponse.error("Upload not found");
        }

        try {
            storageService.delete(upload.getBucket(), upload.getStoragePath());
        } catch (IOException e) {
            log.error("Failed to delete file from Supabase storage, removing DB record anyway", e);
        }

        uploadRepository.delete(upload);
        return ApiResponse.success("File deleted successfully", null);
    }

    @Transactional
    public ApiResponse updateUploadVisibility(UUID id, UploadVisibility newVisibility) {
        return updateVisibilityByUpload(uploadRepository.findById(id).orElse(null), newVisibility);
    }

    @Transactional
    public ApiResponse updateUploadVisibilityByUrl(String url, UploadVisibility newVisibility) {
        return updateVisibilityByUpload(uploadRepository.findByUrl(url).orElse(null), newVisibility);
    }

    @Transactional
    public ApiResponse updateVisibilityByUpload(Upload upload, UploadVisibility newVisibility) {
        if (upload == null) {
            return ApiResponse.error("Upload not found");
        }

        if (upload.getVisibility() == newVisibility) {
            return ApiResponse.success("Upload visibility updated",
                    new UploadData(upload.getId(), upload.getUrl()));
        }

        try {
            String newBucket = newVisibility == UploadVisibility.PUBLIC
                    ? supabaseProperties.getBucketPublic()
                    : supabaseProperties.getBucketPrivate();

            byte[] fileBytes;
            try (var is = new java.net.URL(storageService.publicUrl(upload.getBucket(), upload.getStoragePath())).openStream()) {
                fileBytes = is.readAllBytes();
            }

            storageService.upload(fileBytes, newBucket, upload.getStoragePath(), upload.getContentType());

            storageService.delete(upload.getBucket(), upload.getStoragePath());

            String url = null;
            if (newVisibility == UploadVisibility.PUBLIC) {
                url = storageService.publicUrl(newBucket, upload.getStoragePath());
            }

            upload.setBucket(newBucket);
            upload.setVisibility(newVisibility);
            upload.setUrl(url);

            upload = uploadRepository.save(upload);

            return ApiResponse.success("Upload visibility updated",
                    new UploadData(upload.getId(), upload.getUrl()));
        } catch (IOException e) {
            log.error("Failed to update upload visibility", e);
            return ApiResponse.error("Failed to update upload: " + e.getMessage());
        }
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
