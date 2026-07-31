package com.yourproject.backend.dtos.responses;

import java.time.Instant;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AppointmentResponse {
    private String id;
    private String patientId;
    private UserSummaryResponse patient;
    private UserSummaryResponse doctor;
    private String doctorWorkSlotId;
    private DoctorWorkSlotResponse doctorWorkSlot;
    private WorkSlotResponse slot;
    private ClinicRoomResponse room;
    private AppointmentStatus status;
    private String diagnosis;
    private Instant requestedAt;
    private String cancelledBy;
    private Instant cancelledAt;
    private String cancellationReason;
    private String previousDoctorWorkSlotId;
    private String rescheduledBy;
    private Instant rescheduledAt;
    private String rescheduleReason;

    public static AppointmentResponse from(Appointment appointment) {
        return from(appointment, null, null, null);
    }

    public static AppointmentResponse from(
            Appointment appointment,
            User doctor,
            User patient,
            PatientDataProtectionService patientDataProtectionService) {
        return from(appointment, doctor, patient, null, null, null, patientDataProtectionService);
    }

    public static AppointmentResponse from(
            Appointment appointment, User doctor, User patient, DoctorWorkSlot doctorWorkSlot,
            WorkSlot slot, ClinicRoom room, PatientDataProtectionService patientDataProtectionService) {
        return AppointmentResponse.builder()
                .id(appointment.getId())
                .patientId(appointment.getPatientId())
                .patient(UserSummaryResponse.from(patient, patientDataProtectionService))
                .doctor(UserSummaryResponse.from(doctor, patientDataProtectionService))
                .doctorWorkSlotId(appointment.getDoctorWorkSlotId())
                .doctorWorkSlot(doctorWorkSlot == null ? null : DoctorWorkSlotResponse.from(doctorWorkSlot))
                .slot(slot == null ? null : WorkSlotResponse.from(slot))
                .room(room == null ? null : ClinicRoomResponse.from(room))
                .status(appointment.getStatus())
                .diagnosis(appointment.getDiagnosis())
                .requestedAt(appointment.getRequestedAt())
                .cancelledBy(appointment.getCancelledBy())
                .cancelledAt(appointment.getCancelledAt())
                .cancellationReason(appointment.getCancellationReason())
                .previousDoctorWorkSlotId(appointment.getPreviousDoctorWorkSlotId())
                .rescheduledBy(appointment.getRescheduledBy())
                .rescheduledAt(appointment.getRescheduledAt())
                .rescheduleReason(appointment.getRescheduleReason())
                .build();
    }

}
