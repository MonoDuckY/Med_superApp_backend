package com.yourproject.backend.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.UpdateClinicalInformationRequest;
import com.yourproject.backend.dtos.requests.UpdateDiagnosisRequest;
import com.yourproject.backend.dtos.requests.UpsertPrescriptionRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.DoctorExaminationResponse;
import com.yourproject.backend.dtos.responses.PrescriptionResponse;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.services.ClinicalMedicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctor/appointments")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorExaminationController {
    private final ClinicalMedicationService clinicalMedicationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            Authentication authentication,
            @RequestParam(required = false) AppointmentStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Doctor appointments retrieved successfully.",
                clinicalMedicationService.getDoctorAppointments(authentication.getName(), status)));
    }

    @GetMapping("/{appointmentId}")
    public ResponseEntity<ApiResponse<DoctorExaminationResponse>> getExamination(
            Authentication authentication,
            @PathVariable String appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Examination information retrieved successfully.",
                clinicalMedicationService.getDoctorExamination(authentication.getName(), appointmentId)));
    }

    @PatchMapping("/{appointmentId}/start")
    public ResponseEntity<ApiResponse<DoctorExaminationResponse>> startExamination(
            Authentication authentication,
            @PathVariable String appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Examination started successfully.",
                clinicalMedicationService.startExamination(authentication.getName(), appointmentId)));
    }

    @PatchMapping("/{appointmentId}/clinical-information")
    public ResponseEntity<ApiResponse<DoctorExaminationResponse>> updateClinicalInformation(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody UpdateClinicalInformationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Clinical information updated successfully.",
                clinicalMedicationService.updateClinicalInformation(authentication.getName(), appointmentId, request)));
    }

    @PatchMapping("/{appointmentId}/diagnosis")
    public ResponseEntity<ApiResponse<DoctorExaminationResponse>> updateDiagnosis(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody UpdateDiagnosisRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Diagnosis updated successfully.",
                clinicalMedicationService.updateDiagnosis(authentication.getName(), appointmentId, request)));
    }

    @PostMapping("/{appointmentId}/prescriptions")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> createPrescription(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody UpsertPrescriptionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(
                "Prescription created successfully.",
                clinicalMedicationService.createPrescription(authentication.getName(), appointmentId, request)));
    }

    @PatchMapping("/{appointmentId}/prescriptions/{prescriptionId}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> updatePrescription(
            Authentication authentication,
            @PathVariable String appointmentId,
            @PathVariable String prescriptionId,
            @Valid @RequestBody UpsertPrescriptionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Prescription updated successfully.",
                clinicalMedicationService.updatePrescription(
                        authentication.getName(), appointmentId, prescriptionId, request)));
    }

    @PatchMapping("/{appointmentId}/complete")
    public ResponseEntity<ApiResponse<DoctorExaminationResponse>> completeExamination(
            Authentication authentication,
            @PathVariable String appointmentId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Examination completed successfully.",
                clinicalMedicationService.completeExamination(authentication.getName(), appointmentId)));
    }
}
