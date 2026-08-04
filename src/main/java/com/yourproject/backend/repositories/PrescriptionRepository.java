package com.yourproject.backend.repositories;

import java.util.Collection;
import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Prescription;

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findAllByAppointmentIdOrderByIdAsc(String appointmentId);
    List<Prescription> findAllByAppointmentIdIn(Collection<String> appointmentIds);
}
