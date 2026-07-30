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
    private String slotName;
    private String roomId;
    private String roomCode;
    private Instant startAt;
    private Instant endAt;

    public static AvailableAppointmentSlotResponse from(DoctorWorkSlot slot, String doctorName) {
        return AvailableAppointmentSlotResponse.builder()
                .doctorWorkSlotId(slot.getId())
                .doctorId(slot.getDoctorId())
                .doctorName(doctorName)
                .workDate(slot.getWorkDate())
                .slotId(slot.getSlotId())
                .slotName(slot.getSlotName())
                .roomId(slot.getRoomId())
                .roomCode(slot.getRoomCode())
                .startAt(slot.getStartAt())
                .endAt(slot.getEndAt())
                .build();
    }
}
