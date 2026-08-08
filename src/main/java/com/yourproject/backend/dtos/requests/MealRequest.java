package com.yourproject.backend.dtos.requests;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MealRequest {
    @NotBlank(message = "Meal name is required.")
    @Size(max = 200, message = "Meal name must not exceed 200 characters.")
    private String mealName;
    @NotNull(message = "Scheduled time is required.")
    private Instant scheduledAt;
    @Size(max = 500, message = "Meal note must not exceed 500 characters.")
    private String note;
    @NotEmpty(message = "At least one dish is required.")
    @jakarta.validation.Valid
    private List<DishRequest> dishes;
}
