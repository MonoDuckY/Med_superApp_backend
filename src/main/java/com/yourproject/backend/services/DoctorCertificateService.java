package com.yourproject.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DoctorCertificateService {
    private final UserRepository userRepository;
    private final S3StorageService s3StorageService;

    public User upload(User doctor, MultipartFile file) {
        requireDoctor(doctor);
        String previousObjectKey = doctor.getCertificateObjectKey();
        String newObjectKey = s3StorageService.uploadDoctorCertificate(doctor.getId(), file);
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
        return doctor;
    }

    public User remove(User user) {
        String objectKey = user.getCertificateObjectKey();
        if (objectKey == null || objectKey.isBlank()) {
            return user;
        }
        s3StorageService.deleteObject(objectKey);
        user.setCertificateObjectKey(null);
        return userRepository.save(user);
    }

    public PresignedObjectUrl createPresignedUrl(User doctor) {
        if (doctor.getCertificateObjectKey() == null || doctor.getCertificateObjectKey().isBlank()) {
            return null;
        }
        return s3StorageService.createPresignedGetUrl(doctor.getCertificateObjectKey());
    }

    private void requireDoctor(User user) {
        if (user == null || user.getRole() != UserRole.DOCTOR) {
            throw new BadRequestException("Certificates can only be managed for Doctor accounts.");
        }
    }
}
