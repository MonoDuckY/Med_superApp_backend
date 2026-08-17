package com.yourproject.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.yourproject.backend.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.models.User;
import com.yourproject.backend.services.S3StorageService;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrlWithKey;

class ResearcherMedicalImageIntegrationTest extends MongoIntegrationTestBase {

    @MockitoBean
    private S3StorageService s3StorageService;

    // TC-INT-ResearcherMedicalImageController-001
    @Test
    void getAvailableFolders_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        when(s3StorageService.listMedicalImageFolders()).thenReturn(List.of("folder1", "folder2"));

        mockMvc.perform(get("/api/researcher/medical-images/folders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("folder1"))
                .andExpect(jsonPath("$.data[1]").value("folder2"));
    }

    // TC-INT-ResearcherMedicalImageController-002
    @Test
    void getImagesByFolder_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        PresignedObjectUrlWithKey image = new PresignedObjectUrlWithKey("objKey", "http://url", java.time.Instant.parse("2026-12-31T23:59:59Z"));
        when(s3StorageService.listMedicalImagesByFolder("folder1")).thenReturn(List.of(image));

        mockMvc.perform(get("/api/researcher/medical-images/folders/folder1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].imageId").value("objKey"))
                .andExpect(jsonPath("$.data[0].url").value("http://url"))
                .andExpect(jsonPath("$.data[0].expiresAt").value("2026-12-31T23:59:59Z"));
    }

    // TC-INT-ResearcherMedicalImageController-003
    @Test
    void cloneFolder_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        doNothing().when(s3StorageService).cloneMedicalImageFolder("srcFolder", "destFolder");

        mockMvc.perform(post("/api/researcher/medical-images/folders/clone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sourceFolderName\":\"srcFolder\",\"targetFolderName\":\"destFolder\"}")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // TC-INT-ResearcherMedicalImageController-004
    @Test
    void uploadImage_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());
        when(s3StorageService.uploadMedicalImageToFolder(eq("folder1"), any(MultipartFile.class))).thenReturn("uploadedKey");

        mockMvc.perform(multipart("/api/researcher/medical-images/folders/folder1/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("uploadedKey"));
    }

    // TC-INT-ResearcherMedicalImageController-006
    @Test
    void getImagesByFolder_InvalidFolder_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        when(s3StorageService.listMedicalImagesByFolder("invalid..folder"))
                .thenThrow(new BadRequestException("A valid medical image folder name is required."));

        mockMvc.perform(get("/api/researcher/medical-images/folders/invalid..folder")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("A valid medical image folder name is required."));
    }

    // TC-INT-ResearcherMedicalImageController-007
    @Test
    void uploadImage_EmptyFile_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        when(s3StorageService.uploadMedicalImageToFolder(eq("folder1"), any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Medical image is required."));

        mockMvc.perform(multipart("/api/researcher/medical-images/folders/folder1/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Medical image is required."));
    }

    // TC-INT-ResearcherMedicalImageController-008
    @Test
    void uploadImage_InvalidFormat_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
        when(s3StorageService.uploadMedicalImageToFolder(eq("folder1"), any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Medical image must be JPEG, PNG, or WEBP."));

        mockMvc.perform(multipart("/api/researcher/medical-images/folders/folder1/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Medical image must be JPEG, PNG, or WEBP."));
    }

    // TC-INT-ResearcherMedicalImageController-009
    @Test
    void uploadImage_FileSizeExceeds_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "large.png", MediaType.IMAGE_PNG_VALUE, "large".getBytes());
        when(s3StorageService.uploadMedicalImageToFolder(eq("folder1"), any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Medical image must not exceed 5 MB."));

        mockMvc.perform(multipart("/api/researcher/medical-images/folders/folder1/images")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Medical image must not exceed 5 MB."));
    }

    // TC-INT-ResearcherMedicalImageController-005
    @Test
    void unauthorizedAndForbidden() throws Exception {
        // Unauthorized
        mockMvc.perform(get("/api/researcher/medical-images/folders"))
                .andExpect(status().isUnauthorized());

        // Forbidden
        User patient = saveActivePatient("+84911223344");
        String token = generateAccessTokenAndSave(patient);

        mockMvc.perform(get("/api/researcher/medical-images/folders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
