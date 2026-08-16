package com.yourproject.backend.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.MedicalImageResponse;
import com.yourproject.backend.dtos.requests.CloneMedicalImageFolderRequest;
import com.yourproject.backend.services.S3StorageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/researcher/medical-images")
@PreAuthorize("hasRole('RESEARCHER')")
@RequiredArgsConstructor
public class ResearcherMedicalImageController {
    private final S3StorageService s3StorageService;

    @GetMapping("/folders")
    public ResponseEntity<ApiResponse<List<String>>> getAvailableFolders() {
        return ResponseEntity.ok(ApiResponse.success(
                "Medical image folders retrieved successfully.",
                s3StorageService.listMedicalImageFolders()));
    }

    @GetMapping("/folders/{folderName}")
    public ResponseEntity<ApiResponse<List<MedicalImageResponse>>> getImagesByFolder(
            @PathVariable String folderName) {
        List<MedicalImageResponse> images = s3StorageService.listMedicalImagesByFolder(folderName).stream()
                .map(image -> MedicalImageResponse.builder()
                        .imageId(image.objectKey())
                        .url(image.url())
                        .expiresAt(image.expiresAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.success(
                "Medical images retrieved successfully.", images));
    }

    @PostMapping("/folders/clone")
    public ResponseEntity<ApiResponse<Void>> cloneFolder(
            @Valid @RequestBody CloneMedicalImageFolderRequest request) {
        s3StorageService.cloneMedicalImageFolder(request.getSourceFolderName(), request.getTargetFolderName());
        return ResponseEntity.ok(ApiResponse.success("Medical image folder created successfully.", null));
    }

    @PostMapping(value = "/folders/{folderName}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> uploadImage(
            @PathVariable String folderName,
            @RequestParam("file") MultipartFile file) {
        String objectKey = s3StorageService.uploadMedicalImageToFolder(folderName, file);
        return ResponseEntity.ok(ApiResponse.success("Medical image uploaded successfully.", objectKey));
    }
}
