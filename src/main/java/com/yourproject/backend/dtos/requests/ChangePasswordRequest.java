package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Current password is required.")
    private String currentPassword;

    @NotBlank(message = "New password is required.")
    @Size(max = 50, message = "New password must not exceed 50 characters.")
    private String newPassword;

    @NotBlank(message = "Password confirmation is required.")
    private String confirmPassword;
}
