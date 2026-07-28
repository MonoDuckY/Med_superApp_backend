package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.User;

class AuthPasswordChangeIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void staffChangesPasswordAndRevokesEveryRefreshToken() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair firstLogin = login("0912345678", "OldPassword1!");
        login("0912345678", "OldPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + firstLogin.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"NewPassword2!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password changed successfully."));

        User updatedDoctor = userRepository.findById(doctor.getId()).orElseThrow();
        assertTrue(passwordEncoder.matches("NewPassword2!", updatedDoctor.getPasswordHash()));
        assertFalse(passwordEncoder.matches("OldPassword1!", updatedDoctor.getPasswordHash()));
        assertTrue(refreshTokenRepository.findAll().stream().allMatch(token -> token.getRevokedAt() != null));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + firstLogin.accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"OldPassword1!\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"NewPassword2!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void patientCannotUsePasswordChangeEndpoint() throws Exception {
        User patient = saveActivePatient("+84912345678");
        String code = "123456";
        patientOtpRepository.save(PatientOtp.builder()
                .userId(patient.getId())
                .phoneLookup(patient.getPhoneLookup())
                .codeHash(patientDataProtectionService.secureLookup("otp:" + patient.getId() + ":" + code))
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());

        MvcResult otpLogin = mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"patient-device\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(otpLogin.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"Anything1!\",\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"NewPassword2!\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"))
                .andExpect(jsonPath("$.message").value("Patient accounts authenticate using SMS OTP and do not have passwords."));

        assertNull(userRepository.findById(patient.getId()).orElseThrow().getPasswordHash());
    }

    private TokenPair login(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return new TokenPair(
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken"),
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken"));
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
