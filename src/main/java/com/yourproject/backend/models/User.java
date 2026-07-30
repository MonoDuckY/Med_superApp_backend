package com.yourproject.backend.models;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    private String id;

    private String passwordHash;
    private Set<UserRole> roles;
    private UserRole role;
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

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    @Indexed(unique = true, sparse = true)
    private String phoneLookup;
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

    public UserRole getRole() {
        return roles == null || roles.isEmpty() ? role : roles.iterator().next();
    }

    public Set<UserRole> getRoles() {
        return roles == null || roles.isEmpty()
                ? (role == null ? Set.of() : Set.of(role))
                : roles;
    }

    public void setRole(UserRole role) {
        this.roles = role == null ? Set.of() : Set.of(role);
        this.role = role;
    }
}
