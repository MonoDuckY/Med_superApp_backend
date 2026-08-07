package com.yourproject.backend.models;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "medicine_schedules")
@CompoundIndex(
        name = "prescription_medicine_dosage_time_unique",
        def = "{'prescriptionId': 1, 'medicineName': 1, 'dosage': 1, 'scheduledAt': 1}",
        unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicineSchedule {
    @Id
    private String id;
    private String medicineName;
    private String dosage;
    @Indexed
    private Instant scheduledAt;
    @Indexed
    private MedicineScheduleStatus status;
    @Indexed
    private String prescriptionId;
    private String note;
}
