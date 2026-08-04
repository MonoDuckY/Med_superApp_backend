package com.yourproject.backend.dtos.responses;

import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoctorExaminationResponse {
    AppointmentResponse appointment;
    UserResponse patient;
    List<VitalSignResponse> vitalSigns;
    List<PrescriptionResponse> prescriptions;
}
