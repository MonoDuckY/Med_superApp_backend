package com.yourproject.backend.dtos.requests;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
@Data public class GatewayDeviceRegistrationRequest { @NotBlank private String fcmToken; private String deviceName; }
