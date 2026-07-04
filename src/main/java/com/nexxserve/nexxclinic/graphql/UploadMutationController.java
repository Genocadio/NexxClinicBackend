package com.nexxserve.nexxclinic.graphql;

import com.nexxserve.nexxclinic.dto.out.ApiResponse;
import com.nexxserve.nexxclinic.model.UploadVisibility;
import com.nexxserve.nexxclinic.service.UploadService;
import java.util.UUID;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class UploadMutationController {

    private final UploadService uploadService;

    public UploadMutationController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @MutationMapping
    public ApiResponse uploadFile(
            @Argument MultipartFile file,
            @Argument UploadVisibility visibility
    ) {
        return uploadService.uploadFile(file, visibility);
    }

    @MutationMapping
    public ApiResponse deleteUpload(
            @Argument UUID id
    ) {
        return uploadService.deleteUpload(id);
    }

    @MutationMapping
    public ApiResponse updateUploadVisibility(
            @Argument UUID id,
            @Argument UploadVisibility visibility
    ) {
        return uploadService.updateUploadVisibility(id, visibility);
    }
}
