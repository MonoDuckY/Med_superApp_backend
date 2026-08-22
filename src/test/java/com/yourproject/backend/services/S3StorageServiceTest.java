package com.yourproject.backend.services;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import com.yourproject.backend.exceptions.BadRequestException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {
    private static final byte[] VALID_PNG_BYTES = new byte[] { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0 };
    private static final byte[] VALID_JPEG_BYTES = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0 };

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner s3Presigner;
    @InjectMocks
    private S3StorageService service;

    @BeforeEach
    void configureService() {
        ReflectionTestUtils.setField(service, "bucketName", "test-bucket");
        ReflectionTestUtils.setField(service, "medicalImagePrefix", "");
        ReflectionTestUtils.setField(service, "presignedUrlMinutes", 10L);
        ReflectionTestUtils.setField(service, "maxFileSizeBytes", 5_242_880L);
    }

    @Test
    void uploadMedicalImageUsesDefaultPrefixWhenConfigurationIsBlank() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "scan.png", "image/png", VALID_PNG_BYTES);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = service.uploadMedicalImage("record-1", image);

        assertTrue(objectKey.startsWith("medical-images/record-1/"));
        assertTrue(objectKey.endsWith(".png"));
        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(request.capture(), any(RequestBody.class));
        assertTrue(request.getValue().key().startsWith("medical-images/record-1/"));
    }

    @Test
    void uploadMedicalImageUsesConfiguredPrefix() {
        ReflectionTestUtils.setField(service, "medicalImagePrefix", "/clinical-images/");
        MockMultipartFile image = new MockMultipartFile(
                "image", "scan.jpg", "image/jpeg", VALID_JPEG_BYTES);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = service.uploadMedicalImage("record-2", image);

        assertTrue(objectKey.startsWith("clinical-images/record-2/"));
    }

    @Test
    void uploadMedicalImageRejectsUnsupportedContentType() {
        MockMultipartFile image = new MockMultipartFile(
                "image", "scan.gif", "image/gif", new byte[] { 1 });

        assertThrows(BadRequestException.class, () -> service.uploadMedicalImage("record-3", image));
    }

    @Test
    void uploadDoctorCertificateAcceptsPdfWithValidSignature() {
        MockMultipartFile pdf = new MockMultipartFile(
                "certificate",
                "license.pdf",
                "application/pdf",
                "%PDF-1.7 certificate".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        String objectKey = service.uploadDoctorCertificate("doctor-1", pdf);

        assertTrue(objectKey.startsWith("doctor-certificates/doctor-1/"));
        assertTrue(objectKey.endsWith(".pdf"));
    }

    @Test
    void uploadDoctorCertificateRejectsPdfWithInvalidSignature() {
        MockMultipartFile fakePdf = new MockMultipartFile(
                "certificate",
                "license.pdf",
                "application/pdf",
                "not-a-pdf".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        assertThrows(BadRequestException.class,
                () -> service.uploadDoctorCertificate("doctor-1", fakePdf));
    }

    @Test
    void uploadMedicalImageStillRejectsPdf() {
        MockMultipartFile pdf = new MockMultipartFile(
                "image",
                "scan.pdf",
                "application/pdf",
                "%PDF-1.7 scan".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        assertThrows(BadRequestException.class, () -> service.uploadMedicalImage("record-4", pdf));
    }
}
