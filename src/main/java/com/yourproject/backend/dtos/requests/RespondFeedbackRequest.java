package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RespondFeedbackRequest {
    @NotBlank(message = "Response is required.")
    private String response;
}
