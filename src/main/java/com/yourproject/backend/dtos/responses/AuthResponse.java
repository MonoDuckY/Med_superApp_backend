package com.yourproject.backend.dtos.responses;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AuthResponse {
    String accessToken;
    String refreshToken;
    String tokenType;
    long expiresInSeconds;
    UserResponse user;
}
