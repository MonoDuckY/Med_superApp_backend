package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;

import com.yourproject.backend.dtos.requests.NewsRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.NewsResponse;
import com.yourproject.backend.services.NewsService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/news")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffNewsController {
    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("News articles retrieved successfully.", newsService.getStaffNews()));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object"),
            encoding = {
                    @Encoding(name = "news", contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "coverPhoto", contentType = "image/jpeg, image/png, image/webp"),
                    @Encoding(name = "attachments", contentType = "image/jpeg, image/png, image/webp")
            }))
    public ResponseEntity<ApiResponse<NewsResponse>> create(
            Authentication authentication,
            @Valid @RequestPart("news") NewsRequest request,
            @RequestPart(value = "coverPhoto", required = false) MultipartFile coverPhoto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        NewsResponse response = newsService.create(authentication.getName(), request, coverPhoto);
        if (attachments != null && attachments.stream().anyMatch(file -> file != null && !file.isEmpty())) {
            response = newsService.addContentImages(response.getNewsId(), attachments);
        }
        return ResponseEntity.ok(ApiResponse.success("News draft created successfully.",
                response));
    }

    @PatchMapping(value = "/{newsId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object"),
            encoding = {
                    @Encoding(name = "news", contentType = MediaType.APPLICATION_JSON_VALUE),
                    @Encoding(name = "coverPhoto", contentType = "image/jpeg, image/png, image/webp"),
                    @Encoding(name = "attachments", contentType = "image/jpeg, image/png, image/webp")
            }))
    public ResponseEntity<ApiResponse<NewsResponse>> update(
            Authentication authentication,
            @PathVariable String newsId,
            @Valid @RequestPart("news") NewsRequest request,
            @RequestPart(value = "coverPhoto", required = false) MultipartFile coverPhoto,
            @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments) {
        NewsResponse response = newsService.update(authentication.getName(), newsId, request, coverPhoto);
        if (attachments != null && attachments.stream().anyMatch(file -> file != null && !file.isEmpty())) {
            response = newsService.addContentImages(newsId, attachments);
        }
        return ResponseEntity.ok(ApiResponse.success("News draft updated successfully.",
                response));
    }

    @PostMapping(value = "/{newsId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RequestBody(content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            schema = @Schema(type = "object"),
            encoding = @Encoding(name = "images", contentType = "image/jpeg, image/png, image/webp")))
    public ResponseEntity<ApiResponse<NewsResponse>> addContentImages(
            @PathVariable String newsId,
            @RequestPart("images") List<MultipartFile> images) {
        return ResponseEntity.ok(ApiResponse.success("News content images uploaded successfully.",
                newsService.addContentImages(newsId, images)));
    }

    @PatchMapping("/{newsId}/publish")
    public ResponseEntity<ApiResponse<NewsResponse>> publish(@PathVariable String newsId) {
        return ResponseEntity.ok(ApiResponse.success("News published successfully.", newsService.publish(newsId)));
    }

    @PatchMapping("/{newsId}/disable")
    public ResponseEntity<ApiResponse<NewsResponse>> disable(@PathVariable String newsId) {
        return ResponseEntity.ok(ApiResponse.success("News disabled successfully.", newsService.disable(newsId)));
    }
}
