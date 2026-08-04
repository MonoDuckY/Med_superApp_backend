package com.yourproject.backend.dtos.requests;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMedicineScheduleTimeRequest {
    @NotNull(message = "Scheduled time is required.")
    private Instant scheduledAt;
}
