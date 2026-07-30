package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClinicRoomRequest {
    @NotBlank(message = "Clinic room code is required.")
    @Size(max = 30, message = "Clinic room code must not exceed 30 characters.")
    private String code;

    @NotBlank(message = "Clinic room name is required.")
    @Size(max = 100, message = "Clinic room name must not exceed 100 characters.")
    private String name;
}
