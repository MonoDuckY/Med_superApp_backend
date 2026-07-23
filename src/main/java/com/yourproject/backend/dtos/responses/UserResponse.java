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
    String patientId;
    String fullName;
    String gender;
    LocalDate dateOfBirth;
    String phoneNumber;
    String address;
    String citizenIdentificationCode;
    String healthInsuranceCode;
    String certificate;
    Instant createdAt;
    Instant updatedAt;
    Instant lastLoginAt;

    public static UserResponse from(User user, PatientDataProtectionService patientDataProtectionService) {
        User responseUser = copyOf(user);
        patientDataProtectionService.decryptPatientFields(responseUser);
        return UserResponse.builder()
                .id(responseUser.getId()).role(responseUser.getRole()).status(responseUser.getStatus())
                .patientId(responseUser.getPatientId()).fullName(responseUser.getFullName())
                .gender(responseUser.getGender()).dateOfBirth(responseUser.getDateOfBirth())
                .phoneNumber(responseUser.getPhoneNumber()).address(responseUser.getAddress())
                .citizenIdentificationCode(responseUser.getCitizenIdentificationCode())
                .healthInsuranceCode(responseUser.getHealthInsuranceCode()).certificate(responseUser.getCertificate())
                .createdAt(responseUser.getCreatedAt()).updatedAt(responseUser.getUpdatedAt())
                .lastLoginAt(responseUser.getLastLoginAt())
                .build();
    }

    private static User copyOf(User user) {
        return User.builder().id(user.getId()).passwordHash(user.getPasswordHash()).role(user.getRole())
                .status(user.getStatus()).patientId(user.getPatientId()).fullName(user.getFullName())
                .gender(user.getGender()).dateOfBirth(user.getDateOfBirth()).address(user.getAddress())
                .citizenIdentificationCode(user.getCitizenIdentificationCode()).healthInsuranceCode(user.getHealthInsuranceCode())
                .certificate(user.getCertificate()).phoneNumber(user.getPhoneNumber()).phoneLookup(user.getPhoneLookup())
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
