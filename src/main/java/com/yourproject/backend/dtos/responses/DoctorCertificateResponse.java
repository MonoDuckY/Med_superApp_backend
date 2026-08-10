package com.yourproject.backend.dtos.responses;

import java.time.Instant;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoctorCertificateResponse {
    String doctorId;
    String certificateUrl;
    Instant expiresAt;
}
