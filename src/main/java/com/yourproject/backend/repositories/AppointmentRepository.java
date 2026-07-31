package com.yourproject.backend.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    Optional<Appointment> findByDoctorWorkSlotIdAndActiveTrue(String doctorWorkSlotId);

    boolean existsByPatientUserIdAndAppointmentDateAndActiveTrue(String patientUserId, LocalDate appointmentDate);

    List<Appointment> findAllByPatientUserIdOrderByRequestedAtDesc(String patientUserId);

    @Query(value = "{'$or': [{'patientId': ?0}, {'patientUserId': ?0}]}", sort = "{'requestedAt': -1}")
    List<Appointment> findAllForPatient(String patientId);

    List<Appointment> findAllByStatusOrderByRequestedAtAsc(AppointmentStatus status);

    List<Appointment> findAllByStatusOrderByRequestedAtDesc(AppointmentStatus status);

    List<Appointment> findAllByOrderByRequestedAtDesc();

    Optional<Appointment> findFirstByDoctorWorkSlotIdAndStatusIn(
            String doctorWorkSlotId,
            Collection<AppointmentStatus> statuses);
}
