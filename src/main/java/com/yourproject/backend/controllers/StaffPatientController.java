package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.StaffPatientSearchResponse;
import com.yourproject.backend.services.StaffPatientSearchService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/patients")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffPatientController {
    private final StaffPatientSearchService staffPatientSearchService;

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<StaffPatientSearchResponse>>> searchPatients(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String citizenIdentificationCode,
            @RequestParam(defaultValue = "10") int n) {
        return ResponseEntity.ok(ApiResponse.success(
                "Matching patients retrieved successfully.",
                staffPatientSearchService.search(name, phoneNumber, citizenIdentificationCode, n)));
    }
}
