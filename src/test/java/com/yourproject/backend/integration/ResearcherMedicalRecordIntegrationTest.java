package com.yourproject.backend.integration;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.yourproject.backend.dtos.responses.ResearcherMedicalImageResponse;
import com.yourproject.backend.exceptions.ResourceNotFoundException;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.ResearcherMedicalRecordService;

class ResearcherMedicalRecordIntegrationTest extends MongoIntegrationTestBase {

    @MockitoBean
    private ResearcherMedicalRecordService researcherMedicalRecordService;

    // TC-INT-ResearcherMedicalRecordController-001
    @Test
    void getAllMedicalRecordImages_Success() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        ResearcherMedicalImageResponse responseItem = ResearcherMedicalImageResponse.builder()
                .patientId("pat-1")
                .appointmentId("apt-1")
                .medicalRecordId("rec-1")
                .build();

        when(researcherMedicalRecordService.getAllMedicalRecordImages()).thenReturn(List.of(responseItem));

        mockMvc.perform(get("/api/researcher/medical-records/images")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].patientId").value("pat-1"))
                .andExpect(jsonPath("$.data[0].appointmentId").value("apt-1"))
                .andExpect(jsonPath("$.data[0].medicalRecordId").value("rec-1"));
    }

    // TC-INT-ResearcherMedicalRecordController-004
    @Test
    void getAllMedicalRecordImages_NotFound_ThrowsNotFound() throws Exception {
        User researcher = saveActiveResearcher("+84911223344", "Password123!");
        String token = generateAccessTokenAndSave(researcher);

        when(researcherMedicalRecordService.getAllMedicalRecordImages())
                .thenThrow(new ResourceNotFoundException("Appointment was not found."));

        mockMvc.perform(get("/api/researcher/medical-records/images")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Appointment was not found."));
    }

    // TC-INT-ResearcherMedicalRecordController-002
    @Test
    void getAllMedicalRecordImages_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/researcher/medical-records/images"))
                .andExpect(status().isUnauthorized());
    }

    // TC-INT-ResearcherMedicalRecordController-003
    @Test
    void getAllMedicalRecordImages_Forbidden() throws Exception {
        User patient = saveActivePatient("+84911223344");
        String token = generateAccessTokenAndSave(patient);

        mockMvc.perform(get("/api/researcher/medical-records/images")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}
