package com.yourproject.backend.config;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.repositories.MedicalRecordRepository;

import lombok.RequiredArgsConstructor;

@Component
@Order(4)
@RequiredArgsConstructor
public class ClinicalRecordMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClinicalRecordMigration.class);

    private final MongoTemplate mongoTemplate;
    private final MedicalRecordRepository medicalRecordRepository;

    @Override
    public void run(ApplicationArguments args) {
        long migrated = 0;
        for (Document prescription : mongoTemplate.getCollection("prescriptions")
                .find(Filters.and(
                        Filters.exists("appointmentId", true),
                        Filters.exists("medicalRecordId", false)))) {
            String appointmentId = prescription.getString("appointmentId");
            if (appointmentId == null || appointmentId.isBlank()) continue;

            MedicalRecord medicalRecord = medicalRecordRepository.findByAppointmentId(appointmentId)
                    .orElseGet(() -> createMedicalRecord(appointmentId));
            mongoTemplate.getCollection("prescriptions").updateOne(
                    Filters.eq("_id", prescription.get("_id")),
                    Updates.combine(
                            Updates.set("medicalRecordId", medicalRecord.getId()),
                            Updates.unset("appointmentId")));
            migrated++;
        }
        if (migrated > 0) {
            LOGGER.info("Migrated {} prescriptions from appointmentId to medicalRecordId.", migrated);
        }
    }

    private MedicalRecord createMedicalRecord(String appointmentId) {
        Document appointment = mongoTemplate.getCollection("appointments")
                .find(Filters.eq("_id", mongoId(appointmentId)))
                .first();
        return medicalRecordRepository.save(MedicalRecord.builder()
                .appointmentId(appointmentId)
                .diagnosis(appointment == null ? null : appointment.getString("diagnosis"))
                .build());
    }

    private Object mongoId(String id) {
        return ObjectId.isValid(id) ? new ObjectId(id) : id;
    }
}
