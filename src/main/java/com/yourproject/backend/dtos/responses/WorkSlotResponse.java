package com.yourproject.backend.dtos.responses;

import java.time.LocalTime;

import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkSlotResponse {
    private String id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private WorkSession session;

    public static WorkSlotResponse from(WorkSlot slot) {
        return WorkSlotResponse.builder()
                .id(slot.getId())
                .name(slot.getName())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .session(slot.getSession())
                .build();
    }
}
