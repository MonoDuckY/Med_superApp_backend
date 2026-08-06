package com.yourproject.backend.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.MedicalRecord;

public interface MedicalRecordRepository extends MongoRepository<MedicalRecord, String> {
    Optional<MedicalRecord> findByAppointmentId(String appointmentId);
    List<MedicalRecord> findAllByAppointmentIdIn(Collection<String> appointmentIds);
}
