package com.yourproject.backend.controllers;

import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.CreateFeedbackRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.FeedbackResponse;
import com.yourproject.backend.services.FeedbackService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/feedback")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientFeedbackController {
    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> submit(
            Authentication authentication,
            @Valid @RequestBody CreateFeedbackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Feedback submitted successfully.",
                feedbackService.submit(authentication.getName(), request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<FeedbackResponse>>> getMine(Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Patient feedback retrieved successfully.",
                feedbackService.getPatientFeedback(authentication.getName())));
    }
}
