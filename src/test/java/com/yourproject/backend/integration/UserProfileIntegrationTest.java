package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class UserProfileIntegrationTest extends MongoIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedUserRetrievesOwnProfile() throws Exception {
        String normalizedPhone = "+84912345678";
        userRepository.save(User.builder().fullName("Dr Profile").role(UserRole.DOCTOR).status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone).phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode("Password123!")).certificate("Practice certificate").build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andReturn();
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Dr Profile"))
                .andExpect(jsonPath("$.data.phoneNumber").value(normalizedPhone));
    }
}
