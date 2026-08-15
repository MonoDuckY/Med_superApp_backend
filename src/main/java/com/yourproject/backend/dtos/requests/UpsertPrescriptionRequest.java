package com.yourproject.backend.dtos.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpsertPrescriptionRequest {
    @Size(max = 5000, message = "Prescription content must not exceed 5000 characters.")
    private String content;

    @NotEmpty(message = "At least one medicine schedule is required.")
    @Valid
    private List<MedicineScheduleRequest> medicineSchedules;

    @Valid
    private List<MealRequest> meals;

    @Valid
    private List<WorkoutRequest> workouts;
}
