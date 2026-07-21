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

    @Indexed(unique = true)
    private String username;

    private String passwordHash;
    private UserRole role;
    private AccountStatus status;

    @Indexed(unique = true, sparse = true)
    private String patientId;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String phoneNumber;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastLoginAt;
    private Instant passwordChangedAt;
    private String createdBy;
}
