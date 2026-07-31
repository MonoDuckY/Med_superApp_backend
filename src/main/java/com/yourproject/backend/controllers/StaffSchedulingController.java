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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.yourproject.backend.dtos.requests.AppointmentDecisionRequest;
import com.yourproject.backend.dtos.requests.CreateClinicRoomRequest;
import com.yourproject.backend.dtos.requests.CancelAppointmentRequest;
import com.yourproject.backend.dtos.requests.ScheduleDecisionRequest;
import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.requests.BlockWorkSlotRequest;
import com.yourproject.backend.dtos.requests.RescheduleAppointmentRequest;
import com.yourproject.backend.dtos.requests.StaffCreateAppointmentRequest;
import com.yourproject.backend.dtos.responses.DoctorWorkSlotResponse;
import com.yourproject.backend.dtos.responses.ApiResponse;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.ClinicRoomResponse;
import com.yourproject.backend.dtos.responses.WorkScheduleSubmissionResponse;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.services.AppointmentService;
import com.yourproject.backend.services.SchedulingCatalogService;
import com.yourproject.backend.services.WorkScheduleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/staff/scheduling")
@PreAuthorize("hasRole('STAFF')")
@RequiredArgsConstructor
public class StaffSchedulingController {
    private final SchedulingCatalogService schedulingCatalogService;
    private final WorkScheduleService workScheduleService;
    private final AppointmentService appointmentService;

    @PostMapping("/clinic-rooms")
    public ResponseEntity<ApiResponse<ClinicRoomResponse>> createClinicRoom(
            Authentication authentication,
            @Valid @RequestBody CreateClinicRoomRequest request) {
        ClinicRoomResponse room = ClinicRoomResponse.from(
                schedulingCatalogService.createClinicRoom(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Clinic room created successfully.", room));
    }

    @GetMapping("/clinic-rooms")
    public ResponseEntity<ApiResponse<List<ClinicRoomResponse>>> getClinicRooms() {
        List<ClinicRoomResponse> rooms = schedulingCatalogService.getActiveClinicRooms().stream()
                .map(ClinicRoomResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success("Clinic rooms retrieved successfully.", rooms));
    }

    @GetMapping("/work-schedules/pending")
    public ResponseEntity<ApiResponse<List<WorkScheduleSubmissionResponse>>> getPendingWorkSchedules(
            Authentication authentication) {
        List<DoctorWorkSlot> slots = workScheduleService.getPendingSchedules(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success(
                "Pending work schedules retrieved successfully.",
                workScheduleService.toResponses(slots)));
    }

    @GetMapping("/work-schedules")
    public ResponseEntity<ApiResponse<List<WorkScheduleSubmissionResponse>>> getWorkSchedules(
            Authentication authentication,
            @RequestParam(required = false) DoctorWorkSlotStatus status) {
        List<WorkScheduleSubmissionResponse> schedules = workScheduleService.toResponses(
                workScheduleService.getSchedules(authentication.getName(), status));
        return ResponseEntity.ok(ApiResponse.success("Work schedules retrieved successfully.", schedules));
    }

    @PatchMapping("/work-schedules/{submissionId}/decision")
    public ResponseEntity<ApiResponse<WorkScheduleSubmissionResponse>> decideWorkSchedule(
            Authentication authentication,
            @PathVariable String submissionId,
            @Valid @RequestBody ScheduleDecisionRequest request) {
        WorkScheduleSubmissionResponse response = workScheduleService.toResponse(
                workScheduleService.decide(authentication.getName(), submissionId, request));
        return ResponseEntity.ok(ApiResponse.success("Work schedule decision recorded successfully.", response));
    }

    @PatchMapping("/work-schedules/{submissionId}")
    public ResponseEntity<ApiResponse<WorkScheduleSubmissionResponse>> modifyApprovedWorkSchedule(
            Authentication authentication,
            @PathVariable String submissionId,
            @Valid @RequestBody SubmitWorkScheduleRequest request) {
        WorkScheduleSubmissionResponse response = workScheduleService.toResponse(
                workScheduleService.modifyApprovedSubmission(
                        authentication.getName(),
                        submissionId,
                        request));
        return ResponseEntity.ok(ApiResponse.success(
                "Approved work schedule updated successfully.",
                response));
    }

    @PatchMapping("/work-slots/{doctorWorkSlotId}/block")
    public ResponseEntity<ApiResponse<DoctorWorkSlotResponse>> blockWorkSlot(
            Authentication authentication,
            @PathVariable String doctorWorkSlotId,
            @Valid @RequestBody BlockWorkSlotRequest request) {
        DoctorWorkSlotResponse response = DoctorWorkSlotResponse.from(
                workScheduleService.blockSlot(authentication.getName(), doctorWorkSlotId, request));
        return ResponseEntity.ok(ApiResponse.success("Doctor work slot blocked successfully.", response));
    }

    @GetMapping("/appointments/pending")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getPendingAppointments(
            Authentication authentication) {
        List<AppointmentResponse> appointments = appointmentService.toResponses(
                appointmentService.getPendingAppointments(authentication.getName()));
        return ResponseEntity.ok(ApiResponse.success("Pending appointments retrieved successfully.", appointments));
    }

    @GetMapping("/appointments")
    public ResponseEntity<ApiResponse<List<AppointmentResponse>>> getAppointments(
            Authentication authentication,
            @RequestParam(required = false) AppointmentStatus status) {
        List<AppointmentResponse> appointments = appointmentService.toResponses(
                appointmentService.getAppointments(authentication.getName(), status));
        return ResponseEntity.ok(ApiResponse.success("Appointments retrieved successfully.", appointments));
    }

    @PatchMapping("/appointments/{appointmentId}/decision")
    public ResponseEntity<ApiResponse<AppointmentResponse>> decideAppointment(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody AppointmentDecisionRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(appointmentService.decide(
                authentication.getName(),
                appointmentId,
                request));
        return ResponseEntity.ok(ApiResponse.success(
                "Appointment decision recorded successfully.",
                appointment));
    }

    @PatchMapping("/appointments/{appointmentId}/cancel")
    public ResponseEntity<ApiResponse<AppointmentResponse>> cancelAppointment(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody CancelAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(
                appointmentService.cancel(authentication.getName(), appointmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Appointment cancelled successfully.", appointment));
    }

    @PatchMapping("/appointments/{appointmentId}/reschedule")
    public ResponseEntity<ApiResponse<AppointmentResponse>> rescheduleAppointment(
            Authentication authentication,
            @PathVariable String appointmentId,
            @Valid @RequestBody RescheduleAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(
                appointmentService.reschedule(authentication.getName(), appointmentId, request));
        return ResponseEntity.ok(ApiResponse.success("Appointment rescheduled successfully.", appointment));
    }

    @PostMapping("/appointments")
    public ResponseEntity<ApiResponse<AppointmentResponse>> createAppointment(
            Authentication authentication,
            @Valid @RequestBody StaffCreateAppointmentRequest request) {
        AppointmentResponse appointment = appointmentService.toResponse(
                appointmentService.createByStaff(authentication.getName(), request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Appointment created successfully.", appointment));
    }
}
