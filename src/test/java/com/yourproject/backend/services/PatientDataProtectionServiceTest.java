package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class PatientDataProtectionServiceTest {
    private final PatientDataProtectionService protectionService = new PatientDataProtectionService(key((byte) 1), key((byte) 2));

    @Test
    void encryptPatientFields_replacesPlaintextWithRandomizedCiphertextAndDecryptsIt() {
        User patient = User.builder().role(UserRole.PATIENT).phoneNumber("+84363636363").patientId("PAT-001")
                .fullName("Nguyen Van A").gender("MALE").dateOfBirth(LocalDate.of(1995, 1, 1))
                .address("Ha Noi").citizenIdentificationCode("001095000001").healthInsuranceCode("HN-001").build();

        protectionService.encryptPatientFields(patient);

        assertNull(patient.getPhoneNumber());
        assertNull(patient.getFullName());
        assertNotEquals("Nguyen Van A", patient.getPatientFullNameEncrypted());
        protectionService.decryptPatientFields(patient);
        assertEquals("+84363636363", patient.getPhoneNumber());
        assertEquals("PAT-001", patient.getPatientId());
        assertEquals("Nguyen Van A", patient.getFullName());
        assertEquals(LocalDate.of(1995, 1, 1), patient.getDateOfBirth());
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

    private static String key(byte seed) {
        byte[] bytes = new byte[32];
        for (int index = 0; index < bytes.length; index++) bytes[index] = (byte) (seed + index);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
