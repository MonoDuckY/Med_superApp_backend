package com.yourproject.backend.models;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "vital_signs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSign {
    @Id
    private String id;
    private String vitalName;
    private String vitalNumber;
    private String vitalUnit;
    @Indexed
    private String appointmentId;
}
