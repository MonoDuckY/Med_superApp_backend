package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DishRequest {
    @NotBlank(message = "Dish name is required.")
    @Size(max = 200, message = "Dish name must not exceed 200 characters.")
    private String dishName;
    @Positive(message = "Dish quantity must be greater than zero.")
    @NotNull(message = "Dish quantity is required.")
    private Double quantity;
    @NotBlank(message = "Dish unit is required.")
    @Size(max = 50, message = "Dish unit must not exceed 50 characters.")
    private String unit;
    @PositiveOrZero(message = "Total calories must not be negative.")
    private Double totalCalories;
    @PositiveOrZero(message = "Total protein must not be negative.")
    private Double totalProtein;
    @PositiveOrZero(message = "Total carbohydrates must not be negative.")
    private Double totalCarbohydrates;
    @PositiveOrZero(message = "Total fat must not be negative.")
    private Double totalFat;
}
