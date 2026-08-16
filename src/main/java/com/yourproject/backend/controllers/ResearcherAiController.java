package com.yourproject.backend.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import tools.jackson.databind.JsonNode;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.services.ResearcherAiDetectionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/researcher")
@PreAuthorize("hasRole('RESEARCHER')")
@RequiredArgsConstructor
public class ResearcherAiController {
    private final ResearcherAiDetectionService researcherAiDetectionService;

    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<JsonNode>> detect(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(
                "Image detection completed successfully.",
                researcherAiDetectionService.detect(file)));
    }
}
