package com.yourproject.backend.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.yourproject.backend.dtos.requests.ChangePasswordRequest;
import com.yourproject.backend.dtos.requests.LoginRequest;
import com.yourproject.backend.dtos.requests.LogoutRequest;
import com.yourproject.backend.dtos.requests.RefreshTokenRequest;
import com.yourproject.backend.dtos.responses.AuthResponse;
import com.yourproject.backend.exceptions.GlobalExceptionHandler;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.config.SecurityConfig;
import com.yourproject.backend.security.JwtAuthenticationFilter;
import com.yourproject.backend.security.RestAccessDeniedHandler;
import com.yourproject.backend.security.RestAuthenticationEntryPoint;
import com.yourproject.backend.services.AuthService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class, properties = "app.cors.allowed-origins=http://localhost:3000")
@Import({ GlobalExceptionHandler.class, SecurityConfig.class })
class AuthControllerApiTest {

    private static final String USER_ID = "user-123";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private PatientDataProtectionService patientDataProtectionService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void passRequestsThroughJwtFilter() throws Exception {
        doAnswer(invocation -> {
            FilterChain filterChain = invocation.getArgument(2);
            filterChain.doFilter(
                    invocation.getArgument(0, ServletRequest.class),
                    invocation.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtAuthenticationFilter).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
    }

    @Test
    void loginReturnsTokensForValidRequest() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setPhoneNumber("0912345678");
        request.setPassword("Password123!");
        request.setDeviceId("mobile-device-01");

        AuthResponse response = AuthResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .expiresInSeconds(900)
                .build();
        org.mockito.BDDMockito.given(authService.login(any(LoginRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void loginRejectsMissingPassword() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password is required."))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        verifyNoInteractions(authService);
    }

    @Test
    void refreshReturnsNewTokensForValidRefreshToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");
        AuthResponse response = AuthResponse.builder()
                .accessToken("new-access-token")
                .refreshToken("new-refresh-token")
                .tokenType("Bearer")
                .expiresInSeconds(900)
                .build();
        org.mockito.BDDMockito.given(authService.refresh(any(RefreshTokenRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully."))
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"));

        verify(authService).refresh(any(RefreshTokenRequest.class));
    }

    @Test
    void logoutRevokesRefreshTokenForAuthenticatedUser() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh-token");

        mockMvc.perform(post("/api/auth/logout")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful."));

        verify(authService).logout(eq(USER_ID), any(LogoutRequest.class));
    }

    @Test
    void changePasswordUsesAuthenticatedUserId() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("Password123!");
        request.setNewPassword("NewPassword123!");
        request.setConfirmPassword("NewPassword123!");

        mockMvc.perform(post("/api/auth/change-password")
                        .with(authentication(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        verify(authService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    void getCurrentUserReturnsAuthenticatedUserProfile() throws Exception {
        User user = User.builder()
                .id(USER_ID)
                .role(UserRole.DOCTOR)
                .status(AccountStatus.ACTIVE)
                .fullName("Dr Nguyen Van A")
                .phoneNumber("+84912345678")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        org.mockito.BDDMockito.given(userService.getActiveUserById(USER_ID)).willReturn(user);

        mockMvc.perform(get("/api/auth/me").with(authentication(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Current user retrieved successfully."))
                .andExpect(jsonPath("$.data.id").value(USER_ID))
                .andExpect(jsonPath("$.data.role").value("DOCTOR"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84912345678"));

        verify(userService).getActiveUserById(USER_ID);
        verify(patientDataProtectionService).decryptPatientFields(any(User.class));
    }

    private UsernamePasswordAuthenticationToken authenticatedUser() {
        return new UsernamePasswordAuthenticationToken(
                USER_ID,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_DOCTOR")));
    }
}
