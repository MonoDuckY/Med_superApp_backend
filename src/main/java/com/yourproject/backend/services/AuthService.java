package com.yourproject.backend.services;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.LoginRequest;
import com.yourproject.backend.dtos.requests.LogoutRequest;
import com.yourproject.backend.dtos.requests.RefreshTokenRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;

public interface AuthService {
    AuthResponse login(LoginRequest request);

    AuthResponse refresh(RefreshTokenRequest request);

    void logout(String userId, LogoutRequest request);

    void changePassword(String userId, ChangePasswordRequest request);
}
