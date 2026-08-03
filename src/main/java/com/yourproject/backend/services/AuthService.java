package com.yourproject.backend.services;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.LoginRequest;
import com.yourproject.backend.dtos.requests.LogoutRequest;
import com.yourproject.backend.dtos.requests.RefreshTokenRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;
import com.yourproject.backend.dtos.requests.RequestPatientOtpRequest;
import com.yourproject.backend.dtos.requests.VerifyPatientOtpRequest;
import com.yourproject.backend.dtos.requests.ForgotPasswordRequest;
import com.yourproject.backend.dtos.requests.ResetPasswordRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String userId, LogoutRequest request);

    void changePassword(String userId, ChangePasswordRequest request);
    AuthResponse requestPatientOtp(RequestPatientOtpRequest request);
    AuthResponse verifyPatientOtp(VerifyPatientOtpRequest request);

    void requestPasswordReset(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
