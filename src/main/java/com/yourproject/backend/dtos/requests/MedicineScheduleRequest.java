package com.yourproject.backend.dtos.requests;

import java.time.Instant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MedicineScheduleRequest {
    @NotBlank(message = "Medicine name is required.")
    @Size(max = 200, message = "Medicine name must not exceed 200 characters.")
    private String medicineName;

    @NotBlank(message = "Dosage is required.")
    @Size(max = 100, message = "Dosage must not exceed 100 characters.")
    private String dosage;

    @NotNull(message = "Scheduled time is required.")
    private Instant scheduledAt;

    @Size(max = 500, message = "Medicine note must not exceed 500 characters.")
    private String note;
}
