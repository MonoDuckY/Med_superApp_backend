package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.ResearcherMedicalImageResponse;
import com.yourproject.backend.services.ResearcherMedicalRecordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/researcher/medical-records")
@PreAuthorize("hasRole('RESEARCHER')")
@RequiredArgsConstructor
public class ResearcherMedicalRecordController {
    private final ResearcherMedicalRecordService researcherMedicalRecordService;

    @GetMapping("/images")
    public ResponseEntity<ApiResponse<List<ResearcherMedicalImageResponse>>> getAllMedicalRecordImages() {
        return ResponseEntity.ok(ApiResponse.success(
                "Medical record images retrieved successfully.",
                researcherMedicalRecordService.getAllMedicalRecordImages()));
    }
}
