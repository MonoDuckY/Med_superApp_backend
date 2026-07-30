package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClinicRoomRequest {
    @NotBlank(message = "Clinic room ID is required.")
    @Size(max = 50, message = "Clinic room ID must not exceed 50 characters.")
    private String id;

    @NotBlank(message = "Clinic room name is required.")
    @Size(max = 100, message = "Clinic room name must not exceed 100 characters.")
    private String name;

    @Size(max = 500, message = "Note must not exceed 500 characters.")
    private String note;
}
