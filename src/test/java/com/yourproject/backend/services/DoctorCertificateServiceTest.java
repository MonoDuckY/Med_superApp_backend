package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.yourproject.backend.dtos.responses.DoctorCertificateResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

@ExtendWith(MockitoExtension.class)
class DoctorCertificateServiceTest {
    @Mock
    private UserService userService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3StorageService s3StorageService;
    @InjectMocks
    private DoctorCertificateService service;

    @Test
    void upload_storesObjectKeyAndReturnsPresignedUrl() {
        User doctor = doctor();
        MockMultipartFile file = new MockMultipartFile("file", "certificate.jpg", "image/jpeg", new byte[] { 1 });
        Instant expiresAt = Instant.parse("2026-08-10T12:00:00Z");
        when(userService.getUserById("doctor-id")).thenReturn(doctor);
        when(s3StorageService.uploadDoctorCertificate("doctor-id", file)).thenReturn("doctor-certificates/doctor-id/new.jpg");
        when(s3StorageService.createPresignedGetUrl("doctor-certificates/doctor-id/new.jpg"))
                .thenReturn(new PresignedObjectUrl("https://signed.example/new.jpg", expiresAt));

        DoctorCertificateResponse response = service.upload("doctor-id", file);

        assertEquals("doctor-certificates/doctor-id/new.jpg", doctor.getCertificateObjectKey());
        assertEquals("https://signed.example/new.jpg", response.getCertificateUrl());
        assertEquals(expiresAt, response.getExpiresAt());
        verify(userRepository).save(doctor);
    }

    @Test
    void get_rejectsDoctorWithoutCertificate() {
        when(userService.getUserById("doctor-id")).thenReturn(doctor());

        assertThrows(ResourceNotFoundException.class, () -> service.get("doctor-id"));
    }

    @Test
    void upload_rejectsNonDoctorAccount() {
        User patient = User.builder().id("patient-id").roleId(UserRole.PATIENT.getId()).build();
        when(userService.getUserById("patient-id")).thenReturn(patient);

        assertThrows(BadRequestException.class, () -> service.upload("patient-id", null));
    }

    @Test
    void delete_removesS3ObjectAndDatabaseReference() {
        User doctor = doctor();
        doctor.setCertificateObjectKey("doctor-certificates/doctor-id/old.jpg");
        when(userService.getUserById("doctor-id")).thenReturn(doctor);

        service.delete("doctor-id");

        verify(s3StorageService).deleteObject("doctor-certificates/doctor-id/old.jpg");
        verify(userRepository).save(doctor);
        assertNull(doctor.getCertificateObjectKey());
    }

    private User doctor() {
        return User.builder().id("doctor-id").roleId(UserRole.DOCTOR.getId()).build();
    }
}
