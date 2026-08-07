package com.yourproject.backend.models;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@CompoundIndex(name = "phone_role_unique", def = "{'phoneLookup': 1, 'roleId': 1}", unique = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    private String passwordHash;
    @Indexed
    private String roleId;
    private AccountStatus status;

    private String fullName;
    @Transient
    private String patientId;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String citizenIdentificationCode;
    private String healthInsuranceCode;
    private String certificate;
    private String medicalHistory;
    private String currentSickness;
    private Double height;
    private Double weight;
    private String bloodType;
    private String accessTokenHash;
    private String refreshTokenHash;
    private Instant refreshTokenExpiresAt;
    private String deviceId;

    private String phoneNumber;

    private String phoneLookup;
    @Indexed
    private String citizenIdentificationLookup;
    @Transient
    private String patientIdLookup;

    private String patientPhoneEncrypted;
    @Transient
    private String patientIdEncrypted;
    private String patientFullNameEncrypted;
    private String patientGenderEncrypted;
    private String patientDateOfBirthEncrypted;
    private String patientAddressEncrypted;
    private String patientCitizenIdentificationCodeEncrypted;
    private String patientHealthInsuranceCodeEncrypted;
    private Integer encryptionVersion;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private String createdBy;
    private int failedLoginAttempts;
    private Instant lockedUntil;

    public void setRole(UserRole role) {
        this.roleId = role == null ? null : role.getId();
    }

    public UserRole getRole() {
        return roleId == null ? null : UserRole.fromId(roleId);
    }
}
