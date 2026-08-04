package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VitalSignRequest {
    @NotBlank(message = "Vital sign name is required.")
    @Size(max = 100, message = "Vital sign name must not exceed 100 characters.")
    private String vitalName;

    @NotBlank(message = "Vital sign value is required.")
    @Size(max = 100, message = "Vital sign value must not exceed 100 characters.")
    private String vitalNumber;

    @Size(max = 30, message = "Vital sign unit must not exceed 30 characters.")
    private String vitalUnit;
}
