package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyPasswordResetOtpRequest {
    @NotBlank(message = "Phone number is required.")
    private String phoneNumber;

    @NotBlank(message = "OTP is required.")
    @Pattern(regexp = "\\d{6}", message = "OTP must contain exactly 6 digits.")
    private String code;
}
