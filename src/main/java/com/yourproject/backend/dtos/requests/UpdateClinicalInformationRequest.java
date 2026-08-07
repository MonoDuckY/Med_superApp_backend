package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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

    @Size(max = 1000, message = "Medical record note must not exceed 1000 characters.")
    private String note;

    @Size(max = 30, message = "Blood pressure must not exceed 30 characters.")
    private String bloodPressure;

    @Min(value = 1, message = "Heart rate must be positive.")
    private Integer heartRate;

    @Min(value = 1, message = "Breathing rate must be positive.")
    private Integer breathingRate;

    @DecimalMin(value = "0.1", message = "Body temperature must be positive.")
    private Double bodyTemperature;

    @DecimalMin(value = "0.0", inclusive = false, message = "Blood lipids must be positive.")
    private Double bloodLipids;
}
