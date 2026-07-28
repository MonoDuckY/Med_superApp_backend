package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.User;

class PatientOtpIntegrationTest extends MongoIntegrationTestBase {
    private static final String PHONE = "+84912345678";
    private static final String DOMESTIC_PHONE = "0912345678";
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{6})\\b");

    @Test
    void requestingOtpStoresOnlyHashAndSendsThroughFakeFcm() throws Exception {
        User patient = saveActivePatient(PHONE);

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"new-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());

        String code = captureSentOtp(1);
        PatientOtp storedOtp = patientOtpRepository.findAll().get(0);
        assertEquals(patient.getId(), storedOtp.getUserId());
        assertEquals(patientDataProtectionService.secureLookup("otp:" + patient.getId() + ":" + code), storedOtp.getCodeHash());
        assertNotEquals(code, storedOtp.getCodeHash());
        assertNull(storedOtp.getConsumedAt());
        assertEquals(0, storedOtp.getAttempts());
    }

    @Test
    void validOtpReturnsTokensConsumesOtpAndTrustsDevice() throws Exception {
        User patient = saveActivePatient(PHONE);
        requestOtp("device-a");
        String code = captureSentOtp(1);

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"" + code + "\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.role").value("PATIENT"));

        PatientOtp storedOtp = patientOtpRepository.findAll().get(0);
        assertNotNull(storedOtp.getConsumedAt());
        assertEquals(1, trustedDeviceRepository.count());
        assertEquals("device-a", trustedDeviceRepository.findAll().get(0).getDeviceId());
        assertEquals(1, refreshTokenRepository.count());
        assertNotNull(userRepository.findById(patient.getId()).orElseThrow().getLastLoginAt());
    }

    @Test
    void trustedDeviceSkipsOtpAndReturnsTokensImmediately() throws Exception {
        saveActivePatient(PHONE);
        requestOtp("trusted-device");
        String code = captureSentOtp(1);
        verifyOtp(code, "trusted-device");
        clearInvocations(fcmGatewayService);

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"trusted-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Trusted device authenticated successfully."))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        verifyNoInteractions(fcmGatewayService);
        assertEquals(1, patientOtpRepository.count());
        assertEquals(2, refreshTokenRepository.count());
    }

    @Test
    void requestingOtpDuringCooldownIsRejected() throws Exception {
        saveActivePatient(PHONE);
        requestOtp("device-a");

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Please wait before requesting another OTP."));

        verify(fcmGatewayService, times(1)).sendSmsCommand(eq("integration-test-fcm-token"), eq(PHONE), org.mockito.ArgumentMatchers.anyString());
        assertEquals(1, patientOtpRepository.count());
    }

    @Test
    void requestingNewOtpAfterCooldownInvalidatesPreviousOtp() throws Exception {
        User patient = saveActivePatient(PHONE);
        PatientOtp previousOtp = saveOtp(patient, "123456", 0, Instant.now().minusSeconds(61), Instant.now().plusSeconds(120));

        requestOtp("device-a");

        PatientOtp invalidatedOtp = patientOtpRepository.findById(previousOtp.getId()).orElseThrow();
        PatientOtp latestOtp = patientOtpRepository.findTopByPhoneLookupOrderByCreatedAtDesc(patient.getPhoneLookup()).orElseThrow();
        assertNotNull(invalidatedOtp.getConsumedAt());
        assertNotEquals(previousOtp.getId(), latestOtp.getId());
        assertNull(latestOtp.getConsumedAt());
    }

    @Test
    void incorrectOtpIncrementsAttemptsAndReturnsUnauthorized() throws Exception {
        User patient = saveActivePatient(PHONE);
        PatientOtp otp = saveOtp(patient, "123456", 0, Instant.now(), Instant.now().plusSeconds(300));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"654321\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("OTP is invalid or expired."));

        assertEquals(1, patientOtpRepository.findById(otp.getId()).orElseThrow().getAttempts());
        assertEquals(0, trustedDeviceRepository.count());
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    void expiredConsumedAndMaxAttemptOtpsAreRejected() throws Exception {
        User patient = saveActivePatient(PHONE);

        PatientOtp expired = saveOtp(patient, "111111", 0, Instant.now().minusSeconds(400), Instant.now().minusSeconds(1));
        assertOtpRejected("111111");
        patientOtpRepository.delete(expired);

        PatientOtp consumed = saveOtp(patient, "222222", 0, Instant.now(), Instant.now().plusSeconds(300));
        consumed.setConsumedAt(Instant.now());
        patientOtpRepository.save(consumed);
        assertOtpRejected("222222");
        patientOtpRepository.delete(consumed);

        saveOtp(patient, "333333", 5, Instant.now(), Instant.now().plusSeconds(300));
        assertOtpRejected("333333");

        assertEquals(0, refreshTokenRepository.count());
        assertEquals(0, trustedDeviceRepository.count());
    }

    private void requestOtp(String deviceId) throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + DOMESTIC_PHONE + "\",\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isOk());
    }

    private void verifyOtp(String code, String deviceId) throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + DOMESTIC_PHONE + "\",\"code\":\"" + code + "\",\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isOk());
    }

    private void assertOtpRejected(String code) throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + DOMESTIC_PHONE + "\",\"code\":\"" + code + "\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("OTP is invalid or expired."));
    }

    private PatientOtp saveOtp(User patient, String code, int attempts, Instant createdAt, Instant expiresAt) {
        return patientOtpRepository.save(PatientOtp.builder()
                .userId(patient.getId())
                .phoneLookup(patient.getPhoneLookup())
                .codeHash(patientDataProtectionService.secureLookup("otp:" + patient.getId() + ":" + code))
                .attempts(attempts)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build());
    }

    private String captureSentOtp(int expectedInvocations) {
        ArgumentCaptor<String> contentCaptor = ArgumentCaptor.forClass(String.class);
        verify(fcmGatewayService, times(expectedInvocations))
                .sendSmsCommand(eq("integration-test-fcm-token"), eq(PHONE), contentCaptor.capture());
        String content = contentCaptor.getAllValues().get(contentCaptor.getAllValues().size() - 1);
        Matcher matcher = OTP_PATTERN.matcher(content);
        assertFalse(content.isBlank());
        if (!matcher.find()) {
            throw new AssertionError("No six-digit OTP was found in the fake FCM payload.");
        }
        return matcher.group(1);
    }
}
