package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BookAppointmentRequest {
    @NotBlank(message = "Doctor work slot ID is required.")
    private String doctorWorkSlotId;

    @Size(max = 500, message = "Note must not exceed 500 characters.")
    private String note;
}
