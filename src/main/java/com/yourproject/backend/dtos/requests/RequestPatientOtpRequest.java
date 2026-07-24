package com.yourproject.backend.dtos.requests;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class RequestPatientOtpRequest { @NotBlank private String phoneNumber; }
