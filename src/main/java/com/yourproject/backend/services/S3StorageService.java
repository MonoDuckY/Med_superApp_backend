package com.yourproject.backend.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class S3StorageService {
    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp");
    private static final Map<String, String> ALLOWED_CERTIFICATE_TYPES = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "application/pdf", ".pdf");
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

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
        return uploadFile(
                normalizedPrefix(certificatePrefix, "doctor-certificates"),
                doctorId,
                file,
                validateCertificateFile(file),
                "Unable to upload the doctor certificate.");
    }

    public String uploadMedicalImage(String medicalRecordId, MultipartFile file) {
        return uploadFile(
                normalizedPrefix(medicalImagePrefix, "medical-images"),
                medicalRecordId,
                file,
                validateMedicalImage(file),
                "Unable to upload the medical image.");
    }

    private String uploadFile(
            String prefix,
            String ownerId,
            MultipartFile file,
            String extension,
            String failureMessage) {
        validateConfiguration();
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

    public List<String> listMedicalImageFolders() {
        validateConfiguration();
        String prefix = normalizedPrefix(medicalImagePrefix, "medical-images") + "/";
        try {
            return s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .delimiter("/")
                            .build())
                    .stream()
                    .flatMap(page -> page.commonPrefixes().stream())
                    .map(commonPrefix -> commonPrefix.prefix().substring(prefix.length()))
                    .map(folder -> folder.endsWith("/") ? folder.substring(0, folder.length() - 1) : folder)
                    .filter(folder -> !folder.isBlank())
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (S3Exception exception) {
            throw new FileStorageException("Unable to list medical image folders.");
        }
    }

    public List<PresignedObjectUrlWithKey> listMedicalImagesByFolder(String folderName) {
        validateConfiguration();
        if (folderName == null || folderName.isBlank() || folderName.contains("/")
                || folderName.contains("\\") || folderName.contains("..")) {
            throw new BadRequestException("A valid medical image folder name is required.");
        }
        String prefix = normalizedPrefix(medicalImagePrefix, "medical-images") + "/" + folderName + "/";
        try {
            List<PresignedObjectUrlWithKey> result = new ArrayList<>();
            s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .build())
                    .stream()
                    .flatMap(page -> page.contents().stream())
                    .filter(object -> !object.key().endsWith("/"))
                    .sorted((left, right) -> left.key().compareToIgnoreCase(right.key()))
                    .forEach(object -> {
                        PresignedObjectUrl signedUrl = createPresignedGetUrl(object.key());
                        result.add(new PresignedObjectUrlWithKey(object.key(), signedUrl.url(), signedUrl.expiresAt()));
                    });
            return result;
        } catch (S3Exception exception) {
            throw new FileStorageException("Unable to list medical images.");
        }
    }

    public String uploadMedicalImageToFolder(String folderName, MultipartFile file) {
        validateFolderName(folderName);
        validateConfiguration();
        String objectKey = normalizedPrefix(medicalImagePrefix, "medical-images")
                + "/" + folderName + "/" + UUID.randomUUID() + validateMedicalImage(file);
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
            throw new FileStorageException("Unable to upload the medical image.");
        }
    }

    public void cloneMedicalImageFolder(String sourceFolderName, String targetFolderName) {
        validateFolderName(targetFolderName);
        if (sourceFolderName != null && !sourceFolderName.isBlank()) {
            validateFolderName(sourceFolderName);
        }
        validateConfiguration();
        String prefix = normalizedPrefix(medicalImagePrefix, "medical-images") + "/";
        String targetPrefix = prefix + targetFolderName + "/";
        try {
            boolean copied = false;
            if (sourceFolderName != null && !sourceFolderName.isBlank()) {
                String sourcePrefix = prefix + sourceFolderName + "/";
                for (var page : s3Client.listObjectsV2Paginator(ListObjectsV2Request.builder()
                        .bucket(bucketName).prefix(sourcePrefix).build())) {
                    for (var object : page.contents()) {
                        String relativeKey = object.key().substring(sourcePrefix.length());
                        if (relativeKey.isBlank()) {
                            continue;
                        }
                        s3Client.copyObject(CopyObjectRequest.builder()
                                .copySource(bucketName + "/" + object.key())
                                .bucket(bucketName)
                                .key(targetPrefix + relativeKey)
                                .serverSideEncryption(ServerSideEncryption.AES256)
                                .build());
                        copied = true;
                    }
                }
            }
            if (!copied) {
                s3Client.putObject(PutObjectRequest.builder().bucket(bucketName).key(targetPrefix).build(),
                        RequestBody.empty());
            }
        } catch (S3Exception exception) {
            throw new FileStorageException("Unable to clone the medical image folder.");
        }
    }

    private void validateFolderName(String folderName) {
        if (folderName == null || folderName.isBlank() || folderName.contains("/")
                || folderName.contains("\\") || folderName.contains("..")) {
            throw new BadRequestException("A valid medical image folder name is required.");
        }
    }

    private String validateCertificateFile(MultipartFile file) {
        validateFilePresenceAndSize(file, "Certificate file");
        String extension = ALLOWED_CERTIFICATE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Certificate file must be JPEG, PNG, WEBP, or PDF.");
        }
        if ("application/pdf".equals(file.getContentType())) {
            validatePdfSignature(file);
        }
        return extension;
    }

    private String validateMedicalImage(MultipartFile file) {
        validateFilePresenceAndSize(file, "Medical image");
        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new BadRequestException("Medical image must be JPEG, PNG, or WEBP.");
        }
        return extension;
    }

    private void validateFilePresenceAndSize(MultipartFile file, String fileLabel) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException(fileLabel + " is required.");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException(fileLabel + " must not exceed 5 MB.");
        }
    }

    private void validatePdfSignature(MultipartFile file) {
        try (var inputStream = file.getInputStream()) {
            byte[] signature = inputStream.readNBytes(PDF_SIGNATURE.length);
            if (!Arrays.equals(signature, PDF_SIGNATURE)) {
                throw new BadRequestException("Certificate PDF has an invalid file signature.");
            }
        } catch (IOException exception) {
            throw new BadRequestException("Certificate PDF could not be read.");
        }
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

    public record PresignedObjectUrlWithKey(String objectKey, String url, Instant expiresAt) {
    }
}
