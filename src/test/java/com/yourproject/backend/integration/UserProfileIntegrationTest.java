package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class UserProfileIntegrationTest extends MongoIntegrationTestBase {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void authenticatedUserRetrievesOwnProfile() throws Exception {
        String normalizedPhone = "+84912345678";
        userRepository.save(User.builder().fullName("Dr Profile").roleId(UserRole.DOCTOR.getId()).status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone).phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode("Password123!"))
                .certificateObjectKey("doctor-certificates/doctor-profile/certificate.jpg").build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"role\":\"DOCTOR\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk()).andReturn();
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fullName").value("Dr Profile"))
                .andExpect(jsonPath("$.data.phoneNumber").value(normalizedPhone));
    }

    @Test
    void patientProfileIsEncryptedAtRestAndDecryptedForAuthorizedResponse() throws Exception {
        User patient = saveActivePatient("+84912345678");
        User storedPatient = userRepository.findById(patient.getId()).orElseThrow();
        assertNull(storedPatient.getPhoneNumber());
        assertNull(storedPatient.getPatientId());
        assertNull(storedPatient.getFullName());
        assertNotNull(storedPatient.getPatientPhoneEncrypted());
        assertNotEquals("+84912345678", storedPatient.getPatientPhoneEncrypted());

        String code = "123456";
        patientOtpRepository.save(PatientOtp.builder()
                .userId(patient.getId())
                .phoneLookup(patient.getPhoneLookup())
                .codeHash(patientDataProtectionService.secureLookup("otp:" + patient.getId() + ":" + code))
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"patient-device\"}"))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(loginResult.getResponse().getContentAsString(), "$.data.accessToken");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.patientId").value("PAT-INTEGRATION"))
                .andExpect(jsonPath("$.data.fullName").value("Patient Integration"))
                .andExpect(jsonPath("$.data.gender").value("NONE"))
                .andExpect(jsonPath("$.data.dateOfBirth").value("1995-01-01"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84912345678"))
                .andExpect(jsonPath("$.data.address").value("Test address"));
    }
}
