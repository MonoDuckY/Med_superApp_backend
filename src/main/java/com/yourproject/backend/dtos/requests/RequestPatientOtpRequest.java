package com.yourproject.backend.dtos.requests;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class RequestPatientOtpRequest { @NotBlank private String phoneNumber; @jakarta.validation.constraints.Size(max=255) private String deviceId; }
