package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.dtos.responses.ResearcherMedicalImageResponse;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.models.User;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class ResearcherMedicalRecordServiceTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private MedicalImageService medicalImageService;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @InjectMocks
    private ResearcherMedicalRecordService service;

    // TC-UNIT-ResearcherMedicalRecordService-001
    @Test
    void getAllMedicalRecordImages_Success() {
        MedicalRecord record = new MedicalRecord();
        record.setId("recordId");
        record.setAppointmentId("appointmentId");
        record.setMedicalImages(List.of("imageId"));

        Appointment appointment = new Appointment();
        appointment.setId("appointmentId");
        appointment.setPatientUserId("patientId");

        User patient = new User();
        patient.setId("patientId");

        when(medicalRecordRepository.findAll()).thenReturn(List.of(record));
        when(appointmentRepository.findById("appointmentId")).thenReturn(Optional.of(appointment));
        when(userRepository.findById("patientId")).thenReturn(Optional.of(patient));
        when(medicalImageService.createResponses(record)).thenReturn(Collections.emptyList());

        List<ResearcherMedicalImageResponse> responses = service.getAllMedicalRecordImages();

        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("patientId", responses.get(0).getPatientId());
        assertEquals("appointmentId", responses.get(0).getAppointmentId());
        assertEquals("recordId", responses.get(0).getMedicalRecordId());

        verify(medicalRecordRepository).findAll();
        verify(appointmentRepository).findById("appointmentId");
        verify(userRepository).findById("patientId");
        verify(medicalImageService).createResponses(record);
    }

    // TC-UNIT-ResearcherMedicalRecordService-002
    @Test
    void getAllMedicalRecordImages_ThrowsResourceNotFoundException_WhenAppointmentNotFound() {
        MedicalRecord record = new MedicalRecord();
        record.setAppointmentId("appointmentId");
        record.setMedicalImages(List.of("imageId"));

        when(medicalRecordRepository.findAll()).thenReturn(List.of(record));
        when(appointmentRepository.findById("appointmentId")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getAllMedicalRecordImages());
        assertEquals("Appointment was not found.", exception.getMessage());
    }

    // TC-UNIT-ResearcherMedicalRecordService-003
    @Test
    void getAllMedicalRecordImages_ThrowsResourceNotFoundException_WhenPatientNotFound() {
        MedicalRecord record = new MedicalRecord();
        record.setAppointmentId("appointmentId");
        record.setMedicalImages(List.of("imageId"));

        Appointment appointment = new Appointment();
        appointment.setId("appointmentId");
        appointment.setPatientUserId("patientId");

        when(medicalRecordRepository.findAll()).thenReturn(List.of(record));
        when(appointmentRepository.findById("appointmentId")).thenReturn(Optional.of(appointment));
        when(userRepository.findById("patientId")).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> service.getAllMedicalRecordImages());
        assertEquals("Patient was not found.", exception.getMessage());
    }
}
