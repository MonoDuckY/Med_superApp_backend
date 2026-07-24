package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import com.yourproject.backend.models.UserRole;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @Size(max = 50, message = "Password must not exceed 50 characters.")
    private String password;

    @NotNull(message = "Role is required.")
    private UserRole role;

    @Size(max = 100, message = "Full name must not exceed 100 characters.")
    private String fullName;

    @Size(max = 20, message = "Gender must not exceed 20 characters.")
    private String gender;

    private LocalDate dateOfBirth;

    @NotBlank(message = "Phone number is required.")
    @Size(max = 20, message = "Phone number must not exceed 20 characters.")
    private String phoneNumber;

    @Size(max = 255, message = "Address must not exceed 255 characters.")
    private String address;

    @Size(max = 50, message = "Citizen identification code must not exceed 50 characters.")
    private String citizenIdentificationCode;

    @Size(max = 50, message = "Health insurance code must not exceed 50 characters.")
    private String healthInsuranceCode;

    @Size(max = 255, message = "Certificate must not exceed 255 characters.")
    private String certificate;
}
