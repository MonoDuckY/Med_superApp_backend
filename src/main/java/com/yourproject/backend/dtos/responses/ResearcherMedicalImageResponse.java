package com.yourproject.backend.dtos.responses;

import java.time.Instant;
import java.util.List;

import com.yourproject.backend.models.AppointmentStatus;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ResearcherMedicalImageResponse {
    String patientId;
    UserSummaryResponse patient;
    String appointmentId;
    AppointmentStatus appointmentStatus;
    Instant appointmentRequestedAt;
    String medicalRecordId;
    List<MedicalImageResponse> medicalImages;
}
