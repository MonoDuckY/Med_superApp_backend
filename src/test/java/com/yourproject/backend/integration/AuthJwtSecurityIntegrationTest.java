package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class AuthJwtSecurityIntegrationTest extends MongoIntegrationTestBase {
    private static final String JWT_SECRET = "integration-test-secret-with-at-least-thirty-two-characters";
    @Test
    void protectedEndpointRejectsMissingBearerTokenWithApiResponseWrapper() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void protectedEndpointRejectsMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-valid-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void tokenCannotAuthenticateUserAfterAccountBecomesInactive() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        String accessToken = loginAndGetAccessToken("0912345678", "Password123!");
        doctor.setStatus(AccountStatus.INACTIVE);
        userRepository.save(doctor);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void doctorCannotAccessAdminOnlyUserApi() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        String accessToken = loginAndGetAccessToken("0912345678", "Password123!");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        Instant now = Instant.now();
        String token = signedToken(doctor.getId(), now.minusSeconds(120), now.minusSeconds(60), JWT_SECRET, true);

        assertUnauthorized(token);
    }

    @Test
    void accessTokenSignedWithDifferentSecretIsRejected() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        Instant now = Instant.now();
        String token = signedToken(
                doctor.getId(),
                now,
                now.plusSeconds(300),
                "different-integration-secret-with-at-least-thirty-two-characters",
                true);

        assertUnauthorized(token);
    }

    @Test
    void tokenForUnknownUserIsRejected() throws Exception {
        Instant now = Instant.now();
        String token = signedToken("missing-user-id", now, now.plusSeconds(300), JWT_SECRET, true);

        assertUnauthorized(token);
    }

    @Test
    void signedTokenWithoutIssuedAtIsRejectedInsteadOfCausingServerError() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        Instant now = Instant.now();
        String token = signedToken(doctor.getId(), now, now.plusSeconds(300), JWT_SECRET, false);

        assertUnauthorized(token);
    }

    @Test
    void nonBearerAuthorizationSchemeIsRejected() throws Exception {
        saveActiveDoctor("+84912345678", "Password123!");
        String accessToken = loginAndGetAccessToken("0912345678", "Password123!");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Token " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void adminCanAccessAdminOnlyUserApi() throws Exception {
        saveActiveAdmin("+84912345678", "Password123!");
        String accessToken = loginAndGetAccessToken("0912345678", "Password123!");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void tokenCannotAuthenticateDeletedUser() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        String accessToken = loginAndGetAccessToken("0912345678", "Password123!");
        userRepository.deleteById(doctor.getId());

        assertUnauthorized(accessToken);
    }

    private void assertUnauthorized(String token) throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    private String signedToken(
            String userId,
            Instant issuedAt,
            Instant expiresAt,
            String secret,
            boolean includeIssuedAt) {
        var builder = Jwts.builder()
                .subject(userId)
                .claim("role", "DOCTOR")
                .expiration(Date.from(expiresAt));
        if (includeIssuedAt) {
            builder.issuedAt(Date.from(issuedAt));
        }
        return builder
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    private String loginAndGetAccessToken(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
