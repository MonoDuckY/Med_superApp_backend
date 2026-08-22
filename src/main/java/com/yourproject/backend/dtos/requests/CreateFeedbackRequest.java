package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateFeedbackRequest {
    @NotNull(message = "Rating is required.")
    @Min(value = 1, message = "Rating must be from 1 to 5.")
    @Max(value = 5, message = "Rating must be from 1 to 5.")
    private Integer rating;

    @NotBlank(message = "Service is required.")
    private String serviceType;

    private String content;
}
