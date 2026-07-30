package com.yourproject.backend.dtos.requests;

import com.yourproject.backend.models.ScheduleDecision;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AppointmentDecisionRequest {
    @NotNull(message = "Decision is required.")
    private ScheduleDecision decision;

    @Size(max = 500, message = "Rejection reason must not exceed 500 characters.")
    private String rejectionReason;
}
