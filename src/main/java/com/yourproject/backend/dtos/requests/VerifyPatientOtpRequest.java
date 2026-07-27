package com.yourproject.backend.dtos.requests;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
@Data public class VerifyPatientOtpRequest { @NotBlank private String phoneNumber; @NotBlank @Pattern(regexp="\\d{6}") private String code; private String deviceId; }
