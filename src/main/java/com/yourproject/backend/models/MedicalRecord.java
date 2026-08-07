package com.yourproject.backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "medical_records")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRecord {
    @Id
    private String id;

    @Indexed(unique = true)
    private String appointmentId;

    private String diagnosis;
    private String note;
    private String bloodPressure;
    private Integer heartRate;
    private Integer breathingRate;
    private Double bodyTemperature;
    private Double bloodLipids;
}
