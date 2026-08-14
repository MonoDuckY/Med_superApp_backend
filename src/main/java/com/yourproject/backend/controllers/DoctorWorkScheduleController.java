package com.yourproject.backend.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;

import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.SchedulingOptionsResponse;
import com.yourproject.backend.dtos.responses.WorkScheduleSubmissionResponse;
import com.yourproject.backend.dtos.responses.WorkSlotResponse;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.services.SchedulingCatalogService;
import com.yourproject.backend.services.WorkScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/doctor/work-schedules")
@PreAuthorize("hasRole('DOCTOR')")
@RequiredArgsConstructor
public class DoctorWorkScheduleController {
    private final SchedulingCatalogService schedulingCatalogService;
    private final WorkScheduleService workScheduleService;

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<SchedulingOptionsResponse>> getOptions() {
        SchedulingOptionsResponse response = SchedulingOptionsResponse.builder()
                .slots(schedulingCatalogService.getActiveWorkSlots().stream().map(WorkSlotResponse::from).toList())
                .build();
        return ResponseEntity.ok(ApiResponse.success("Scheduling options retrieved successfully.", response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkScheduleSubmissionResponse>>> getSchedules(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<DoctorWorkSlot> slots = workScheduleService.getDoctorSchedules(authentication.getName(), from, to);
        return ResponseEntity.ok(ApiResponse.success(
                "Doctor work schedules retrieved successfully.",
                workScheduleService.toResponses(slots)));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<WorkScheduleSubmissionResponse>>> getAllDoctorSchedules(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) DoctorWorkSlotStatus status) {
        List<DoctorWorkSlot> slots = workScheduleService.getAllDoctorSchedules(
                authentication.getName(), from, to, status);
        return ResponseEntity.ok(ApiResponse.success(
                "All doctor work schedules retrieved successfully.",
                workScheduleService.toResponses(slots)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkScheduleSubmissionResponse>> submit(
            Authentication authentication,
            @Valid @RequestBody SubmitWorkScheduleRequest request) {
        WorkScheduleSubmissionResponse response = workScheduleService.toResponse(
                workScheduleService.submit(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Work schedule submitted for staff approval.", response));
    }

    @DeleteMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<Void>> cancel(
            Authentication authentication,
            @PathVariable String submissionId) {
        workScheduleService.cancelPendingSubmission(authentication.getName(), submissionId);
        return ResponseEntity.ok(ApiResponse.success("Pending work schedule cancelled successfully.", null));
    }

    @PatchMapping("/{submissionId}")
    public ResponseEntity<ApiResponse<WorkScheduleSubmissionResponse>> modifyPending(
            Authentication authentication,
            @PathVariable String submissionId,
            @Valid @RequestBody SubmitWorkScheduleRequest request) {
        WorkScheduleSubmissionResponse response = workScheduleService.toResponse(
                workScheduleService.modifyPendingSubmission(
                        authentication.getName(),
                        submissionId,
                        request));
        return ResponseEntity.ok(ApiResponse.success(
                "Pending work schedule updated successfully.",
                response));
    }
}
