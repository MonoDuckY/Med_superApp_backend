package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateDiagnosisRequest {
    @NotBlank(message = "Diagnosis is required.")
    @Size(max = 5000, message = "Diagnosis must not exceed 5000 characters.")
    private String diagnosis;
}
