package com.yourproject.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.ResearcherLamaService;

class ResearcherLamaIntegrationTest extends MongoIntegrationTestBase {

    @MockitoBean
    private ResearcherLamaService researcherLamaService;

    // TC-INT-ResearcherLamaController-001
    @Test
    void inpaint_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("image", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());
        ResearcherLamaService.ProcessedImage mockProcessedImage = new ResearcherLamaService.ProcessedImage("processed_content".getBytes(), MediaType.IMAGE_PNG);

        when(researcherLamaService.inpaint(any(MultipartFile.class))).thenReturn(mockProcessedImage);

        mockMvc.perform(multipart("/api/researcher/LaMa")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes("processed_content".getBytes()));
    }

    // TC-INT-ResearcherLamaController-004
    @Test
    void inpaint_EmptyFile_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("image", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        when(researcherLamaService.inpaint(any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Image is required."));

        mockMvc.perform(multipart("/api/researcher/LaMa")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image is required."));
    }

    // TC-INT-ResearcherLamaController-005
    @Test
    void inpaint_InvalidFormat_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("image", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
        when(researcherLamaService.inpaint(any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Invalid image format."));

        mockMvc.perform(multipart("/api/researcher/LaMa")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid image format."));
    }

    // TC-INT-ResearcherLamaController-002
    @Test
    void inpaint_Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/researcher/LaMa")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // TC-INT-ResearcherLamaController-003
    @Test
    void inpaint_Forbidden() throws Exception {
        User patient = saveActivePatient("+84911223344");
        String token = generateAccessTokenAndSave(patient);

        MockMultipartFile file = new MockMultipartFile("image", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/researcher/LaMa")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
