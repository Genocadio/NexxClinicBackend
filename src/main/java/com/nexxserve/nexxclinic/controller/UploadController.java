package com.nexxserve.nexxclinic.controller;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.UploadVisibility;
import com.nexxserve.nexxclinic.service.UploadService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @GetMapping("/by-url")
    public ResponseEntity<ApiResponse> findByUrl(@RequestParam("url") String url) {
        return ResponseEntity.ok(uploadService.findByUrl(url));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "visibility", defaultValue = "PUBLIC") UploadVisibility visibility
    ) {
        ApiResponse response = uploadService.uploadFile(file, visibility);
        return ResponseEntity.status(response.status().name().equals("SUCCESS") ? 200 : 400).body(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteUpload(@PathVariable UUID id) {
        return ResponseEntity.ok(uploadService.deleteUpload(id));
    }

    @DeleteMapping("/by-url")
    public ResponseEntity<ApiResponse> deleteUploadByUrl(@RequestParam("url") String url) {
        return ResponseEntity.ok(uploadService.deleteUploadByUrl(url));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse> updateVisibility(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body
    ) {
        String visibilityStr = body.get("visibility");
        if (visibilityStr == null || visibilityStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("visibility is required"));
        }
        try {
            UploadVisibility visibility = UploadVisibility.valueOf(visibilityStr.toUpperCase());
            return ResponseEntity.ok(uploadService.updateUploadVisibility(id, visibility));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid visibility value. Use PUBLIC or PRIVATE"));
        }
    }

    @PatchMapping("/by-url/visibility")
    public ResponseEntity<ApiResponse> updateVisibilityByUrl(
            @RequestParam("url") String url,
            @RequestBody Map<String, String> body
    ) {
        String visibilityStr = body.get("visibility");
        if (visibilityStr == null || visibilityStr.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("visibility is required"));
        }
        try {
            UploadVisibility visibility = UploadVisibility.valueOf(visibilityStr.toUpperCase());
            return ResponseEntity.ok(uploadService.updateUploadVisibilityByUrl(url, visibility));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid visibility value. Use PUBLIC or PRIVATE"));
        }
    }
}
