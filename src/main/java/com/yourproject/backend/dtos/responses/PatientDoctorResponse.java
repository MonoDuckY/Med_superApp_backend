package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.User;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PatientDoctorResponse {
    String id;
    String fullName;
    String phoneNumber;

    public static PatientDoctorResponse from(User doctor) {
        return PatientDoctorResponse.builder()
                .id(doctor.getId())
                .fullName(doctor.getFullName())
                .phoneNumber(doctor.getPhoneNumber())
                .build();
    }
}
