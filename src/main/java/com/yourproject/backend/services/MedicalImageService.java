package com.yourproject.backend.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.responses.MedicalImageResponse;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MedicalImageService {
    private final MedicalRecordRepository medicalRecordRepository;
    private final S3StorageService s3StorageService;

    public List<MedicalImageResponse> upload(MedicalRecord medicalRecord, MultipartFile image) {
        String objectKey = s3StorageService.uploadMedicalImage(medicalRecord.getId(), image);
        List<String> objectKeys = mutableObjectKeys(medicalRecord);
        objectKeys.add(objectKey);
        medicalRecord.setMedicalImages(objectKeys);
        try {
            MedicalRecord saved = medicalRecordRepository.save(medicalRecord);
            return createResponses(saved);
        } catch (RuntimeException exception) {
            s3StorageService.deleteObject(objectKey);
            throw exception;
        }
    }

    public List<MedicalImageResponse> delete(MedicalRecord medicalRecord, String imageId) {
        List<String> objectKeys = mutableObjectKeys(medicalRecord);
        String objectKey = objectKeys.stream()
                .filter(key -> imageId(key).equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Medical image was not found."));
        s3StorageService.deleteObject(objectKey);
        objectKeys.remove(objectKey);
        medicalRecord.setMedicalImages(objectKeys);
        return createResponses(medicalRecordRepository.save(medicalRecord));
    }

    public List<MedicalImageResponse> createResponses(MedicalRecord medicalRecord) {
        if (medicalRecord == null || medicalRecord.getMedicalImages() == null) return List.of();
        return medicalRecord.getMedicalImages().stream()
                .filter(key -> key != null && !key.isBlank())
                .map(this::createResponse)
                .toList();
    }

    private MedicalImageResponse createResponse(String objectKey) {
        PresignedObjectUrl signedUrl = s3StorageService.createPresignedGetUrl(objectKey);
        return MedicalImageResponse.builder()
                .imageId(imageId(objectKey))
                .url(signedUrl.url())
                .expiresAt(signedUrl.expiresAt())
                .build();
    }

    private List<String> mutableObjectKeys(MedicalRecord medicalRecord) {
        return medicalRecord.getMedicalImages() == null
                ? new ArrayList<>()
                : new ArrayList<>(medicalRecord.getMedicalImages());
    }

    private String imageId(String objectKey) {
        int separator = objectKey.lastIndexOf('/');
        return separator < 0 ? objectKey : objectKey.substring(separator + 1);
    }
}
