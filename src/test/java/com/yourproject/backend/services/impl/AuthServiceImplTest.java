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
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.PatientOtpRepository;
import com.yourproject.backend.models.OtpPurpose;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.dtos.requests.ForgotPasswordRequest;
import com.yourproject.backend.dtos.requests.ResetPasswordRequest;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.SmsGatewayService;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.JwtUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @Mock
    private PatientOtpRepository patientOtpRepository;

    @Mock
    private SmsGatewayService smsGatewayService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7L);
        ReflectionTestUtils.setField(authService, "otpExpirationMinutes", 5L);
        ReflectionTestUtils.setField(authService, "otpResendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(authService, "otpMaxAttempts", 5);
        ReflectionTestUtils.setField(authService, "maxFailedLoginAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutMinutes", 15L);
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
        verify(userRepository, times(2)).save(user);
    }

    @Test
    void login_rejectsIncorrectPassword() {
        User user = activeDoctor();
        when(userService.findByPhoneNumber("0363636363")).thenReturn(user);
        when(userService.getActiveUserById("user-id")).thenReturn(user);
        when(passwordEncoder.matches("Validpass1", "password-hash")).thenReturn(false);

        assertThrows(UnauthorizedException.class, () -> authService.login(loginRequest()));
        verify(userService, never()).recordSuccessfulLogin(any(User.class));
        verify(userRepository).save(user);
    }

    @Test
    void login_rejectsInactiveAccount() {
        User user = activeDoctor();
        when(userService.findByPhoneNumber("0363636363")).thenReturn(user);
        when(userService.getActiveUserById("user-id"))
                .thenThrow(new UnauthorizedException("This account is inactive."));

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
        user.setRefreshTokenHash(hashToken("refresh-token"));
        user.setRefreshTokenExpiresAt(Instant.now().plus(Duration.ofDays(1)));
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(userRepository.findByRefreshTokenHash(hashToken("refresh-token"))).thenReturn(Optional.of(user));
        when(userService.getActiveUserById("user-id")).thenReturn(user);
        when(jwtUtils.generateAccessToken(user)).thenReturn("new-access-token");
        when(jwtUtils.getAccessTokenExpirationSeconds()).thenReturn(900L);

        assertEquals("new-access-token", authService.refresh(request).getAccessToken());
        verify(userRepository, times(1)).findByRefreshTokenHash(hashToken("refresh-token"));
    }

    @Test
    void refresh_rejectsRevokedToken() {
        User user = activeDoctor();
        user.setRefreshTokenExpiresAt(Instant.now().minusSeconds(1));
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("refresh-token");
        when(userRepository.findByRefreshTokenHash(hashToken("refresh-token"))).thenReturn(Optional.of(user));

        assertThrows(UnauthorizedException.class, () -> authService.refresh(request));
        verify(userService, never()).getActiveUserById(any());
    }

    @Test
    void logout_rejectsTokenOwnedByAnotherUser() {
        User user = activeDoctor();
        user.setRefreshTokenHash(hashToken("another-token"));
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");
        when(userService.getActiveUserById("user-id")).thenReturn(user);

        assertThrows(UnauthorizedException.class, () -> authService.logout("user-id", request));
        verify(userRepository, never()).save(user);
    }

    @Test
    void logout_revokesRefreshTokenOwnedByCurrentUser() {
        User user = activeDoctor();
        user.setRefreshTokenHash(hashToken("refresh-token"));
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");
        when(userService.getActiveUserById("user-id")).thenReturn(user);

        authService.logout("user-id", request);

        assertEquals(null, user.getRefreshTokenHash());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_revokesEveryActiveRefreshToken() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Oldpass1");
        request.setNewPassword("Newpass1");
        request.setConfirmPassword("Newpass1");
        User user = activeDoctor();
        user.setAccessTokenHash("access-hash");
        user.setRefreshTokenHash("refresh-hash");
        when(userService.getUserById("user-id")).thenReturn(user);

        authService.changePassword("user-id", request);

        verify(userService).changePassword("user-id", request);
        assertEquals(null, user.getAccessTokenHash());
        assertEquals(null, user.getRefreshTokenHash());
        verify(userRepository).save(user);
    }

    @Test
    void requestPasswordReset_createsOtpForEligibleAccount() {
        User user = activeDoctor();
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("0363636363");
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(user));
        when(patientOtpRepository.findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(patientOtpRepository.findAllByPhoneLookupAndPurposeAndConsumedAtIsNull("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(List.of());
        when(patientDataProtectionService.secureLookup(any())).thenReturn("otp-hash");
        when(patientOtpRepository.save(any(PatientOtp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(request);

        verify(patientOtpRepository).save(any(PatientOtp.class));
        verify(smsGatewayService).enqueue(eq("user-id"), eq("+84363636363"), any(), any());
    }

    @Test
    void requestPasswordReset_doesNotRevealUnknownAccount() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("0363636363");
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.empty());

        authService.requestPasswordReset(request);

        verify(patientOtpRepository, never()).save(any());
        verify(smsGatewayService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void requestPasswordReset_allowsNonPatientAccountWithoutExistingPasswordHash() {
        User user = activeDoctor();
        user.setPasswordHash(null);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("0363636363");
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(user));
        when(patientOtpRepository.findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.empty());
        when(patientOtpRepository.findAllByPhoneLookupAndPurposeAndConsumedAtIsNull("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(List.of());
        when(patientDataProtectionService.secureLookup(any())).thenReturn("otp-hash");
        when(patientOtpRepository.save(any(PatientOtp.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.requestPasswordReset(request);

        verify(smsGatewayService).enqueue(eq("user-id"), eq("+84363636363"), any(), any());
    }

    @Test
    void requestPasswordReset_ignoresPatientOnlyAccountWithoutPassword() {
        User patient = activeDoctor();
        patient.setRole(UserRole.PATIENT);
        patient.setPasswordHash(null);
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setPhoneNumber("0363636363");
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(patient));

        authService.requestPasswordReset(request);

        verify(patientOtpRepository, never()).save(any());
        verify(smsGatewayService, never()).enqueue(any(), any(), any(), any());
    }

    @Test
    void resetPassword_changesPasswordAndRevokesCurrentTokens() {
        User user = activeDoctor();
        user.setAccessTokenHash("access-hash");
        user.setRefreshTokenHash("refresh-hash");
        user.setRefreshTokenExpiresAt(Instant.now().plusSeconds(300));
        PatientOtp otp = PatientOtp.builder()
                .userId("user-id")
                .phoneLookup("phone-lookup")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .codeHash("otp-hash")
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        ResetPasswordRequest request = resetPasswordRequest();
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(user));
        when(patientOtpRepository.findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otp));
        when(patientDataProtectionService.secureLookup("password-reset:user-id:123456")).thenReturn("otp-hash");
        when(passwordEncoder.matches("NewPassword2!", "password-hash")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword2!")).thenReturn("new-password-hash");
        when(patientOtpRepository.findAllByPhoneLookupAndPurposeAndConsumedAtIsNull("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(List.of(otp));

        authService.resetPassword(request);

        assertEquals("new-password-hash", user.getPasswordHash());
        assertEquals(null, user.getAccessTokenHash());
        assertEquals(null, user.getRefreshTokenHash());
        assertNotNull(otp.getConsumedAt());
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_rejectsIncorrectOtpAndIncrementsAttempts() {
        User user = activeDoctor();
        PatientOtp otp = PatientOtp.builder()
                .userId("user-id")
                .phoneLookup("phone-lookup")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .codeHash("correct-hash")
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(user));
        when(patientOtpRepository.findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otp));
        when(patientDataProtectionService.secureLookup("password-reset:user-id:123456")).thenReturn("wrong-hash");

        assertThrows(UnauthorizedException.class, () -> authService.resetPassword(resetPasswordRequest()));

        assertEquals(1, otp.getAttempts());
        verify(patientOtpRepository).save(otp);
        verify(userRepository, never()).save(user);
    }

    @Test
    void resetPassword_setsFirstPasswordWhenExistingHashIsNull() {
        User user = activeDoctor();
        user.setPasswordHash(null);
        PatientOtp otp = PatientOtp.builder()
                .userId("user-id")
                .phoneLookup("phone-lookup")
                .purpose(OtpPurpose.PASSWORD_RESET)
                .codeHash("otp-hash")
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build();
        when(patientDataProtectionService.phoneLookup("+84363636363")).thenReturn("phone-lookup");
        when(userRepository.findByPhoneLookup("phone-lookup")).thenReturn(Optional.of(user));
        when(patientOtpRepository.findTopByPhoneLookupAndPurposeOrderByCreatedAtDesc("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(Optional.of(otp));
        when(patientDataProtectionService.secureLookup("password-reset:user-id:123456")).thenReturn("otp-hash");
        when(passwordEncoder.encode("NewPassword2!")).thenReturn("new-password-hash");
        when(patientOtpRepository.findAllByPhoneLookupAndPurposeAndConsumedAtIsNull("phone-lookup", OtpPurpose.PASSWORD_RESET))
                .thenReturn(List.of(otp));

        authService.resetPassword(resetPasswordRequest());

        assertEquals("new-password-hash", user.getPasswordHash());
        verify(passwordEncoder, never()).matches(eq("NewPassword2!"), any());
        verify(userRepository).save(user);
    }

    private ResetPasswordRequest resetPasswordRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setPhoneNumber("0363636363");
        request.setCode("123456");
        request.setNewPassword("NewPassword2!");
        request.setConfirmPassword("NewPassword2!");
        return request;
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

    private String hashToken(String token) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
