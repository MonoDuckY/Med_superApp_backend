package com.yourproject.backend.services;

import java.util.List;

import org.springframework.stereotype.Service;

import com.yourproject.backend.dtos.responses.ResearcherMedicalImageResponse;
import com.yourproject.backend.dtos.responses.UserSummaryResponse;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ResearcherMedicalRecordService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final MedicalImageService medicalImageService;
    private final PatientDataProtectionService patientDataProtectionService;

    public List<ResearcherMedicalImageResponse> getAllMedicalRecordImages() {
        return medicalRecordRepository.findAll().stream()
                .filter(record -> record.getMedicalImages() != null && !record.getMedicalImages().isEmpty())
                .map(record -> {
                    Appointment appointment = appointmentRepository.findById(record.getAppointmentId())
                            .orElseThrow(() -> new ResourceNotFoundException("Appointment was not found."));
                    String patientId = appointment.getPatientUserId() != null
                            ? appointment.getPatientUserId()
                            : appointment.getPatientId();
                    var patient = userRepository.findById(patientId)
                            .orElseThrow(() -> new ResourceNotFoundException("Patient was not found."));
                    return ResearcherMedicalImageResponse.builder()
                            .patientId(patient.getId())
                            .patient(UserSummaryResponse.from(patient, patientDataProtectionService))
                            .appointmentId(appointment.getId())
                            .appointmentStatus(appointment.getStatus())
                            .appointmentRequestedAt(appointment.getRequestedAt())
                            .medicalRecordId(record.getId())
                            .medicalImages(medicalImageService.createResponses(record))
                            .build();
                })
                .toList();
    }
}
