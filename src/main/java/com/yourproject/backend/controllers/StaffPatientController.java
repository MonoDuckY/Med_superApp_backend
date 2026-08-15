package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.StaffCreatePatientRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.StaffPatientSearchResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.StaffPatientAccountService;
import com.yourproject.backend.services.StaffPatientSearchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/patients")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffPatientController {
    private final StaffPatientSearchService staffPatientSearchService;
    private final StaffPatientAccountService staffPatientAccountService;
    private final PatientDataProtectionService patientDataProtectionService;

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createPatient(
            Authentication authentication,
            @Valid @RequestBody StaffCreatePatientRequest request) {
        UserResponse patient = UserResponse.from(
                staffPatientAccountService.createPatient(request, authentication.getName()),
                patientDataProtectionService);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Patient account created successfully.", patient));
    }

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
