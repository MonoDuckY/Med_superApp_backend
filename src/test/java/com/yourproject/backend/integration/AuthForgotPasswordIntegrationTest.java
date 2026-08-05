package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import com.yourproject.backend.models.User;

class AuthForgotPasswordIntegrationTest extends MongoIntegrationTestBase {
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Test
    void forgotPasswordOtpResetsPasswordAndRevokesExistingTokens() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair oldTokens = login("0912345678", "OldPassword1!");

        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("If the account is eligible, a password reset OTP has been sent."));

        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmGatewayService).sendSmsCommand(
                eq("integration-test-fcm-token"), eq("+84912345678"), contentCaptor.capture());
        Matcher matcher = OTP_PATTERN.matcher(contentCaptor.getValue());
        if (!matcher.find()) {
            throw new AssertionError("No six-digit reset OTP was found in the fake FCM payload.");
        }

        MvcResult verifyResult = mockMvc.perform(post("/api/auth/forgot-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"" + matcher.group(1) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andReturn();
        String resetToken = JsonPath.read(verifyResult.getResponse().getContentAsString(), "$.data.resetToken");

        mockMvc.perform(post("/api/auth/forgot-password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"resetToken\":\"" + resetToken
                                + "\",\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"NewPassword2!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully. Please sign in again."));

        User updated = userRepository.findById(doctor.getId()).orElseThrow();
        assertFalse(passwordEncoder.matches("OldPassword1!", updated.getPasswordHash()));
        assertNotNull(updated.getPasswordChangedAt());
        assertNull(updated.getAccessTokenHash());
        assertNull(updated.getRefreshTokenHash());

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + oldTokens.accessToken()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldTokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"NewPassword2!\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void unknownPhoneReturnsGenericSuccessWithoutCreatingOtp() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        org.junit.jupiter.api.Assertions.assertEquals(0, patientOtpRepository.count());
    }

    @Test
    void incorrectResetOtpIsRejectedAndCannotChangePassword() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        mockMvc.perform(post("/api/auth/forgot-password/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/forgot-password/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"000000\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Password reset OTP is invalid or expired."));

        User unchanged = userRepository.findById(doctor.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("OldPassword1!", unchanged.getPasswordHash()));
    }

    private TokenPair login(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return new TokenPair(
                JsonPath.read(body, "$.data.accessToken"),
                JsonPath.read(body, "$.data.refreshToken"));
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
