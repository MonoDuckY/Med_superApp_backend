package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import com.yourproject.backend.models.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "Username is required.")
    @Size(max = 50, message = "Username must not exceed 50 characters.")
    private String username;

    @NotBlank(message = "Password is required.")
    @Size(max = 50, message = "Password must not exceed 50 characters.")
    private String password;

    @NotNull(message = "Role is required.")
    private UserRole role;

    @Size(max = 100, message = "Full name must not exceed 100 characters.")
    private String fullName;

    @Size(max = 50, message = "Patient ID must not exceed 50 characters.")
    private String patientId;

    @Size(max = 20, message = "Gender must not exceed 20 characters.")
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;
}
