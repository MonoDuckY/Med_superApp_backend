package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;

class AuthTokenLifecycleIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void refreshRotatesTokenAndRejectsOldToken() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair original = login("0912345678", "Password123!", "web-device");

        MvcResult result = refresh(original.refreshToken(), status().isOk());
        String rotatedRefreshToken = JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.refreshToken");

        assertNotEquals(original.refreshToken(), rotatedRefreshToken);
        User updated = userRepository.findById(doctor.getId()).orElseThrow();
        assertEquals(hashToken(rotatedRefreshToken), updated.getRefreshTokenHash());

        refresh(original.refreshToken(), status().isUnauthorized());
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!", null);
        doctor = userRepository.findById(doctor.getId()).orElseThrow();
        doctor.setRefreshTokenExpiresAt(Instant.now().minusSeconds(1));
        userRepository.save(doctor);

        refresh(tokens.refreshToken(), status().isUnauthorized())
                .getResponse();
    }

    @Test
    void logoutClearsTokensStoredInUser() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!", "web-device");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk());

        User loggedOut = userRepository.findById(doctor.getId()).orElseThrow();
        assertNull(loggedOut.getAccessTokenHash());
        assertNull(loggedOut.getRefreshTokenHash());
        assertNull(loggedOut.getRefreshTokenExpiresAt());
        refresh(tokens.refreshToken(), status().isUnauthorized());
    }

    @Test
    void logoutRejectsRefreshTokenOwnedByAnotherUser() throws Exception {
        User first = saveActiveDoctor("+84912345678", "Password123!");
        saveActiveDoctor("+84987654321", "Password123!");
        TokenPair firstTokens = login("0912345678", "Password123!", "first-device");
        TokenPair secondTokens = login("0987654321", "Password123!", "second-device");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + firstTokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondTokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message")
                        .value("Refresh token does not belong to the current user."));

        assertNotNull(userRepository.findById(first.getId()).orElseThrow().getRefreshTokenHash());
    }

    @Test
    void unknownRefreshTokenIsRejected() throws Exception {
        refresh("unknown-refresh-token", status().isUnauthorized())
                .getResponse();
    }

    @Test
    void inactiveUserCannotRefreshTokens() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!", null);
        doctor = userRepository.findById(doctor.getId()).orElseThrow();
        doctor.setStatus(AccountStatus.INACTIVE);
        userRepository.save(doctor);

        refresh(tokens.refreshToken(), status().isUnauthorized())
                .getResponse();
    }

    @Test
    void deletedUserCannotRefreshTokens() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!", null);
        userRepository.deleteById(doctor.getId());

        refresh(tokens.refreshToken(), status().isUnauthorized())
                .getResponse();
    }

    @Test
    void refreshRotationPreservesDeviceId() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!", "web-browser-a");

        refresh(tokens.refreshToken(), status().isOk());

        assertEquals("web-browser-a",
                userRepository.findById(doctor.getId()).orElseThrow().getDeviceId());
    }

    @Test
    void refreshWithoutTokenReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    private MvcResult refresh(
            String refreshToken,
            org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        return mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(expectedStatus)
                .andReturn();
    }

    private TokenPair login(String phoneNumber, String password, String deviceId) throws Exception {
        String deviceJson = deviceId == null ? "" : ",\"deviceId\":\"" + deviceId + "\"";
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber
                                + "\",\"role\":\"DOCTOR\",\"password\":\"" + password + "\"" + deviceJson + "}"))
                .andExpect(status().isOk())
                .andReturn();
        return new TokenPair(
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken"),
                JsonPath.read(result.getResponse().getContentAsString(), "$.data.refreshToken"));
    }

    private String hashToken(String token) throws Exception {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
