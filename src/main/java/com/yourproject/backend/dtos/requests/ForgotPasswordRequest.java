package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import com.yourproject.backend.models.UserRole;
import jakarta.validation.constraints.NotNull;

@Data
public class ForgotPasswordRequest {
    @NotBlank(message = "Phone number is required.")
    private String phoneNumber;

    @NotNull(message = "Role is required.")
    private UserRole role;
}
