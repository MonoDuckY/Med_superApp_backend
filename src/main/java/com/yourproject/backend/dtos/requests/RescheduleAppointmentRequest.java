package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RescheduleAppointmentRequest {
    @NotBlank(message = "New doctor work slot ID is required.")
    private String doctorWorkSlotId;

    @NotBlank(message = "Reschedule reason is required.")
    @Size(max = 500, message = "Reschedule reason must not exceed 500 characters.")
    private String reason;
}
