package com.yourproject.backend.dtos.responses;

import com.yourproject.backend.models.MedicalRecord;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MedicalRecordResponse {
    String id;
    String appointmentId;
    String diagnosis;
    String note;
    String bloodPressure;
    Integer heartRate;
    Integer breathingRate;
    Double bodyTemperature;
    Double bloodLipids;

    public static MedicalRecordResponse from(MedicalRecord record) {
        if (record == null) return null;
        return MedicalRecordResponse.builder()
                .id(record.getId())
                .appointmentId(record.getAppointmentId())
                .diagnosis(record.getDiagnosis())
                .note(record.getNote())
                .bloodPressure(record.getBloodPressure())
                .heartRate(record.getHeartRate())
                .breathingRate(record.getBreathingRate())
                .bodyTemperature(record.getBodyTemperature())
                .bloodLipids(record.getBloodLipids())
                .build();
    }
}
