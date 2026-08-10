package com.yourproject.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.responses.DoctorCertificateResponse;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorCertificateService {
    private final UserService userService;
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public DoctorCertificateResponse upload(String doctorId, MultipartFile file) {
        User doctor = requireDoctor(doctorId);
        String previousObjectKey = doctor.getCertificateObjectKey();
        String newObjectKey = s3StorageService.uploadDoctorCertificate(doctorId, file);
        doctor.setCertificateObjectKey(newObjectKey);
        try {
            userRepository.save(doctor);
        } catch (RuntimeException exception) {
            s3StorageService.deleteObject(newObjectKey);
            throw exception;
        }
        if (previousObjectKey != null && !previousObjectKey.isBlank()) {
            s3StorageService.deleteObject(previousObjectKey);
        }
        return responseFor(doctor);
    }

    public DoctorCertificateResponse get(String doctorId) {
        User doctor = requireDoctor(doctorId);
        if (doctor.getCertificateObjectKey() == null || doctor.getCertificateObjectKey().isBlank()) {
            throw new ResourceNotFoundException("Doctor certificate was not found.");
        }
        return responseFor(doctor);
    }

    public void delete(String doctorId) {
        User doctor = requireDoctor(doctorId);
        String objectKey = doctor.getCertificateObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            throw new ResourceNotFoundException("Doctor certificate was not found.");
        }
        s3StorageService.deleteObject(objectKey);
        doctor.setCertificateObjectKey(null);
        userRepository.save(doctor);
    }

    private User requireDoctor(String doctorId) {
        User user = userService.getUserById(doctorId);
        if (user.getRole() != UserRole.DOCTOR) {
            throw new BadRequestException("Certificates can only be managed for Doctor accounts.");
        }
        return user;
    }

    private DoctorCertificateResponse responseFor(User doctor) {
        PresignedObjectUrl presignedUrl = s3StorageService.createPresignedGetUrl(doctor.getCertificateObjectKey());
        return DoctorCertificateResponse.builder()
                .doctorId(doctor.getId())
                .certificateUrl(presignedUrl.url())
                .expiresAt(presignedUrl.expiresAt())
                .build();
    }
}
