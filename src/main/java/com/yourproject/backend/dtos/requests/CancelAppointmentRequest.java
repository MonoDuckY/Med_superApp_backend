package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelAppointmentRequest {
    @NotBlank(message = "Cancellation reason is required.")
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters.")
    private String cancellationReason;
}
