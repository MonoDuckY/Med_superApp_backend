package com.yourproject.backend.repositories;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Prescription;

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    Optional<Prescription> findByMedicalRecordId(String medicalRecordId);
    boolean existsByMedicalRecordId(String medicalRecordId);
    List<Prescription> findAllByMedicalRecordIdOrderByIdAsc(String medicalRecordId);
    List<Prescription> findAllByMedicalRecordIdIn(Collection<String> medicalRecordIds);
}
