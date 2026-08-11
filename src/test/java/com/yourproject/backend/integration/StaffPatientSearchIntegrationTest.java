package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

class StaffPatientSearchIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void staffSearchesActivePatientsByClosestName() throws Exception {
        saveNamedPatient("Nguyễn Văn An", "+84911111111");
        saveNamedPatient("Nguyễn Văn Anh", "+84922222222");
        saveNamedPatient("Trần Minh Đức", "+84933333333");
        saveActiveStaff("+84944444444", "StaffPassword1!");
        String token = loginAccessToken("0944444444", UserRole.STAFF, "StaffPassword1!");

        mockMvc.perform(get("/api/staff/patients/search")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "nguyen van an")
                        .param("n", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].fullName").value("Nguyễn Văn An"))
                .andExpect(jsonPath("$.data[1].fullName").value("Nguyễn Văn Anh"));
    }

    @Test
    void nonStaffCannotSearchPatients() throws Exception {
        saveActiveAdmin("+84944444444", "AdminPassword1!");
        String token = loginAccessToken("0944444444", UserRole.ADMIN, "AdminPassword1!");

        mockMvc.perform(get("/api/staff/patients/search")
                        .header("Authorization", "Bearer " + token)
                        .param("name", "Nguyen")
                        .param("n", "5"))
                .andExpect(status().isForbidden());
    }

    private User saveNamedPatient(String fullName, String phoneNumber) {
        Instant now = Instant.now();
        User patient = User.builder()
                .roleId(UserRole.PATIENT.getId())
                .status(AccountStatus.ACTIVE)
                .fullName(fullName)
                .gender("NONE")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .phoneNumber(phoneNumber)
                .phoneLookup(patientDataProtectionService.phoneLookup(phoneNumber))
                .createdAt(now)
                .updatedAt(now)
                .build();
        patientDataProtectionService.encryptPatientFields(patient);
        return userRepository.save(patient);
    }

    private String loginAccessToken(String phoneNumber, UserRole role, String password) throws Exception {
        String body = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/auth/login")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"role\":\"" + role
                                + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.data.accessToken");
    }
}
