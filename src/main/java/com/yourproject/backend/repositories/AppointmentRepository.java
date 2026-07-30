package com.yourproject.backend.repositories;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    Optional<Appointment> findByDoctorWorkSlotIdAndActiveTrue(String doctorWorkSlotId);

    boolean existsByPatientUserIdAndAppointmentDateAndActiveTrue(String patientUserId, LocalDate appointmentDate);

    List<Appointment> findAllByPatientUserIdOrderByRequestedAtDesc(String patientUserId);

    List<Appointment> findAllByStatusOrderByRequestedAtAsc(AppointmentStatus status);

    List<Appointment> findAllByStatusOrderByRequestedAtDesc(AppointmentStatus status);

    List<Appointment> findAllByOrderByRequestedAtDesc();
}
