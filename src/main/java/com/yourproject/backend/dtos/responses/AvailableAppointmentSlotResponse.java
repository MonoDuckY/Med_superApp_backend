package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.DoctorWorkSlot;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableAppointmentSlotResponse {
    private String doctorWorkSlotId;
    private String doctorId;
    private String doctorName;
    private LocalDate workDate;
    private String slotId;
    private String roomId;
    private Instant startAt;
    private Instant endAt;

    public static AvailableAppointmentSlotResponse from(
            DoctorWorkSlot slot,
            String doctorName,
            Instant startAt,
            Instant endAt) {
        return AvailableAppointmentSlotResponse.builder()
                .doctorWorkSlotId(slot.getId())
                .doctorId(slot.getDoctorId())
                .doctorName(doctorName)
                .workDate(slot.getWorkDate())
                .slotId(slot.getSlotId())
                .roomId(slot.getRoomId())
                .startAt(startAt)
                .endAt(endAt)
                .build();
    }
}
