package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserResponse {
    String id;
    UserRole role;
    AccountStatus status;
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    String phoneNumber;
    String address;
    String citizenIdentificationCode;
    String healthInsuranceCode;
    String certificate;
    String medicalHistory;
    String currentSickness;
    Double height;
    Double weight;
    String bloodType;
    Instant createdAt;
    Instant updatedAt;
    Instant lastLoginAt;

    public static UserResponse from(User user, PatientDataProtectionService patientDataProtectionService) {
        User responseUser = copyOf(user);
        patientDataProtectionService.decryptPatientFields(responseUser);
        return fromUnprotected(responseUser);
    }

    public static UserResponse fromUnprotected(User user) {
        return UserResponse.builder()
                .id(user.getId()).role(user.getRole()).status(user.getStatus())
                .fullName(user.getFullName())
                .gender(user.getGender()).dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber()).address(user.getAddress())
                .citizenIdentificationCode(user.getCitizenIdentificationCode())
                .healthInsuranceCode(user.getHealthInsuranceCode()).certificate(user.getCertificate())
                .medicalHistory(user.getMedicalHistory()).currentSickness(user.getCurrentSickness())
                .height(user.getHeight()).weight(user.getWeight()).bloodType(user.getBloodType())
                .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }

    private static User copyOf(User user) {
        return User.builder().id(user.getId()).passwordHash(user.getPasswordHash()).roleId(user.getRoleId())
                .status(user.getStatus()).fullName(user.getFullName())
                .gender(user.getGender()).dateOfBirth(user.getDateOfBirth()).address(user.getAddress())
                .citizenIdentificationCode(user.getCitizenIdentificationCode()).healthInsuranceCode(user.getHealthInsuranceCode())
                .certificate(user.getCertificate()).medicalHistory(user.getMedicalHistory())
                .currentSickness(user.getCurrentSickness()).height(user.getHeight()).weight(user.getWeight())
                .bloodType(user.getBloodType()).phoneNumber(user.getPhoneNumber()).phoneLookup(user.getPhoneLookup())
                .patientIdLookup(user.getPatientIdLookup()).patientPhoneEncrypted(user.getPatientPhoneEncrypted())
                .patientIdEncrypted(user.getPatientIdEncrypted()).patientFullNameEncrypted(user.getPatientFullNameEncrypted())
                .patientGenderEncrypted(user.getPatientGenderEncrypted()).patientDateOfBirthEncrypted(user.getPatientDateOfBirthEncrypted())
                .patientAddressEncrypted(user.getPatientAddressEncrypted())
                .patientCitizenIdentificationCodeEncrypted(user.getPatientCitizenIdentificationCodeEncrypted())
                .patientHealthInsuranceCodeEncrypted(user.getPatientHealthInsuranceCodeEncrypted())
                .encryptionVersion(user.getEncryptionVersion()).createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt()).passwordChangedAt(user.getPasswordChangedAt()).createdBy(user.getCreatedBy()).build();
    }
}
