package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void incorrectCurrentPasswordIsRejectedWithoutMutation() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"WrongPassword1!\",\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"NewPassword2!\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Current password is incorrect."));

        assertTrue(passwordEncoder.matches("OldPassword1!", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
        assertNull(refreshTokenRepository.findAll().get(0).getRevokedAt());
    }

    @Test
    void mismatchedPasswordConfirmationIsRejectedWithoutMutation() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"DifferentPassword3!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and confirmation do not match."));

        assertTrue(passwordEncoder.matches("OldPassword1!", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
        assertNull(refreshTokenRepository.findAll().get(0).getRevokedAt());
    }

    @Test
    void passwordEqualToCurrentPasswordIsRejected() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"OldPassword1!\",\"confirmPassword\":\"OldPassword1!\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must be different from the current password."));

        assertTrue(passwordEncoder.matches("OldPassword1!", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void everyWeakPasswordPolicyPartitionIsRejected() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");
        String[] weakPasswords = {"Short1!", "UPPERCASE1!", "lowercase1!", "NoDigitOrSpecial"};

        for (String weakPassword : weakPasswords) {
            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"" + weakPassword + "\",\"confirmPassword\":\"" + weakPassword + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Password must contain 8-50 characters, including lowercase, uppercase, and a number or special character."));
        }

        assertTrue(passwordEncoder.matches("OldPassword1!", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
        assertNull(refreshTokenRepository.findAll().get(0).getRevokedAt());
    }

    @Test
    void missingPasswordChangeFieldsReturnValidationErrors() throws Exception {
        saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");
        String[] bodies = {
                "{\"newPassword\":\"NewPassword2!\",\"confirmPassword\":\"NewPassword2!\"}",
                "{\"currentPassword\":\"OldPassword1!\",\"confirmPassword\":\"NewPassword2!\"}",
                "{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"NewPassword2!\"}"
        };

        for (String body : bodies) {
            mockMvc.perform(post("/api/auth/change-password")
                            .header("Authorization", "Bearer " + tokens.accessToken())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        }
    }

    @Test
    void overlongNewPasswordReturnsValidationError() throws Exception {
        saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");
        String overlongPassword = "Aa1!" + "x".repeat(47);

        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"" + overlongPassword + "\",\"confirmPassword\":\"" + overlongPassword + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password must not exceed 50 characters."));
    }

    @Test
    void passwordWithDigitButNoSpecialCharacterIsAccepted() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");

        changePasswordSuccessfully(tokens.accessToken(), "NewPassword2");

        assertTrue(passwordEncoder.matches("NewPassword2", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
    }

    @Test
    void passwordWithSpecialCharacterButNoDigitIsAccepted() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "OldPassword1!");
        TokenPair tokens = login("0912345678", "OldPassword1!");

        changePasswordSuccessfully(tokens.accessToken(), "NewPassword!");

        assertTrue(passwordEncoder.matches("NewPassword!", userRepository.findById(doctor.getId()).orElseThrow().getPasswordHash()));
    }

    private void changePasswordSuccessfully(String accessToken, String newPassword) throws Exception {
        mockMvc.perform(post("/api/auth/change-password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"OldPassword1!\",\"newPassword\":\"" + newPassword + "\",\"confirmPassword\":\"" + newPassword + "\"}"))
                .andExpect(status().isOk());
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
