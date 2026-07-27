package com.yourproject.backend.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FcmSmsCommandRequest {

    @NotBlank(message = "Gateway FCM token is required.")
    private String gatewayFcmToken;

    @NotBlank(message = "Phone number is required.")
    private String phoneNumber;

    @NotBlank(message = "SMS content is required.")
    private String content;
}
