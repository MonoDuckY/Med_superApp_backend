package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CloneMedicalImageFolderRequest {
    private String sourceFolderName;

    @NotBlank(message = "Target folder name is required.")
    private String targetFolderName;
}
