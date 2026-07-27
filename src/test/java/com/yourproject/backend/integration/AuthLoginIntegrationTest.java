package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class AuthLoginIntegrationTest extends MongoIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginWithValidCredentialsReturnsTokens() throws Exception {
        saveActiveDoctor();

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
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

    private void saveActiveDoctor() {
        String normalizedPhone = "+84912345678";
        userRepository.save(User.builder().fullName("Dr Integration").role(UserRole.DOCTOR).status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone).phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode("Password123!")).certificate("Practice certificate").build());
    }
}
