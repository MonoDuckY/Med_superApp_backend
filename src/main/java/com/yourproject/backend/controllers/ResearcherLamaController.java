package com.yourproject.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.services.ResearcherLamaService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/researcher")
@PreAuthorize("hasRole('RESEARCHER')")
@RequiredArgsConstructor
public class ResearcherLamaController {
    private final ResearcherLamaService researcherLamaService;

    @PostMapping(value = {"/LaMa", "/LaMa/"}, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<byte[]> inpaint(@RequestPart("image") MultipartFile image) {
        ResearcherLamaService.ProcessedImage processedImage = researcherLamaService.inpaint(image);
        return ResponseEntity.ok()
                .contentType(processedImage.contentType())
                .body(processedImage.content());
    }
}
