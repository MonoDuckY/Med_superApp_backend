package com.yourproject.backend.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import com.yourproject.backend.services.ResearcherLamaService;
import com.yourproject.backend.services.ResearcherImageCompareService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/researcher")
@PreAuthorize("hasRole('RESEARCHER')")
@RequiredArgsConstructor
public class ResearcherLamaController {
    private final ResearcherLamaService researcherLamaService;
    private final ResearcherImageCompareService researcherImageCompareService;

    @PostMapping(value = "/LaMa", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = @Encoding(name = "image", contentType = "image/png, image/jpeg, image/webp")))
    public ResponseEntity<byte[]> inpaint(@RequestPart("image") MultipartFile image) {
        ResearcherLamaService.ProcessedImage processedImage = researcherLamaService.inpaint(image);
        return ResponseEntity.ok()
                .contentType(processedImage.contentType())
                .body(processedImage.content());
    }

    @PostMapping(value = "/compare", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = {
                    @Encoding(name = "image1", contentType = "image/png, image/jpeg, image/webp"),
                    @Encoding(name = "image2", contentType = "image/png, image/jpeg, image/webp")
            }))
    public ResponseEntity<byte[]> compare(
            @RequestPart("image1") MultipartFile image1,
            @RequestPart("image2") MultipartFile image2) {
        ResearcherImageCompareService.ProcessedImage comparedImage =
                researcherImageCompareService.compare(image1, image2);
        return ResponseEntity.ok()
                .contentType(comparedImage.contentType())
                .body(comparedImage.content());
    }
}
