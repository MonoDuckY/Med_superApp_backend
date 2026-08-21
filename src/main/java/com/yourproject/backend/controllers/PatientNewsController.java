package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.NewsResponse;
import com.yourproject.backend.services.NewsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/news")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientNewsController {
    private final NewsService newsService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getPublished() {
        return ResponseEntity.ok(ApiResponse.success("Published news retrieved successfully.",
                newsService.getPublishedNews()));
    }

    @GetMapping("/{newsId}")
    public ResponseEntity<ApiResponse<NewsResponse>> getOne(@PathVariable String newsId) {
        return ResponseEntity.ok(ApiResponse.success("Published news retrieved successfully.",
                newsService.getPublished(newsId)));
    }
}
