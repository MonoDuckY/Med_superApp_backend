package com.yourproject.backend.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.RespondFeedbackRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.FeedbackResponse;
import com.yourproject.backend.services.FeedbackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/feedback")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffFeedbackController {
    private final FeedbackService feedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(
                "Feedback retrieved successfully.", feedbackService.getAll()));
    }

    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackResponse>> get(@PathVariable String feedbackId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Feedback retrieved successfully.", feedbackService.get(feedbackId)));
    }

    @PatchMapping("/{feedbackId}/response")
    public ResponseEntity<ApiResponse<FeedbackResponse>> respond(
            Authentication authentication,
            @PathVariable String feedbackId,
            @Valid @RequestBody RespondFeedbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Feedback response submitted successfully.",
                feedbackService.respond(authentication.getName(), feedbackId, request)));
    }
}
