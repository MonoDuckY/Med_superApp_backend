package com.yourproject.backend.controllers;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.yourproject.backend.dtos.requests.BookAppointmentRequest;
import com.yourproject.backend.dtos.requests.CancelAppointmentRequest;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.AvailableAppointmentSlotResponse;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.services.AppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/patient/appointments")
@PreAuthorize("hasRole('PATIENT')")
@RequiredArgsConstructor
public class PatientAppointmentController {
    private final AppointmentService appointmentService;

    @GetMapping("/available-slots")
    public ResponseEntity<ApiResponse<List<AvailableAppointmentSlotResponse>>> getAvailableSlots(
            Authentication authentication,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String doctorId) {
        List<DoctorWorkSlot> slots = appointmentService.getAvailableSlots(authentication.getName(), date, doctorId);
        Map<String, String> doctors = appointmentService.getDoctorNames(slots);
        List<AvailableAppointmentSlotResponse> response = slots.stream()
                .map(slot -> AvailableAppointmentSlotResponse.from(slot, doctors.get(slot.getDoctorId())))
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Available appointment slots retrieved successfully.", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AppointmentResponse>> book(
            Authentication authentication,
            @Valid @RequestBody BookAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(
                appointmentService.book(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Appointment submitted for staff confirmation.",
                        appointment));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(Authentication authentication) {
        List<AppointmentResponse> appointments = appointmentService.toResponses(
                appointmentService.getPatientAppointments(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.success("Patient appointments retrieved successfully.", appointments));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(
                appointmentService.cancelByPatient(authentication.getName(), appointmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully.", appointment));
    }
}
