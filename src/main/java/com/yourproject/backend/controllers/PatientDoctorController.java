package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.PatientDoctorResponse;
import com.yourproject.backend.services.DoctorDirectoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/doctors")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientDoctorController {
    private final DoctorDirectoryService doctorDirectoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PatientDoctorResponse>>> getDoctors() {
        return ResponseEntity.ok(ApiResponse.success(
                "Active doctors retrieved successfully.",
                doctorDirectoryService.getActiveDoctors()));
    }
}
