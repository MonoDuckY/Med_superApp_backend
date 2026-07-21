package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "Username is required.")
    @Size(max = 50, message = "Username must not exceed 50 characters.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(max = 50, message = "Password must not exceed 50 characters.")
    private String password;

    @Size(max = 255, message = "Device ID must not exceed 255 characters.")
    private String deviceId;
}
