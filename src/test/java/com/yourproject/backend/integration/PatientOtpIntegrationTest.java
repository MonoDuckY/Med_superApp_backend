package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
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
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.TrustedDevice;
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

    @Test
    void otpRequestWithoutPhoneNumberReturnsValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(0, patientOtpRepository.count());
        verifyNoInteractions(fcmGatewayService);
    }

    @Test
    void unknownPhoneCannotRequestOtp() throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid phone number or password."));

        assertEquals(0, patientOtpRepository.count());
        verifyNoInteractions(fcmGatewayService);
    }

    @Test
    void nonPatientCannotRequestOrVerifyOtp() throws Exception {
        saveActiveDoctor(PHONE, "Password123!");

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only patient accounts can use SMS OTP."));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only patient accounts can use SMS OTP."));

        assertEquals(0, patientOtpRepository.count());
        verifyNoInteractions(fcmGatewayService);
    }

    @Test
    void inactivePatientCannotRequestOrVerifyOtp() throws Exception {
        User patient = saveActivePatient(PHONE);
        patient.setStatus(AccountStatus.INACTIVE);
        userRepository.save(patient);

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        assertEquals(0, patientOtpRepository.count());
        verifyNoInteractions(fcmGatewayService);
    }

    @Test
    void malformedOtpVerificationFieldsReturnValidationErrorWithoutIncrementingAttempts() throws Exception {
        User patient = saveActivePatient(PHONE);
        PatientOtp otp = saveOtp(patient, "123456", 0, Instant.now(), Instant.now().plusSeconds(300));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"123456\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"12345A\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));

        assertEquals(0, patientOtpRepository.findById(otp.getId()).orElseThrow().getAttempts());
    }

    @Test
    void overlongDeviceIdIsRejectedByOtpEndpoints() throws Exception {
        String deviceId = "d".repeat(256);

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"" + deviceId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_ERROR"));
    }

    @Test
    void malformedPhoneIsRejectedByOtpEndpoints() throws Exception {
        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"12345\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"12345\",\"code\":\"123456\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void verificationWithoutExistingOtpIsRejected() throws Exception {
        saveActivePatient(PHONE);

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("OTP is invalid or expired."));
    }

    @Test
    void validOtpWithoutDeviceIdDoesNotCreateTrustedDevice() throws Exception {
        User patient = saveActivePatient(PHONE);
        saveOtp(patient, "123456", 0, Instant.now(), Instant.now().plusSeconds(300));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        assertEquals(0, trustedDeviceRepository.count());
        assertEquals(1, refreshTokenRepository.count());
    }

    @Test
    void revokedTrustedDeviceMustRequestOtpAgain() throws Exception {
        User patient = saveActivePatient(PHONE);
        trustedDeviceRepository.save(TrustedDevice.builder()
                .userId(patient.getId())
                .deviceId("revoked-device")
                .verifiedAt(Instant.now().minusSeconds(60))
                .revokedAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"revoked-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        captureSentOtp(1);
        assertEquals(1, patientOtpRepository.count());
    }

    @Test
    void trustedDeviceBelongingToAnotherPatientDoesNotBypassOtp() throws Exception {
        User firstPatient = saveActivePatient("+84911111111", "PAT-FIRST");
        saveActivePatient("+84922222222", "PAT-SECOND");
        trustedDeviceRepository.save(TrustedDevice.builder()
                .userId(firstPatient.getId())
                .deviceId("shared-device")
                .verifiedAt(Instant.now())
                .build());

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0922222222\",\"deviceId\":\"shared-device\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(fcmGatewayService).sendSmsCommand(eq("integration-test-fcm-token"), eq("+84922222222"), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void fifthIncorrectOtpBlocksSubsequentCorrectCode() throws Exception {
        User patient = saveActivePatient(PHONE);
        PatientOtp otp = saveOtp(patient, "123456", 4, Instant.now(), Instant.now().plusSeconds(300));

        mockMvc.perform(post("/api/auth/patient-otp/verify").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"code\":\"654321\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isUnauthorized());
        assertEquals(5, patientOtpRepository.findById(otp.getId()).orElseThrow().getAttempts());

        assertOtpRejected("123456");
        assertEquals(0, refreshTokenRepository.count());
    }

    @Test
    void onlyLatestOtpCanCompleteAuthentication() throws Exception {
        User patient = saveActivePatient(PHONE);
        requestOtp("device-a");
        String firstCode = captureSentOtp(1);
        PatientOtp firstOtp = patientOtpRepository.findAll().get(0);
        firstOtp.setCreatedAt(Instant.now().minusSeconds(61));
        patientOtpRepository.save(firstOtp);

        requestOtp("device-a");
        String secondCode = captureSentOtp(2);

        assertOtpRejected(firstCode);
        verifyOtp(secondCode, "device-a");
        assertEquals(1, refreshTokenRepository.count());
        assertEquals(1, trustedDeviceRepository.count());
    }

    @Test
    void smsGatewayFailureRemovesGeneratedOtp() throws Exception {
        saveActivePatient(PHONE);
        doThrow(new RuntimeException("FCM unavailable")).when(fcmGatewayService)
                .sendSmsCommand(eq("integration-test-fcm-token"), eq(PHONE), org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(post("/api/auth/patient-otp/request").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"deviceId\":\"device-a\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("OTP could not be delivered. Please try again."));

        assertEquals(0, patientOtpRepository.count());
        assertEquals(0, refreshTokenRepository.count());
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
