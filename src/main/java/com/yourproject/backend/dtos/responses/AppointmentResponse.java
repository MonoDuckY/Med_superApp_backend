package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentResponse {
    private String id;
    private String patientUserId;
    private String patientId;
    private UserSummaryResponse patient;
    private String doctorId;
    private UserSummaryResponse doctor;
    private String doctorWorkSlotId;
    private LocalDate appointmentDate;
    private String slotId;
    private String slotName;
    private String roomId;
    private String roomCode;
    private Instant startAt;
    private Instant endAt;
    private AppointmentStatus status;
    private String note;
    private Instant requestedAt;
    private String reviewedBy;
    private Instant reviewedAt;
    private String rejectionReason;
    private String cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;

    public static AppointmentResponse from(Appointment appointment) {
        return from(appointment, null, null, null);
    }

    public static AppointmentResponse from(
            Appointment appointment,
            User doctor,
            User patient,
            PatientDataProtectionService patientDataProtectionService) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientUserId(appointment.getPatientUserId())
                .patientId(appointment.getPatientId())
                .patient(UserSummaryResponse.from(patient, patientDataProtectionService))
                .doctorId(appointment.getDoctorId())
                .doctor(UserSummaryResponse.from(doctor, patientDataProtectionService))
                .doctorWorkSlotId(appointment.getDoctorWorkSlotId())
                .appointmentDate(appointment.getAppointmentDate())
                .slotId(appointment.getSlotId())
                .slotName(appointment.getSlotName())
                .roomId(appointment.getRoomId())
                .roomCode(appointment.getRoomCode())
                .startAt(appointment.getStartAt())
                .endAt(appointment.getEndAt())
                .status(appointment.getStatus())
                .note(appointment.getNote())
                .requestedAt(appointment.getRequestedAt())
                .reviewedBy(appointment.getReviewedBy())
                .reviewedAt(appointment.getReviewedAt())
                .rejectionReason(appointment.getRejectionReason())
                .cancelledBy(appointment.getCancelledBy())
                .cancelledAt(appointment.getCancelledAt())
                .cancellationReason(appointment.getCancellationReason())
                .build();
    }

}
