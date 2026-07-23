package com.yourproject.backend.models;

import java.time.Instant;
import java.time.LocalDate;

import org.springframework.data.annotation.Id;
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
    private UserRole role;
    private AccountStatus status;

    @Indexed(unique = true, sparse = true)
    private String patientId;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String address;
    private String citizenIdentificationCode;
    private String healthInsuranceCode;
    private String certificate;

    @Indexed(unique = true, sparse = true)
    private String phoneNumber;

    @Indexed(unique = true, sparse = true)
    private String phoneLookup;

    @Indexed(unique = true, sparse = true)
    private String patientIdLookup;
    private String patientPhoneEncrypted;
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
}
