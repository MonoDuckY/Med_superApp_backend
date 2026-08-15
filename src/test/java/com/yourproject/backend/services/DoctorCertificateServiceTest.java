package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class DoctorCertificateServiceTest {
    @Mock
    private UserRepository userRepository;
    @Mock
    private S3StorageService s3StorageService;
    @InjectMocks
    private DoctorCertificateService service;

    @Test
    void upload_storesObjectKeyAndDeletesPreviousObject() {
        User doctor = doctor();
        doctor.setCertificateObjectKey("doctor-certificates/doctor-id/old.jpg");
        MockMultipartFile file = new MockMultipartFile("file", "certificate.jpg", "image/jpeg", new byte[] { 1 });
        when(s3StorageService.uploadDoctorCertificate("doctor-id", file)).thenReturn("doctor-certificates/doctor-id/new.jpg");

        User updated = service.upload(doctor, file);

        assertEquals("doctor-certificates/doctor-id/new.jpg", updated.getCertificateObjectKey());
        verify(userRepository).save(doctor);
        verify(s3StorageService).deleteObject("doctor-certificates/doctor-id/old.jpg");
    }

    @Test
    void upload_rejectsNonDoctorAccount() {
        User patient = User.builder().id("patient-id").roleId(UserRole.PATIENT.getId()).build();
        assertThrows(BadRequestException.class, () -> service.upload(patient, null));
    }

    @Test
    void delete_removesS3ObjectAndDatabaseReference() {
        User doctor = doctor();
        doctor.setCertificateObjectKey("doctor-certificates/doctor-id/old.jpg");
        service.remove(doctor);

        verify(s3StorageService).deleteObject("doctor-certificates/doctor-id/old.jpg");
        verify(userRepository).save(doctor);
        assertNull(doctor.getCertificateObjectKey());
    }

    private User doctor() {
        return User.builder().id("doctor-id").roleId(UserRole.DOCTOR.getId()).build();
    }
}
