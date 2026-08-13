package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.WorkScheduleExpirationJob;

class SchedulingIntegrationTest extends MongoIntegrationTestBase {
    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private WorkScheduleExpirationJob workScheduleExpirationJob;

    @Test
    void doctorMustSubmitWorkScheduleAtLeastOneDayInAdvance() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "Password123!");
        ClinicRoom room = saveRoom("ROOM-101", "Clinic Room 101");

        submitSchedule(doctor, LocalDate.now(HOSPITAL_ZONE), "MORNING", room.getId(), 400);

        assertEquals(0, doctorWorkSlotRepository.count());
    }

    @Test
    void expirationJobRejectsPendingScheduleWhenWorkDateHasArrived() {
        LocalDate today = LocalDate.now(HOSPITAL_ZONE);
        DoctorWorkSlot pending = doctorWorkSlotRepository.save(DoctorWorkSlot.builder()
                .submissionId("expired-submission")
                .doctorId("doctor-1")
                .workDate(today)
                .slotId("slot-1")
                .roomId("room-1")
                .status(DoctorWorkSlotStatus.PENDING)
                .submittedAt(Instant.now().minusSeconds(3600))
                .updatedAt(Instant.now().minusSeconds(3600))
                .build());

        workScheduleExpirationJob.rejectExpiredPendingSchedules();

        DoctorWorkSlot rejected = doctorWorkSlotRepository.findById(pending.getId()).orElseThrow();
        assertEquals(DoctorWorkSlotStatus.REJECTED, rejected.getStatus());
        assertEquals(
                WorkScheduleExpirationJob.AUTOMATIC_REJECTION_REASON,
                rejected.getRejectionReason());
    }

    @Test
    void completeDoctorStaffPatientStaffSchedulingFlow() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "Password123!");
        User staff = saveActiveStaff("+84922222222", "Password123!");
        User patient = saveActivePatient("+84933333333", "PAT-SCHEDULE-1");
        ClinicRoom room = saveRoom("ROOM-101", "Clinic Room 101");
        LocalDate workDate = LocalDate.now(HOSPITAL_ZONE).plusDays(2);

        mockMvc.perform(post("/api/doctor/work-schedules")
                        .header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest(workDate, "MORNING", room.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.slots.length()").value(8));

        List<DoctorWorkSlot> submittedSlots = doctorWorkSlotRepository.findAll();
        assertEquals(8, submittedSlots.size());
        String submissionId = submittedSlots.get(0).getSubmissionId();

        mockMvc.perform(patch("/api/staff/scheduling/work-schedules/{submissionId}/decision", submissionId)
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("AVAILABLE"));

        DoctorWorkSlot availableSlot = doctorWorkSlotRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(DoctorWorkSlot::getSlotId))
                .findFirst()
                .orElseThrow();
        assertEquals(DoctorWorkSlotStatus.AVAILABLE, availableSlot.getStatus());

        mockMvc.perform(get("/api/patient/appointments/available-slots")
                        .header("Authorization", bearer(patient))
                        .param("date", workDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(8));

        mockMvc.perform(post("/api/patient/appointments")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorWorkSlotId\":\"" + availableSlot.getId() + "\",\"note\":\"First visit\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_STAFF_CONFIRMATION"));

        Appointment appointment = appointmentRepository.findAll().get(0);
        assertEquals(DoctorWorkSlotStatus.SCHEDULING,
                doctorWorkSlotRepository.findById(availableSlot.getId()).orElseThrow().getStatus());

        mockMvc.perform(patch("/api/staff/scheduling/appointments/{appointmentId}/decision", appointment.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        Appointment confirmed = appointmentRepository.findById(appointment.getId()).orElseThrow();
        assertEquals(AppointmentStatus.CONFIRMED, confirmed.getStatus());
        assertEquals(DoctorWorkSlotStatus.BOOKED,
                doctorWorkSlotRepository.findById(availableSlot.getId()).orElseThrow().getStatus());

        mockMvc.perform(get("/api/patient/appointments/available-slots")
                        .header("Authorization", bearer(patient))
                        .param("date", workDate.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(7));
    }

    @Test
    void pendingScheduleBlocksRoomAndDoctorConflicts() throws Exception {
        User firstDoctor = saveActiveDoctor("+84911111111", "Password123!");
        User secondDoctor = saveActiveDoctor("+84922222222", "Password123!");
        ClinicRoom firstRoom = saveRoom("ROOM-101", "Clinic Room 101");
        ClinicRoom secondRoom = saveRoom("ROOM-102", "Clinic Room 102");
        LocalDate workDate = LocalDate.now(HOSPITAL_ZONE).plusDays(3);

        submitSchedule(firstDoctor, workDate, "MORNING", firstRoom.getId(), 201);
        submitSchedule(secondDoctor, workDate, "MORNING", firstRoom.getId(), 409);
        submitSchedule(firstDoctor, workDate, "MORNING", secondRoom.getId(), 409);

        assertEquals(8, doctorWorkSlotRepository.count());
    }

    @Test
    void rejectedWorkScheduleReleasesRoomForAnotherDoctor() throws Exception {
        User firstDoctor = saveActiveDoctor("+84911111111", "Password123!");
        User secondDoctor = saveActiveDoctor("+84922222222", "Password123!");
        User staff = saveActiveStaff("+84933333333", "Password123!");
        ClinicRoom room = saveRoom("ROOM-101", "Clinic Room 101");
        LocalDate workDate = LocalDate.now(HOSPITAL_ZONE).plusDays(4);

        submitSchedule(firstDoctor, workDate, "AFTERNOON", room.getId(), 201);
        String submissionId = doctorWorkSlotRepository.findAll().get(0).getSubmissionId();

        mockMvc.perform(patch("/api/staff/scheduling/work-schedules/{submissionId}/decision", submissionId)
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"rejectionReason\":\"Doctor reassigned\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertTrue(doctorWorkSlotRepository.findAll().stream().noneMatch(DoctorWorkSlot::isConflictActive));
        submitSchedule(secondDoctor, workDate, "AFTERNOON", room.getId(), 201);
        assertEquals(16, doctorWorkSlotRepository.count());
    }

    @Test
    void rejectedAppointmentReleasesSlotAndPatientCannotBookTwicePerDay() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "Password123!");
        User staff = saveActiveStaff("+84922222222", "Password123!");
        User firstPatient = saveActivePatient("+84933333333", "PAT-SCHEDULE-1");
        User secondPatient = saveActivePatient("+84944444444", "PAT-SCHEDULE-2");
        ClinicRoom room = saveRoom("ROOM-101", "Clinic Room 101");
        LocalDate workDate = LocalDate.now(HOSPITAL_ZONE).plusDays(5);

        submitSchedule(doctor, workDate, "MORNING", room.getId(), 201);
        String submissionId = doctorWorkSlotRepository.findAll().get(0).getSubmissionId();
        approveSchedule(staff, submissionId);
        List<DoctorWorkSlot> slots = doctorWorkSlotRepository.findAll().stream()
                .sorted(java.util.Comparator.comparing(DoctorWorkSlot::getSlotId))
                .toList();

        book(firstPatient, slots.get(0).getId(), 201);
        book(firstPatient, slots.get(1).getId(), 409);
        book(secondPatient, slots.get(0).getId(), 409);

        Appointment appointment = appointmentRepository.findAll().get(0);
        mockMvc.perform(patch("/api/staff/scheduling/appointments/{appointmentId}/decision", appointment.getId())
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECT\",\"rejectionReason\":\"Patient information requires review\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        assertFalse(appointmentRepository.findById(appointment.getId()).orElseThrow().isActive());
        assertEquals(DoctorWorkSlotStatus.AVAILABLE,
                doctorWorkSlotRepository.findById(slots.get(0).getId()).orElseThrow().getStatus());
        book(secondPatient, slots.get(0).getId(), 201);
    }

    private ClinicRoom saveRoom(String code, String name) {
        Instant now = Instant.now();
        return clinicRoomRepository.save(ClinicRoom.builder()
                .code(code)
                .name(name)
                .active(true)
                .updatedAt(now)
                .build());
    }

    private String bearer(User user) {
        String token = jwtUtils.generateAccessToken(user);
        try {
            user.setAccessTokenHash(Base64.getUrlEncoder().withoutPadding().encodeToString(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
        userRepository.save(user);
        return "Bearer " + token;
    }

    private String scheduleRequest(LocalDate workDate, String session, String roomId) {
        return "{\"workDate\":\"" + workDate + "\",\"session\":\"" + session
                + "\",\"roomId\":\"" + roomId + "\"}";
    }

    private void submitSchedule(
            User doctor,
            LocalDate workDate,
            String session,
            String roomId,
            int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/doctor/work-schedules")
                        .header("Authorization", bearer(doctor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleRequest(workDate, session, roomId)))
                .andExpect(status().is(expectedStatus));
    }

    private void approveSchedule(User staff, String submissionId) throws Exception {
        mockMvc.perform(patch("/api/staff/scheduling/work-schedules/{submissionId}/decision", submissionId)
                        .header("Authorization", bearer(staff))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isOk());
    }

    private void book(User patient, String doctorWorkSlotId, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/patient/appointments")
                        .header("Authorization", bearer(patient))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"doctorWorkSlotId\":\"" + doctorWorkSlotId + "\"}"))
                .andExpect(status().is(expectedStatus));
    }
}
