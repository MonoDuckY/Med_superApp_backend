package com.yourproject.backend.services;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

@Service
public class PatientDataProtectionService {
    private static final int AES_KEY_LENGTH = 32;
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec aesKey;
    private final SecretKeySpec hmacKey;

    public PatientDataProtectionService(
            @Value("${app.patient-data.aes-key}") String encodedAesKey,
            @Value("${app.patient-data.lookup-hmac-key}") String encodedHmacKey) {
        this.aesKey = new SecretKeySpec(decodeKey(encodedAesKey, "PATIENT_DATA_AES_KEY"), "AES");
        this.hmacKey = new SecretKeySpec(decodeKey(encodedHmacKey, "PATIENT_LOOKUP_HMAC_KEY"), "HmacSHA256");
    }

    public String phoneLookup(String normalizedPhoneNumber) {
        return hmac(normalizedPhoneNumber);
    }

    public String patientIdLookup(String patientId) {
        return hmac(patientId);
    }

    public void encryptPatientFields(User user) {
        if (user.getRole() != UserRole.PATIENT) return;
        user.setPatientPhoneEncrypted(encrypt(user.getPhoneNumber()));
        user.setPatientIdEncrypted(encrypt(user.getPatientId()));
        user.setPatientFullNameEncrypted(encrypt(user.getFullName()));
        user.setPatientGenderEncrypted(encrypt(user.getGender()));
        user.setPatientDateOfBirthEncrypted(encrypt(user.getDateOfBirth().toString()));
        user.setPatientAddressEncrypted(encrypt(user.getAddress()));
        user.setPatientCitizenIdentificationCodeEncrypted(encrypt(user.getCitizenIdentificationCode()));
        user.setPatientHealthInsuranceCodeEncrypted(encrypt(user.getHealthInsuranceCode()));
        user.setPhoneNumber(null);
        user.setPatientId(null);
        user.setFullName(null);
        user.setGender(null);
        user.setDateOfBirth(null);
        user.setAddress(null);
        user.setCitizenIdentificationCode(null);
        user.setHealthInsuranceCode(null);
        user.setEncryptionVersion(1);
    }

    public void decryptPatientFields(User user) {
        if (user.getRole() != UserRole.PATIENT || user.getEncryptionVersion() == null) return;
        user.setPhoneNumber(decrypt(user.getPatientPhoneEncrypted()));
        user.setPatientId(decrypt(user.getPatientIdEncrypted()));
        user.setFullName(decrypt(user.getPatientFullNameEncrypted()));
        user.setGender(decrypt(user.getPatientGenderEncrypted()));
        user.setDateOfBirth(LocalDate.parse(decrypt(user.getPatientDateOfBirthEncrypted())));
        user.setAddress(decrypt(user.getPatientAddressEncrypted()));
        user.setCitizenIdentificationCode(decrypt(user.getPatientCitizenIdentificationCodeEncrypted()));
        user.setHealthInsuranceCode(decrypt(user.getPatientHealthInsuranceCodeEncrypted()));
    }

    private String encrypt(String value) {
        if (value == null) return null;
        try {
            byte[] iv = new byte[IV_LENGTH];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(ByteBuffer.allocate(iv.length + ciphertext.length).put(iv).put(ciphertext).array());
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to encrypt patient data.", exception);
        }
    }

    private String decrypt(String value) {
        if (value == null) return null;
        try {
            byte[] payload = Base64.getUrlDecoder().decode(value.substring(3));
            byte[] iv = new byte[IV_LENGTH];
            byte[] ciphertext = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to decrypt patient data.", exception);
        }
    }

    private String hmac(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to create patient lookup.", exception);
        }
    }

    private byte[] decodeKey(String encodedKey, String variableName) {
        try {
            byte[] key = Base64.getDecoder().decode(encodedKey);
            if (key.length != AES_KEY_LENGTH) throw new IllegalArgumentException();
            return key;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(variableName + " must be a Base64-encoded 32-byte key.");
        }
    }
}
