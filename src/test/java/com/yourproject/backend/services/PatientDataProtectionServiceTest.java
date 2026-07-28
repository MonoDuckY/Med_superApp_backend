package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class PatientDataProtectionServiceTest {
    private final PatientDataProtectionService protectionService = new PatientDataProtectionService(key((byte) 1), key((byte) 2));

    @Test
    void encryptPatientFields_encryptsEveryPatientFieldAndClearsPlaintext() {
        User patient = patientWithPlaintextFields();

        protectionService.encryptPatientFields(patient);

        assertNull(patient.getPhoneNumber());
        assertNull(patient.getPatientId());
        assertNull(patient.getFullName());
        assertNull(patient.getGender());
        assertNull(patient.getDateOfBirth());
        assertNull(patient.getAddress());
        assertNull(patient.getCitizenIdentificationCode());
        assertNull(patient.getHealthInsuranceCode());
        assertNotEquals("Nguyen Van A", patient.getPatientFullNameEncrypted());
        assertTrue(patient.getPatientPhoneEncrypted().startsWith("v1:"));
        assertTrue(patient.getPatientIdEncrypted().startsWith("v1:"));
        assertNotNull(patient.getPatientGenderEncrypted());
        assertNotNull(patient.getPatientDateOfBirthEncrypted());
        assertNotNull(patient.getPatientAddressEncrypted());
        assertNotNull(patient.getPatientCitizenIdentificationCodeEncrypted());
        assertNotNull(patient.getPatientHealthInsuranceCodeEncrypted());
        assertEquals(1, patient.getEncryptionVersion());
    }

    @Test
    void decryptPatientFields_restoresEveryPatientField() {
        User patient = User.builder()
                .role(UserRole.PATIENT)
                .encryptionVersion(1)
                .patientPhoneEncrypted(protectionService.encryptSensitiveValue("+84363636363"))
                .patientIdEncrypted(protectionService.encryptSensitiveValue("PAT-001"))
                .patientFullNameEncrypted(protectionService.encryptSensitiveValue("Nguyen Van A"))
                .patientGenderEncrypted(protectionService.encryptSensitiveValue("MALE"))
                .patientDateOfBirthEncrypted(protectionService.encryptSensitiveValue("1995-01-01"))
                .patientAddressEncrypted(protectionService.encryptSensitiveValue("Ha Noi"))
                .patientCitizenIdentificationCodeEncrypted(protectionService.encryptSensitiveValue("001095000001"))
                .patientHealthInsuranceCodeEncrypted(protectionService.encryptSensitiveValue("HN-001"))
                .build();

        protectionService.decryptPatientFields(patient);

        assertEquals("+84363636363", patient.getPhoneNumber());
        assertEquals("PAT-001", patient.getPatientId());
        assertEquals("Nguyen Van A", patient.getFullName());
        assertEquals("MALE", patient.getGender());
        assertEquals(LocalDate.of(1995, 1, 1), patient.getDateOfBirth());
        assertEquals("Ha Noi", patient.getAddress());
        assertEquals("001095000001", patient.getCitizenIdentificationCode());
        assertEquals("HN-001", patient.getHealthInsuranceCode());
    }

    @Test
    void phoneLookup_isStableForTheSameNormalizedPhoneNumber() {
        assertEquals(protectionService.phoneLookup("+84363636363"), protectionService.phoneLookup("+84363636363"));
    }

    @Test
    void phoneLookup_returnsDifferentValueForUnknownPhoneNumber() {
        assertNotEquals(protectionService.phoneLookup("+84363636363"), protectionService.phoneLookup("+84912345678"));
    }

    @Test
    void patientIdLookup_isStableForTheSamePatientId() {
        assertEquals(protectionService.patientIdLookup("PAT-000001"), protectionService.patientIdLookup("PAT-000001"));
    }

    @Test
    void patientIdLookup_returnsDifferentValueForDifferentPatientId() {
        assertNotEquals(protectionService.patientIdLookup("PAT-000001"), protectionService.patientIdLookup("PAT-000002"));
    }

    @Test
    void encryptPatientFields_leavesNonPatientDataUntouched() {
        User doctor = User.builder().role(UserRole.DOCTOR).phoneNumber("+84363636363").fullName("Dr Nguyen").build();

        protectionService.encryptPatientFields(doctor);

        assertEquals("+84363636363", doctor.getPhoneNumber());
        assertNull(doctor.getEncryptionVersion());
    }

    @Test
    void decryptPatientFields_rejectsMalformedCiphertext() {
        User patient = User.builder().role(UserRole.PATIENT).encryptionVersion(1)
                .patientPhoneEncrypted("v1:not-valid-base64").build();

        assertThrows(IllegalStateException.class, () -> protectionService.decryptPatientFields(patient));
    }

    private User patientWithPlaintextFields() {
        return User.builder().role(UserRole.PATIENT).phoneNumber("+84363636363").patientId("PAT-001")
                .fullName("Nguyen Van A").gender("MALE").dateOfBirth(LocalDate.of(1995, 1, 1))
                .address("Ha Noi").citizenIdentificationCode("001095000001").healthInsuranceCode("HN-001").build();
    }

    private static String key(byte seed) {
        byte[] bytes = new byte[32];
        for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) (seed + index);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
