package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.User;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UserSummaryResponse {
    String id;
    String fullName;
    String phoneNumber;
    String certificate;

    public static UserSummaryResponse from(
            User user,
            PatientDataProtectionService patientDataProtectionService) {
        if (user == null) {
            return null;
        }
        UserResponse response = patientDataProtectionService == null
                ? UserResponse.fromUnprotected(user)
                : UserResponse.from(user, patientDataProtectionService);
        return UserSummaryResponse.builder()
                .id(response.getId())
                .fullName(response.getFullName())
                .phoneNumber(response.getPhoneNumber())
                .certificate(response.getCertificate())
                .build();
    }
}
