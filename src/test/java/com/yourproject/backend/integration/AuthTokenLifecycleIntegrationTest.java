package com.yourproject.backend.integration;

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
import com.yourproject.backend.models.RefreshToken;

class AuthTokenLifecycleIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void refreshRotatesTokenAndRejectsOldToken() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        TokenPair original = login("0912345678", "Password123!");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + original.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andReturn();

        String newAccessToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.accessToken");
        String newRefreshToken = JsonPath.read(refreshResult.getResponse().getContentAsString(), "$.data.refreshToken");
        assertNotNull(newAccessToken);
        assertNotEquals(original.refreshToken(), newRefreshToken);
        assertNotNull(refreshTokenRepository.findByTokenHash(hashToken(original.refreshToken())).orElseThrow().getRevokedAt());
        assertNull(refreshTokenRepository.findByTokenHash(hashToken(newRefreshToken)).orElseThrow().getRevokedAt());

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + original.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is expired or revoked."));
    }

    @Test
    void expiredRefreshTokenIsRejected() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!");
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(hashToken(tokens.refreshToken())).orElseThrow();
        storedToken.setExpiresAt(Instant.now().minusSeconds(1));
        refreshTokenRepository.save(storedToken);

        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token is expired or revoked."));
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        TokenPair tokens = login("0912345678", "Password123!");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logout successful."));

        assertNotNull(refreshTokenRepository.findByTokenHash(hashToken(tokens.refreshToken())).orElseThrow().getRevokedAt());
        mockMvc.perform(post("/api/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRejectsRefreshTokenOwnedByAnotherUser() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        saveActiveDoctor("+84987654321", "Password123!");
        TokenPair firstUser = login("0912345678", "Password123!");
        TokenPair secondUser = login("0987654321", "Password123!");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + firstUser.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + secondUser.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token does not belong to the current user."));

        assertNull(refreshTokenRepository.findByTokenHash(hashToken(secondUser.refreshToken())).orElseThrow().getRevokedAt());
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

    private String hashToken(String token) throws Exception {
        byte[] hashed = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(hashed);
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }
}
