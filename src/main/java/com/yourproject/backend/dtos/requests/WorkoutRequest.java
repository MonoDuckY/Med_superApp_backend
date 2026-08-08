package com.yourproject.backend.dtos.requests;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class WorkoutRequest {
    @NotBlank(message = "Workout name is required.")
    @Size(max = 200, message = "Workout name must not exceed 200 characters.")
    private String workoutName;
    @Size(max = 2000, message = "Workout content must not exceed 2000 characters.")
    private String content;
    @NotNull(message = "Scheduled time is required.")
    private Instant scheduledAt;
    @Size(max = 500, message = "Workout note must not exceed 500 characters.")
    private String note;
}
