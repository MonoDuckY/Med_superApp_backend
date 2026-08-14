package com.yourproject.backend.dtos.responses;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SchedulingOptionsResponse {
    private List<WorkSlotResponse> slots;
}
