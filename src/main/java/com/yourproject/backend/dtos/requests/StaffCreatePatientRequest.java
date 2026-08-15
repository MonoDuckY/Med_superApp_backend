package com.yourproject.backend.dtos.requests;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class StaffCreatePatientRequest {
    @NotBlank(message = "Full name is required.")
    @Size(max = 100, message = "Full name must not exceed 100 characters.")
    private String fullName;

    @NotBlank(message = "Gender is required.")
    @Size(max = 20, message = "Gender must not exceed 20 characters.")
    private String gender;

    @NotNull(message = "Date of birth is required.")
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

    private String medicalHistory;
    private String currentSickness;
    private Double height;
    private Double weight;
    private String bloodType;
}
