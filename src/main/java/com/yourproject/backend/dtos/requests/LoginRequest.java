package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Phone number is required.")
    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;

    @NotBlank(message = "Password is required.")
    @Size(max = 50, message = "Password must not exceed 50 characters.")
    private String password;

    @Size(max = 255, message = "Device ID must not exceed 255 characters.")
    private String deviceId;
}
