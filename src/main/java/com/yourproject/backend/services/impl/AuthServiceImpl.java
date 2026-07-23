package com.yourproject.backend.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.LoginRequest;
import com.yourproject.backend.dtos.requests.LogoutRequest;
import com.yourproject.backend.dtos.requests.RefreshTokenRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.models.RefreshToken;
import com.yourproject.backend.models.User;
import com.yourproject.backend.repositories.RefreshTokenRepository;
import com.yourproject.backend.services.AuthService;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.JwtUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PatientDataProtectionService patientDataProtectionService;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userService.findByPhoneNumber(request.getPhoneNumber());
        user = userService.getActiveUserById(user.getId());
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid phone number or password.");
        }

        userService.recordSuccessfulLogin(user);
        return issueTokens(user, request.getDeviceId());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));

        if (storedToken.getRevokedAt() != null || storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked.");
        }

        User user = userService.getActiveUserById(storedToken.getUserId());
        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);
        return issueTokens(user, storedToken.getDeviceId());
    }

    @Override
    public void logout(String userId, LogoutRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));
        if (!storedToken.getUserId().equals(userId)) {
            throw new UnauthorizedException("Refresh token does not belong to the current user.");
        }

        storedToken.setRevokedAt(Instant.now());
        refreshTokenRepository.save(storedToken);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull(userId).forEach(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    private AuthResponse issueTokens(User user, String deviceId) {
        return AuthResponse.builder()
                .accessToken(jwtUtils.generateAccessToken(user))
                .refreshToken(createRefreshToken(user.getId(), deviceId))
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getAccessTokenExpirationSeconds())
                .user(UserResponse.from(user, patientDataProtectionService))
                .build();
    }

    private String createRefreshToken(String userId, String deviceId) {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        Instant now = Instant.now();

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenHash(hashToken(token))
                .userId(userId)
                .deviceId(deviceId == null || deviceId.isBlank() ? null : deviceId.trim())
                .createdAt(now)
                .expiresAt(now.plus(Duration.ofDays(refreshTokenExpirationDays)))
                .build();
        refreshTokenRepository.save(refreshToken);
        return token;
    }

    private String hashToken(String token) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }
}
