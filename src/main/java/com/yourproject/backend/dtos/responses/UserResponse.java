package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.time.LocalDate;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

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

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .role(user.getRole())
                .status(user.getStatus())
                .patientId(user.getPatientId())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .citizenIdentificationCode(user.getCitizenIdentificationCode())
                .healthInsuranceCode(user.getHealthInsuranceCode())
                .certificate(user.getCertificate())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}
