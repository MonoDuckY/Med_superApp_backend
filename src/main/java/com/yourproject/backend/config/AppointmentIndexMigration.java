package com.yourproject.backend.config;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.Appointment;

@Component
@Order(2)
public class AppointmentIndexMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentIndexMigration.class);
    private static final Set<String> LEGACY_INDEXES = Set.of(
            "active_work_slot_appointment_unique",
            "active_patient_appointment_day_unique");

    private final MongoTemplate mongoTemplate;

    public AppointmentIndexMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        IndexOperations indexOperations = mongoTemplate.indexOps(Appointment.class);
        indexOperations.getIndexInfo().stream()
                .map(index -> index.getName())
                .filter(LEGACY_INDEXES::contains)
                .forEach(indexName -> {
                    indexOperations.dropIndex(indexName);
                    LOGGER.info("Removed legacy appointment index: {}", indexName);
                });
    }
}
