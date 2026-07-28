package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;

class AuthJwtSecurityIntegrationTest extends MongoIntegrationTestBase {
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

    private String loginAndGetAccessToken(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
