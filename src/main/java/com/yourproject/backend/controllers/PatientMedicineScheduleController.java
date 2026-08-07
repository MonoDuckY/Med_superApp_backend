package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.MedicineScheduleResponse;
import com.yourproject.backend.services.ClinicalMedicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/medicine-schedules")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientMedicineScheduleController {
    private final ClinicalMedicationService clinicalMedicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineScheduleResponse>>> getMedicineSchedules(
            Authentication authentication) {
        return ResponseEntity.ok(ApiResponse.success(
                "Medicine schedules retrieved successfully.",
                clinicalMedicationService.getPatientMedicineSchedules(authentication.getName())));
    }

    @PatchMapping("/{scheduleId}/time")
    public ResponseEntity<ApiResponse<MedicineScheduleResponse>> updateScheduleTime(
            Authentication authentication,
            @PathVariable String scheduleId,
            @Valid @RequestBody UpdateMedicineScheduleTimeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Medicine schedule time updated successfully.",
                clinicalMedicationService.updatePatientScheduleTime(
                        authentication.getName(), scheduleId, request)));
    }

    @PatchMapping("/{scheduleId}/take")
    public ResponseEntity<ApiResponse<MedicineScheduleResponse>> markTaken(
            Authentication authentication,
            @PathVariable String scheduleId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Medicine marked as taken successfully.",
                clinicalMedicationService.markMedicineTaken(authentication.getName(), scheduleId)));
    }
}
