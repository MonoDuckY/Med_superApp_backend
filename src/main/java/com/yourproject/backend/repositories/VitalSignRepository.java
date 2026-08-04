package com.yourproject.backend.repositories;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.VitalSign;

public interface VitalSignRepository extends MongoRepository<VitalSign, String> {
    List<VitalSign> findAllByAppointmentIdOrderByVitalNameAsc(String appointmentId);
    void deleteAllByAppointmentId(String appointmentId);
}
