package com.yourproject.backend.services;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.FileStorageException;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService {
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${app.aws.s3.bucket-name}")
    private String bucketName;
    @Value("${app.aws.s3.certificate-prefix}")
    private String certificatePrefix;
    @Value("${app.aws.s3.medical-image-prefix:medical-images}")
    private String medicalImagePrefix;
    @Value("${app.aws.s3.presigned-url-minutes}")
    private long presignedUrlMinutes;
    @Value("${app.aws.s3.max-file-size-bytes}")
    private long maxFileSizeBytes;

    public String uploadDoctorCertificate(String doctorId, MultipartFile file) {
        return uploadImage(
                normalizedPrefix(certificatePrefix, "doctor-certificates"),
                doctorId,
                file,
                "Certificate image",
                "Unable to upload the doctor certificate.");
    }

    public String uploadMedicalImage(String medicalRecordId, MultipartFile file) {
        return uploadImage(
                normalizedPrefix(medicalImagePrefix, "medical-images"),
                medicalRecordId,
                file,
                "Medical image",
                "Unable to upload the medical image.");
    }

    private String uploadImage(
            String prefix,
            String ownerId,
            MultipartFile file,
            String imageLabel,
            String failureMessage) {
        validateConfiguration();
        String extension = validateImage(file, imageLabel);
        String objectKey = prefix + "/" + ownerId + "/" + UUID.randomUUID() + extension;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build();
            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return objectKey;
        } catch (IOException | S3Exception exception) {
            throw new FileStorageException(failureMessage);
        }
    }

    public PresignedObjectUrl createPresignedGetUrl(String objectKey) {
        validateConfiguration();
        if (objectKey == null || objectKey.isBlank()) {
            throw new BadRequestException("Object key is required.");
        }
        Duration duration = Duration.ofMinutes(presignedUrlMinutes);
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(duration)
                    .getObjectRequest(getObjectRequest)
                    .build();
            String url = s3Presigner.presignGetObject(presignRequest).url().toString();
            return new PresignedObjectUrl(url, Instant.now().plus(duration));
        } catch (RuntimeException exception) {
            throw new FileStorageException("Unable to create an object access URL.");
        }
    }

    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        validateConfiguration();
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(objectKey).build());
        } catch (S3Exception exception) {
            throw new FileStorageException("Unable to delete the stored image.");
        }
    }

    private String validateImage(MultipartFile file, String imageLabel) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(imageLabel + " is required.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException(imageLabel + " must not exceed 5 MB.");
        }
        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException(imageLabel + " must be JPEG, PNG, or WEBP.");
        }
        return extension;
    }

    private void validateConfiguration() {
        if (bucketName == null || bucketName.isBlank()) {
            throw new FileStorageException("AWS S3 bucket is not configured.");
        }
        if (presignedUrlMinutes <= 0) {
            throw new FileStorageException("AWS S3 presigned URL duration must be positive.");
        }
    }

    private String normalizedPrefix(String configuredPrefix, String fallback) {
        String prefix = configuredPrefix == null ? "" : configuredPrefix.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix.isBlank() ? fallback : prefix;
    }

    public record PresignedObjectUrl(String url, Instant expiresAt) {
    }
}
