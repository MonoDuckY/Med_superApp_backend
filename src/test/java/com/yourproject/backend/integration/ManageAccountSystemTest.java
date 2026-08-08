package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;

/**
 * System Tests — BF-01 Manage account (TC-SYS-BF01-001 to TC-SYS-BF01-006)
 *
 * <p>Covers the full HTTP flow for the Manage Account business function:
 * <ol>
 *   <li>Admin logs in successfully.</li>
 *   <li>Admin creates a new user account (user_01).</li>
 *   <li>Admin edits an existing user account (user_01).</li>
 *   <li>Admin disables an existing user account (user_01).</li>
 *   <li>Exception: creating a user with an already-registered phone number returns 409.</li>
 *   <li>Exception: a disabled user attempting to log in receives 401.</li>
 * </ol>
 *
 * <p>Each test is independent — {@link MongoIntegrationTestBase#clearDatabase()} wipes all
 * collections before every test, so earlier steps are reproduced inline when needed as
 * setup rather than relying on shared mutable state between tests.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ManageAccountSystemTest extends MongoIntegrationTestBase {

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-001  Step 1 — Admin Login
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-001
     * Actor : Admin
     * Action: POST /api/auth/login with admin_01 (correct password)
     * Expect: 200 OK; success=true; valid accessToken and refreshToken; user.role=ADMIN
     */
    @Test
    @Order(1)
    void tcSysBf01001_AdminLoginSuccessful() throws Exception {
        // Given — an active admin account exists
        saveActiveAdmin("+84912345678", "Password123!");

        // When — admin sends correct credentials
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0912345678\",\"password\":\"Password123!\",\"role\":\"ADMIN\"}"))
                // Then — login is accepted and JWT pair is returned
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful."))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-002  Step 2 — Admin Creates User Account
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-002
     * Actor : Admin
     * Action: POST /api/admin/users with valid user_01 payload
     * Expect: 201 Created; success=true; user_01 appears in database with status=ACTIVE
     */
    @Test
    @Order(2)
    void tcSysBf01002_AdminCreateUser() throws Exception {
        // Given — an active admin is logged in
        saveActiveAdmin("+84912345678", "Password123!");
        String adminToken = loginAndGetAccessToken("0912345678", "Password123!");

        String createUserJson = """
                {
                    "phoneNumber": "0911111111",
                    "password": "Password123!",
                    "role": "STAFF",
                    "fullName": "Staff User One",
                    "gender": "MALE",
                    "dateOfBirth": "1990-01-01"
                }
                """;

        // When — admin calls the create-user endpoint
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserJson))
                // Then — account is created with the supplied data
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User account created successfully."))
                .andExpect(jsonPath("$.data.fullName").value("Staff User One"))
                .andExpect(jsonPath("$.data.phoneNumber").value("+84911111111"))
                .andExpect(jsonPath("$.data.role").value("STAFF"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.errorCode").doesNotExist());

        // And — the account is persisted in the database
        String phoneLookup = patientDataProtectionService.phoneLookup("+84911111111");
        assertTrue(userRepository.existsByPhoneLookupAndRoleId(phoneLookup, UserRole.STAFF.getId()),
                "user_01 should exist in the database after creation");
    }

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-003  Step 3 — Admin Edits User Account
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-003
     * Actor : Admin
     * Action: PUT /api/admin/users/{id} with updated fields for user_01
     * Expect: 200 OK; success=true; updated fields reflected in response and database
     */
    @Test
    @Order(3)
    void tcSysBf01003_AdminEditUser() throws Exception {
        // Given — admin creates user_01
        saveActiveAdmin("+84912345678", "Password123!");
        String adminToken = loginAndGetAccessToken("0912345678", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phoneNumber": "0911111111",
                                    "password": "Password123!",
                                    "role": "STAFF",
                                    "fullName": "Staff User One",
                                    "gender": "MALE",
                                    "dateOfBirth": "1990-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // When — admin patches user_01 with new fullName and gender
        mockMvc.perform(patch("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "fullName": "Staff User Updated",
                                    "gender": "FEMALE"
                                }
                                """))
                // Then — response reflects the updated values
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User account updated successfully."))
                .andExpect(jsonPath("$.data.fullName").value("Staff User Updated"))
                .andExpect(jsonPath("$.data.gender").value("FEMALE"))
                .andExpect(jsonPath("$.errorCode").doesNotExist());

        // And — the database reflects the same changes
        User updatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals("Staff User Updated", updatedUser.getFullName(),
                "fullName in DB should be updated");
        assertEquals("FEMALE", updatedUser.getGender(),
                "gender in DB should be updated");
    }

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-004  Step 4 — Admin Disables User Account
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-004
     * Actor : Admin
     * Action: DELETE /api/admin/users/{id} to disable user_01
     * Expect: 200 OK; success=true; user_01 status=INACTIVE in database
     */
    @Test
    @Order(4)
    void tcSysBf01004_AdminDisableUser() throws Exception {
        // Given — admin creates user_01
        saveActiveAdmin("+84912345678", "Password123!");
        String adminToken = loginAndGetAccessToken("0912345678", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phoneNumber": "0911111111",
                                    "password": "Password123!",
                                    "role": "STAFF",
                                    "fullName": "Staff User One",
                                    "gender": "MALE",
                                    "dateOfBirth": "1990-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        // When — admin deactivates user_01
        mockMvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                // Then — response confirms deactivation
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User account status changed successfully."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());

        // And — user_01 status is INACTIVE in the database
        User deactivatedUser = userRepository.findById(userId).orElseThrow();
        assertEquals(AccountStatus.INACTIVE, deactivatedUser.getStatus(),
                "user_01 status should be INACTIVE after deactivation");
    }

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-005  Exception — Duplicate Phone Number
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-005
     * Actor : Admin
     * Action: POST /api/admin/users with user_02 sharing user_01's phone number
     * Expect: 409 Conflict; success=false; errorCode=CONFLICT
     */
    @Test
    @Order(5)
    void tcSysBf01005_AdminCreateUserWithExistingPhone() throws Exception {
        // Given — admin creates user_01 with phone 0911111111
        saveActiveAdmin("+84912345678", "Password123!");
        String adminToken = loginAndGetAccessToken("0912345678", "Password123!");

        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phoneNumber": "0911111111",
                                    "password": "Password123!",
                                    "role": "STAFF",
                                    "fullName": "Staff User One",
                                    "gender": "MALE",
                                    "dateOfBirth": "1990-01-01"
                                }
                                """))
                .andExpect(status().isCreated());

        // When — admin attempts to create user_02 with the same phone number
        mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phoneNumber": "0911111111",
                                    "password": "Password999!",
                                    "role": "STAFF",
                                    "fullName": "Staff User Two",
                                    "gender": "FEMALE",
                                    "dateOfBirth": "1992-02-02"
                                }
                                """))
                // Then — conflict is returned because the phone number already exists
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("An account with this phone number and role already exists."));
    }

    // -------------------------------------------------------------------------
    // TC-SYS-BF01-006  Exception — Disabled Account Cannot Login
    // -------------------------------------------------------------------------

    /**
     * TC-SYS-BF01-006
     * Actor : Patient (or any user) with status=INACTIVE
     * Action: POST /api/auth/login with credentials of a disabled user (user_03)
     * Expect: 401 Unauthorized; success=false; errorCode=UNAUTHORIZED
     */
    @Test
    @Order(6)
    void tcSysBf01006_DisabledUserLoginFails() throws Exception {
        // Given — admin creates then deactivates user_03
        saveActiveAdmin("+84912345678", "Password123!");
        String adminToken = loginAndGetAccessToken("0912345678", "Password123!");

        MvcResult createResult = mockMvc.perform(post("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "phoneNumber": "0911111113",
                                    "password": "Password123!",
                                    "role": "STAFF",
                                    "fullName": "Staff User Three",
                                    "gender": "MALE",
                                    "dateOfBirth": "1990-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String userId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(patch("/api/admin/users/" + userId + "/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // When — user_03 (now INACTIVE) attempts to log in
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0911111113\",\"password\":\"Password123!\",\"role\":\"STAFF\"}"))
                // Then — login is rejected with 401 UNAUTHORIZED
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value(
                        "This account is inactive. Please contact an administrator."));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String loginAndGetAccessToken(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber
                                + "\",\"password\":\"" + password + "\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
