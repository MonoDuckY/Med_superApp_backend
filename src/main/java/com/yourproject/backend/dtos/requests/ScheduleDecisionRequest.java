package com.yourproject.backend.dtos.requests;

import com.yourproject.backend.models.ScheduleDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleDecisionRequest {
    @NotNull(message = "Decision is required.")
    private ScheduleDecision decision;

    @Size(max = 100, message = "Clinic room ID must not exceed 100 characters.")
    private String roomId;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters.")
    private String rejectionReason;
}
