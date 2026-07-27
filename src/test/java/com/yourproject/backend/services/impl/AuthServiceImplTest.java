package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.LoginRequest;
import com.yourproject.backend.dtos.requests.LogoutRequest;
import com.yourproject.backend.dtos.requests.RefreshTokenRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.RefreshToken;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.RefreshTokenRepository;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserService userService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7L);
    }

    @Test
    void login_issuesAccessAndRefreshTokensForActiveAccountWithCorrectPassword() {
        User user = activeDoctor();
        LoginRequest request = loginRequest();
        when(userService.findByPhoneNumber("0363636363")).thenReturn(user);
        when(userService.getActiveUserById("user-id")).thenReturn(user);
        when(passwordEncoder.matches("Validpass1", "password-hash")).thenReturn(true);
        when(jwtUtils.generateAccessToken(user)).thenReturn("access-token");
        when(jwtUtils.getAccessTokenExpirationSeconds()).thenReturn(900L);

        AuthResponse response = authService.login(request);

        assertEquals("access-token", response.getAccessToken());
        assertNotNull(response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        verify(userService).recordSuccessfulLogin(user);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_rejectsIncorrectPassword() {
        User user = activeDoctor();
        when(userService.findByPhoneNumber("0363636363")).thenReturn(user);
        when(userService.getActiveUserById("user-id")).thenReturn(user);
        when(passwordEncoder.matches("Validpass1", "password-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest()));
        verify(userService, never()).recordSuccessfulLogin(any(User.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void login_rejectsDisabledAccount() {
        User user = activeDoctor();
        when(userService.findByPhoneNumber("0363636363")).thenReturn(user);
        when(userService.getActiveUserById("user-id"))
                .thenThrow(new UnauthorizedException("This account is disabled."));

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest()));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_rejectsPatientAccountBecauseItMustUseOtp() {
        User patient = activeDoctor();
        patient.setRole(UserRole.PATIENT);
        patient.setPasswordHash(null);
        when(userService.findByPhoneNumber("0363636363")).thenReturn(patient);
        when(userService.getActiveUserById("user-id")).thenReturn(patient);

        assertThrows(BadRequestException.class, () -> authService.login(loginRequest()));
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_rejectsRequestWithoutPhoneNumber() {
        LoginRequest request = loginRequest();
        request.setPhoneNumber(null);

        assertThrows(BadRequestException.class, () -> authService.login(request));
        verify(userService, never()).findByPhoneNumber(any());
    }

    @Test
    void refresh_revokesOldTokenAndIssuesNewTokenPair() {
        User user = activeDoctor();
        RefreshToken storedToken = activeRefreshToken("refresh-token", "user-id");
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hashToken("refresh-token"))).thenReturn(Optional.of(storedToken));
        when(userService.getActiveUserById("user-id")).thenReturn(user);
        when(jwtUtils.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtils.getAccessTokenExpirationSeconds()).thenReturn(900L);

        assertEquals("new-access-token", authService.refresh(request).getAccessToken());
        assertNotNull(storedToken.getRevokedAt());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void refresh_rejectsRevokedToken() {
        RefreshToken storedToken = activeRefreshToken("refresh-token", "user-id");
        storedToken.setRevokedAt(Instant.now());
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hashToken("refresh-token"))).thenReturn(Optional.of(storedToken));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request));
        verify(userService, never()).getActiveUserById(any());
    }

    @Test
    void logout_rejectsTokenOwnedByAnotherUser() {
        RefreshToken storedToken = activeRefreshToken("refresh-token", "other-user");
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");
        when(refreshTokenRepository.findByTokenHash(hashToken("refresh-token"))).thenReturn(Optional.of(storedToken));

        assertThrows(UnauthorizedException.class, () -> authService.logout("user-id", request));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void changePassword_revokesEveryActiveRefreshToken() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Oldpass1");
        request.setNewPassword("Newpass1");
        request.setConfirmPassword("Newpass1");
        RefreshToken firstToken = RefreshToken.builder().userId("user-id").build();
        RefreshToken secondToken = RefreshToken.builder().userId("user-id").build();
        when(refreshTokenRepository.findAllByUserIdAndRevokedAtIsNull("user-id"))
                .thenReturn(List.of(firstToken, secondToken));

        authService.changePassword("user-id", request);

        verify(userService).changePassword("user-id", request);
        assertNotNull(firstToken.getRevokedAt());
        assertNotNull(secondToken.getRevokedAt());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0363636363");
        request.setPassword("Validpass1");
        request.setDeviceId("device-id");
        return request;
    }

    private User activeDoctor() {
        return User.builder()
                .id("user-id")
                .phoneNumber("+84363636363")
                .passwordHash("password-hash")
                .fullName("Dr Nguyen")
                .role(UserRole.DOCTOR)
                .status(AccountStatus.ACTIVE)
                .createdAt(Instant.now())
                .build();
    }

    private RefreshToken activeRefreshToken(String token, String userId) {
        return RefreshToken.builder().tokenHash(hashToken(token)).userId(userId).deviceId("device-id")
                .createdAt(Instant.now()).expiresAt(Instant.now().plus(Duration.ofDays(1))).build();
    }

    private String hashToken(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
