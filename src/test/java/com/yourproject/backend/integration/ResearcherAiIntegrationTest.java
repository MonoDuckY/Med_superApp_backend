package com.yourproject.backend.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
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
import com.yourproject.backend.services.ResearcherAiDetectionService;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

class ResearcherAiIntegrationTest extends MongoIntegrationTestBase {

    @MockitoBean
    private ResearcherAiDetectionService researcherAiDetectionService;

    // TC-INT-ResearcherAiController-001
    @Test
    void detect_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode mockResponse = mapper.createObjectNode();
        mockResponse.put("result", "detected");

        when(researcherAiDetectionService.detect(any(MultipartFile.class))).thenReturn(mockResponse);

        mockMvc.perform(multipart("/api/researcher/detect")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.result").value("detected"));
    }

    // TC-INT-ResearcherAiController-004
    @Test
    void detect_EmptyFile_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "empty.png", MediaType.IMAGE_PNG_VALUE, new byte[0]);
        when(researcherAiDetectionService.detect(any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Image is required."));

        mockMvc.perform(multipart("/api/researcher/detect")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Image is required."));
    }

    // TC-INT-ResearcherAiController-005
    @Test
    void detect_InvalidFormat_ThrowsBadRequest() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        MockMultipartFile file = new MockMultipartFile("file", "doc.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());
        when(researcherAiDetectionService.detect(any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Invalid image format."));

        mockMvc.perform(multipart("/api/researcher/detect")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid image format."));
    }

    // TC-INT-ResearcherAiController-002
    @Test
    void detect_Unauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/researcher/detect")
                        .file(file))
                .andExpect(status().isUnauthorized());
    }

    // TC-INT-ResearcherAiController-003
    @Test
    void detect_Forbidden() throws Exception {
        User patient = saveActivePatient("+84911223344");
        String token = generateAccessTokenAndSave(patient);

        MockMultipartFile file = new MockMultipartFile("file", "image.png", MediaType.IMAGE_PNG_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/researcher/detect")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
