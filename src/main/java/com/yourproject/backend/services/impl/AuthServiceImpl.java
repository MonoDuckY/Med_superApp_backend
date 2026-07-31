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
import com.yourproject.backend.dtos.requests.RequestPatientOtpRequest;
import com.yourproject.backend.dtos.requests.VerifyPatientOtpRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.dtos.responses.UserResponse;
import com.yourproject.backend.exceptions.UnauthorizedException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.PatientOtpRepository;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.services.SmsGatewayService;
import com.yourproject.backend.services.AuthService;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.JwtUtils;
import com.yourproject.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final PatientDataProtectionService patientDataProtectionService;
    private final PatientOtpRepository patientOtpRepository;
    private final SmsGatewayService smsGatewayService;
    private final UserRepository userRepository;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;
    @Value("${app.otp.expiration-minutes}") private long otpExpirationMinutes;
    @Value("${app.otp.resend-cooldown-seconds}") private long otpResendCooldownSeconds;
    @Value("${app.otp.max-attempts}") private int otpMaxAttempts;
    @Value("${app.auth.max-failed-login-attempts}") private int maxFailedLoginAttempts;
    @Value("${app.auth.lockout-minutes}") private long lockoutMinutes;

    @Override
    public AuthResponse requestPatientOtp(RequestPatientOtpRequest request) {
        User user=userService.findByPhoneNumber(request.getPhoneNumber()); user=userService.getActiveUserById(user.getId());
        if(!user.getRoles().contains(UserRole.PATIENT)) throw new BadRequestException("Only patient accounts can use SMS OTP.");
        if (isTrustedDevice(user, request.getDeviceId())) { userService.recordSuccessfulLogin(user); return issueTokens(user, request.getDeviceId()); }
        String phoneLookup=patientDataProtectionService.phoneLookup(com.yourproject.backend.utils.PhoneNumberNormalizer.normalize(request.getPhoneNumber()));
        patientOtpRepository.findTopByPhoneLookupOrderByCreatedAtDesc(phoneLookup).ifPresent(previous->{if(previous.getCreatedAt().plusSeconds(otpResendCooldownSeconds).isAfter(Instant.now())) throw new BadRequestException("Please wait before requesting another OTP.");});
        patientOtpRepository.findAllByPhoneLookupAndConsumedAtIsNull(phoneLookup).forEach(previous -> { previous.setConsumedAt(Instant.now()); patientOtpRepository.save(previous); });
        String code=String.format("%06d",SECURE_RANDOM.nextInt(1_000_000)); Instant expires=Instant.now().plus(Duration.ofMinutes(otpExpirationMinutes));
        System.out.println("\n=======================================================");
        System.out.println("MÃ OTP CỦA BẠN LÀ: " + code);
        System.out.println("=======================================================\n");
        PatientOtp otp = patientOtpRepository.save(PatientOtp.builder().userId(user.getId()).phoneLookup(phoneLookup).codeHash(patientDataProtectionService.secureLookup("otp:"+user.getId()+":"+code)).attempts(0).createdAt(Instant.now()).expiresAt(expires).build());
        try {
            smsGatewayService.enqueue(user.getId(),com.yourproject.backend.utils.PhoneNumberNormalizer.normalize(request.getPhoneNumber()),"[Hospital Management System] Ma OTP testing cua ban la "+code+". Khong chia se ma nay.",expires);
        } catch (RuntimeException exception) {
            patientOtpRepository.deleteById(otp.getId());
            throw new BadRequestException("OTP could not be delivered. Please try again.");
        }
        return null;
    }

    @Override
    public AuthResponse verifyPatientOtp(VerifyPatientOtpRequest request) {
        User user=userService.findByPhoneNumber(request.getPhoneNumber()); user=userService.getActiveUserById(user.getId());
        if(!user.getRoles().contains(UserRole.PATIENT)) throw new BadRequestException("Only patient accounts can use SMS OTP.");
        String lookup=patientDataProtectionService.phoneLookup(com.yourproject.backend.utils.PhoneNumberNormalizer.normalize(request.getPhoneNumber()));
        PatientOtp otp=patientOtpRepository.findTopByPhoneLookupOrderByCreatedAtDesc(lookup).orElseThrow(()->new UnauthorizedException("OTP is invalid or expired."));
        if(otp.getConsumedAt()!=null||otp.getExpiresAt().isBefore(Instant.now())||otp.getAttempts()>=otpMaxAttempts) throw new UnauthorizedException("OTP is invalid or expired.");
        if(!patientDataProtectionService.secureLookup("otp:"+user.getId()+":"+request.getCode()).equals(otp.getCodeHash())) { otp.setAttempts(otp.getAttempts()+1);patientOtpRepository.save(otp);throw new UnauthorizedException("OTP is invalid or expired."); }
        otp.setConsumedAt(Instant.now());patientOtpRepository.save(otp);trustDevice(user,request.getDeviceId());userService.recordSuccessfulLogin(user);return issueTokens(user,request.getDeviceId());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        validateLoginRequest(request);
        User user = userService.findByPhoneNumber(request.getPhoneNumber());
        user = userService.getActiveUserById(user.getId());
        if (user.getRoles().size() == 1 && user.getRoles().contains(UserRole.PATIENT)) {
            throw new BadRequestException("Patient accounts must sign in using SMS OTP.");
        }
        if (user.getLockedUntil()!=null && user.getLockedUntil().isAfter(Instant.now())) throw new UnauthorizedException("Account is temporarily locked. Please try again later.");
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            user.setFailedLoginAttempts(user.getFailedLoginAttempts()+1);
            if(user.getFailedLoginAttempts()>=maxFailedLoginAttempts) user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(lockoutMinutes)));
            userRepository.save(user);
            throw new UnauthorizedException("Invalid phone number or password.");
        }
        user.setFailedLoginAttempts(0); user.setLockedUntil(null); userRepository.save(user);
        userService.recordSuccessfulLogin(user);
        return issueTokens(user, request.getDeviceId());
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {
        User user = userRepository.findByRefreshTokenHash(hashToken(request.getRefreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid."));
        if (user.getRefreshTokenExpiresAt() == null || !user.getRefreshTokenExpiresAt().isAfter(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked.");
        }
        user = userService.getActiveUserById(user.getId());
        return issueTokens(user, user.getDeviceId());
    }

    @Override
    public void logout(String userId, LogoutRequest request) {
        User user = userService.getActiveUserById(userId);
        if (!hashToken(request.getRefreshToken()).equals(user.getRefreshTokenHash())) {
            throw new UnauthorizedException("Refresh token does not belong to the current user.");
        }
        user.setAccessTokenHash(null);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
    }

    @Override
    public void changePassword(String userId, ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        User user = userService.getUserById(userId);
        user.setAccessTokenHash(null);
        user.setRefreshTokenHash(null);
        user.setRefreshTokenExpiresAt(null);
        userRepository.save(user);
    }

    private AuthResponse issueTokens(User user, String deviceId) {
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = createRefreshToken();
        user.setAccessTokenHash(hashToken(accessToken));
        user.setRefreshTokenHash(hashToken(refreshToken));
        user.setRefreshTokenExpiresAt(Instant.now().plus(Duration.ofDays(refreshTokenExpirationDays)));
        user.setDeviceId(deviceId == null || deviceId.isBlank() ? null : deviceId.trim());
        userRepository.save(user);
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresInSeconds(jwtUtils.getAccessTokenExpirationSeconds())
                .user(UserResponse.from(user, patientDataProtectionService))
                .build();
    }

    private String createRefreshToken() {
        byte[] randomBytes = new byte[48];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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

    private void validateLoginRequest(LoginRequest request) {
        if (request == null || request.getPhoneNumber() == null || request.getPhoneNumber().isBlank()) {
            throw new BadRequestException("Phone number is required.");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BadRequestException("Password is required.");
        }
    }
    private boolean isTrustedDevice(User user,String deviceId){return deviceId!=null&&!deviceId.isBlank()&&deviceId.trim().equals(user.getDeviceId());}
    private void trustDevice(User user,String deviceId){if(deviceId==null||deviceId.isBlank())return;user.setDeviceId(deviceId.trim());userRepository.save(user);}
}
