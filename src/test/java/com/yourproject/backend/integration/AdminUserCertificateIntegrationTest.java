package com.yourproject.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.DoctorCertificateService;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

class AdminUserCertificateIntegrationTest extends MongoIntegrationTestBase {
    @MockitoBean
    private DoctorCertificateService doctorCertificateService;

    @Test
    void createDoctorRequiresCertificatePart() throws Exception {
        String token = adminToken();

        mockMvc.perform(multipart("/api/admin/users")
                        .file(jsonPart("{\"phoneNumber\":\"0911111111\",\"password\":\"Doctor123!\","
                                + "\"role\":\"DOCTOR\",\"fullName\":\"Doctor Missing Certificate\"}"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Doctor accounts require a certificate file."));
    }

    @Test
    void createDoctorUploadsCertificateAndReturnsAdminPresignedUrl() throws Exception {
        String token = adminToken();
        MockMultipartFile certificate = certificate();
        stubCertificateUpload(certificate, "doctor-certificates/new-doctor/certificate.jpg");

        mockMvc.perform(multipart("/api/admin/users")
                        .file(jsonPart("{\"phoneNumber\":\"0911111111\",\"password\":\"Doctor123!\","
                                + "\"role\":\"DOCTOR\",\"fullName\":\"Doctor With Certificate\"}"))
                        .file(certificate)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("DOCTOR"))
                .andExpect(jsonPath("$.data.hasCertificate").value(true))
                .andExpect(jsonPath("$.data.certificateUrl").value("https://signed.example/certificate.jpg"));
    }

    @Test
    void createStaffRejectsCertificatePart() throws Exception {
        String token = adminToken();

        mockMvc.perform(multipart("/api/admin/users")
                        .file(jsonPart("{\"phoneNumber\":\"0922222222\",\"password\":\"Staff123!\","
                                + "\"role\":\"STAFF\",\"fullName\":\"Staff User\"}"))
                        .file(certificate())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only Doctor accounts can have a certificate file."));
    }

    @Test
    void patchDoctorCanReplaceCertificate() throws Exception {
        String token = adminToken();
        User doctor = saveActiveDoctor("+84911111111", "Doctor123!");
        MockMultipartFile certificate = certificate();
        stubCertificateUpload(certificate, "doctor-certificates/doctor-id/replaced.jpg");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/admin/users/{userId}", doctor.getId())
                        .file(jsonPart("{\"fullName\":\"Doctor Updated\"}"))
                        .file(certificate)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fullName").value("Doctor Updated"))
                .andExpect(jsonPath("$.data.hasCertificate").value(true))
                .andExpect(jsonPath("$.data.certificateUrl").value("https://signed.example/certificate.jpg"));
    }

    private String adminToken() throws Exception {
        saveActiveAdmin("+84999999999", "Admin123!");
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0999999999\",\"role\":\"ADMIN\",\"password\":\"Admin123!\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.data.accessToken");
    }

    private MockMultipartFile jsonPart(String json) {
        return new MockMultipartFile(
                "user",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile certificate() {
        return new MockMultipartFile(
                "certificate",
                "certificate.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                new byte[] { 1, 2, 3 });
    }

    private void stubCertificateUpload(MockMultipartFile certificate, String objectKey) {
        when(doctorCertificateService.upload(any(User.class), eq(certificate))).thenAnswer(invocation -> {
            User doctor = invocation.getArgument(0);
            doctor.setCertificateObjectKey(objectKey);
            userRepository.save(doctor);
            return doctor;
        });
        when(doctorCertificateService.createPresignedUrl(any(User.class)))
                .thenReturn(new PresignedObjectUrl(
                        "https://signed.example/certificate.jpg",
                        Instant.now().plusSeconds(600)));
    }
}
