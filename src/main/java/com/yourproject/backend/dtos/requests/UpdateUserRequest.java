package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.UserRole;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {
    @Size(max = 50, message = "Username must not exceed 50 characters.")
    private String username;

    private UserRole role;
    private AccountStatus status;

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
