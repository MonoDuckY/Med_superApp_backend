package com.yourproject.backend.dtos.responses;

import java.time.Instant;

import com.yourproject.backend.models.MedicineSchedule;
import com.yourproject.backend.models.MedicineScheduleStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MedicineScheduleResponse {
    String id;
    String medicineName;
    String dosage;
    Instant scheduledAt;
    MedicineScheduleStatus status;
    String prescriptionId;
    String note;

    public static MedicineScheduleResponse from(MedicineSchedule schedule) {
        return MedicineScheduleResponse.builder()
                .id(schedule.getId())
                .medicineName(schedule.getMedicineName())
                .dosage(schedule.getDosage())
                .scheduledAt(schedule.getScheduledAt())
                .status(schedule.getStatus())
                .prescriptionId(schedule.getPrescriptionId())
                .note(schedule.getNote())
                .build();
    }
}
