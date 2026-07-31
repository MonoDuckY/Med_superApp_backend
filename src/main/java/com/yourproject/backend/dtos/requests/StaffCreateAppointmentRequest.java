package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StaffCreateAppointmentRequest {
    @NotBlank(message = "Patient ID is required.")
    private String patientId;

    @NotBlank(message = "Doctor work slot ID is required.")
    private String doctorWorkSlotId;
}
