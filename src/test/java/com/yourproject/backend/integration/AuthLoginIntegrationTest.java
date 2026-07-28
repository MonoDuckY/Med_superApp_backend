package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;

class AuthLoginIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        assertEquals(1, refreshTokenRepository.count());
        assertNotNull(userRepository.findAll().get(0).getLastLoginAt());
    }

    @Test
    void loginWithoutPhoneNumberReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"Password123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Phone number is required."))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void loginWithoutPasswordReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password is required."))
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void incorrectPasswordIncrementsFailedLoginAttempts() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"WrongPassword1!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid phone number or password."));

        assertEquals(1, userRepository.findById(doctor.getId()).orElseThrow().getFailedLoginAttempts());
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    void fifthIncorrectPasswordLocksAccountAndBlocksCorrectPassword() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phoneNumber\":\"0912345678\",\"password\":\"WrongPassword1!\"}"))
                    .andExpect(status().isUnauthorized());
        }

        User lockedDoctor = userRepository.findById(doctor.getId()).orElseThrow();
        assertEquals(5, lockedDoctor.getFailedLoginAttempts());
        assertNotNull(lockedDoctor.getLockedUntil());
        assertTrue(lockedDoctor.getLockedUntil().isAfter(java.time.Instant.now()));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Account is temporarily locked. Please try again later."));
    }

    @Test
    void inactiveStaffAccountCannotLogin() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        doctor.setStatus(AccountStatus.INACTIVE);
        userRepository.save(doctor);

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void patientAccountMustUseOtpInsteadOfPassword() throws Exception {
        saveActivePatient("+84912345678");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Patient accounts must sign in using SMS OTP."));
    }
}
