package com.yourproject.backend.dtos.responses;

public record PasswordResetTokenResponse(String resetToken, long expiresInSeconds) {
}
