package com.yourproject.backend.dtos.responses;

import java.util.List;

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.Prescription;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PrescriptionResponse {
    String id;
    String appointmentId;
    String content;
    List<MedicineScheduleResponse> medicineSchedules;

    public static PrescriptionResponse from(Prescription prescription, List<MedicineSchedule> schedules) {
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .appointmentId(prescription.getAppointmentId())
                .content(prescription.getContent())
                .medicineSchedules(schedules.stream().map(MedicineScheduleResponse::from).toList())
                .build();
    }
}
