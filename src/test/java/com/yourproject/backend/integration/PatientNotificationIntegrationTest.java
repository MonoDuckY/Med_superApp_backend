package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import com.yourproject.backend.models.Notification;
import com.yourproject.backend.models.NotificationStatus;
import com.yourproject.backend.models.User;

class PatientNotificationIntegrationTest extends MongoIntegrationTestBase {
    @Test
    void patientReadsAndMarksNotificationsRead() throws Exception {
        User patient = saveActivePatient("+84911111111");
        String authorization = bearer(patient);
        Notification first = notificationRepository.save(Notification.builder()
                .userId(patient.getId())
                .content("Appointment confirmed.")
                .notifyTime(Instant.now().minusSeconds(60))
                .status(NotificationStatus.UNREAD)
                .build());
        notificationRepository.save(Notification.builder()
                .userId(patient.getId())
                .content("Medication reminder.")
                .notifyTime(Instant.now())
                .status(NotificationStatus.UNREAD)
                .build());

        mockMvc.perform(get("/api/patient/notifications").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].content").value("Medication reminder."));

        mockMvc.perform(get("/api/patient/notifications/unread-count")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));

        mockMvc.perform(patch("/api/patient/notifications/{notificationId}/read", first.getId())
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READ"));

        mockMvc.perform(patch("/api/patient/notifications/read-all")
                        .header("Authorization", authorization))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/patient/notifications/unread-count")
                        .header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    void patientCannotMarkAnotherPatientsNotificationRead() throws Exception {
        User patient = saveActivePatient("+84911111111");
        User otherPatient = saveActivePatient("+84922222222", "PAT-OTHER");
        Notification notification = notificationRepository.save(Notification.builder()
                .userId(otherPatient.getId())
                .content("Private notification")
                .notifyTime(Instant.now())
                .status(NotificationStatus.UNREAD)
                .build());

        mockMvc.perform(patch("/api/patient/notifications/{notificationId}/read", notification.getId())
                        .header("Authorization", bearer(patient)))
                .andExpect(status().isNotFound());
    }

    private String bearer(User user) throws Exception {
        String token = jwtUtils.generateAccessToken(user);
        user.setAccessTokenHash(Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        userRepository.save(user);
        return "Bearer " + token;
    }
}
