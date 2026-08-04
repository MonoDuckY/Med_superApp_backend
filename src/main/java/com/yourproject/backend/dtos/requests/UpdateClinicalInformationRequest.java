package com.yourproject.backend.dtos.requests;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateClinicalInformationRequest {
    @Size(max = 5000, message = "Medical history must not exceed 5000 characters.")
    private String medicalHistory;

    @Size(max = 2000, message = "Current sickness must not exceed 2000 characters.")
    private String currentSickness;

    @Positive(message = "Height must be positive.")
    private Double height;

    @Positive(message = "Weight must be positive.")
    private Double weight;

    @Size(max = 10, message = "Blood type must not exceed 10 characters.")
    private String bloodType;

    @Valid
    private List<VitalSignRequest> vitalSigns;
}
