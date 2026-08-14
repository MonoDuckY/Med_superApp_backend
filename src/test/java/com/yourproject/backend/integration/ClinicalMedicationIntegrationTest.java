package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.models.OtpPurpose;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.User;

class ClinicalMedicationIntegrationTest extends MongoIntegrationTestBase {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    @Test
    void doctorCompletesExaminationAndPatientManagesGeneratedMedicineSchedule() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.status").value("IN_PROGRESS"));

        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicalHistory":"No known allergy",
                                  "currentSickness":"Persistent cough",
                                  "height":170,
                                  "weight":65,
                                  "bloodType":"O+",
                                  "note":"Initial examination",
                                  "bloodPressure":"120/80",
                                  "heartRate":78,
                                  "breathingRate":18,
                                  "bodyTemperature":37.2,
                                  "bloodLipids":4.5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medicalRecord.bloodPressure").value("120/80"));

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Acute cough\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medicalRecord.diagnosis").value("Acute cough"));

        LocalDate medicineDay = LocalDate.now(VIETNAM_ZONE).plusDays(1);
        Instant firstTime = medicineDay.atTime(8, 0).atZone(VIETNAM_ZONE).toInstant();
        Instant secondTime = medicineDay.atTime(12, 0).atZone(VIETNAM_ZONE).toInstant();
        MvcResult prescriptionResult = mockMvc.perform(post(
                        "/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content":"Cough treatment",
                                  "medicineSchedules":[
                                    {"medicineName":"Cough medicine","dosage":"30ml","scheduledAt":"%s","note":"After meal"},
                                    {"medicineName":"Cough medicine","dosage":"30ml","scheduledAt":"%s","note":"After meal"}
                                  ],
                                  "meals":[{"mealName":"Healthy breakfast","scheduledAt":"%s","note":"Low salt","dishes":[{"dishName":"Oatmeal","quantity":200,"unit":"g","totalCalories":300,"totalProtein":10,"totalCarbohydrates":50,"totalFat":6}]}],
                                  "workouts":[{"workoutName":"Walking","content":"Walk for 20 minutes","scheduledAt":"%s"}]
                                }
                                """.formatted(firstTime, secondTime, firstTime, secondTime)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.medicineSchedules.length()").value(2))
                .andExpect(jsonPath("$.data.meals[0].prescriptionId").isNotEmpty())
                .andExpect(jsonPath("$.data.workouts[0].prescriptionId").isNotEmpty())
                .andReturn();
        String scheduleId = JsonPath.read(
                prescriptionResult.getResponse().getContentAsString(),
                "$.data.medicineSchedules[0].id");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.status").value("COMPLETED"));

        String patientToken = patientAccessToken(patient, "123456");
        mockMvc.perform(get("/api/patient/medicine-schedules")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        Instant changedTime = medicineDay.atTime(18, 0).atZone(VIETNAM_ZONE).toInstant();
        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/time", scheduleId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"" + changedTime + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOT_YET"));

        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/take", scheduleId)
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TAKEN"));

        assertEquals(MedicineScheduleStatus.TAKEN,
                medicineScheduleRepository.findById(scheduleId).orElseThrow().getStatus());
        assertNotNull(medicalRecordRepository.findByAppointmentId(appointment.getId()).orElseThrow());
    }

    @Test
    void anotherDoctorCannotOpenOrModifyAppointment() throws Exception {
        User owner = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User anotherDoctor = saveActiveDoctor("+84933333333", "OtherDoctor1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(owner, patient);
        String token = loginAccessToken("0933333333", "OtherDoctor1!");

        mockMvc.perform(get("/api/doctor/appointments/{id}", appointment.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        assertEquals(AppointmentStatus.CONFIRMED,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
        assertNotNull(anotherDoctor.getId());
    }

    @Test
    void patientCannotMoveMedicineScheduleToAnotherVietnamCalendarDay() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
        Instant originalTime = LocalDate.now(VIETNAM_ZONE).plusDays(2)
                .atTime(8, 0).atZone(VIETNAM_ZONE).toInstant();
        MvcResult result = mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineSchedules\":[{\"medicineName\":\"Medicine A\",\"dosage\":\"1 tablet\",\"scheduledAt\":\"" + originalTime + "\"}]}"))
                .andExpect(status().isCreated()).andReturn();
        String scheduleId = JsonPath.read(result.getResponse().getContentAsString(), "$.data.medicineSchedules[0].id");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"heartRate\":75}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diagnosis\":\"Diagnosis\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
        String patientToken = patientAccessToken(patient, "123456");

        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/time", scheduleId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"" + originalTime.plusSeconds(86400) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Medicine schedule time must remain on the same calendar day."));
    }

    @Test
    void doctorListsDistinctPatientsAndTheirMedicalRecordHistory() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User anotherDoctor = saveActiveDoctor("+84933333333", "OtherDoctor1!");
        User patient = saveActivePatient("+84922222222");
        Appointment firstAppointment = saveConfirmedAppointment(doctor, patient);
        Appointment secondAppointment = saveConfirmedAppointment(anotherDoctor, patient);
        medicalRecordRepository.save(com.yourproject.backend.models.MedicalRecord.builder()
                .appointmentId(firstAppointment.getId()).diagnosis("Historical diagnosis").build());
        medicalRecordRepository.save(com.yourproject.backend.models.MedicalRecord.builder()
                .appointmentId(secondAppointment.getId()).diagnosis("Diagnosis by another doctor").build());
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments/patients")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(patient.getId()))
                .andExpect(jsonPath("$.data[0].phoneNumber").value("+84922222222"));

        mockMvc.perform(get("/api/doctor/appointments/patients/{patientId}/medical-records", patient.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].medicalRecord.diagnosis")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "Historical diagnosis", "Diagnosis by another doctor")));
    }

    @Test
    void patientCreatesAndCompletesIndependentMealAndWorkoutPlans() throws Exception {
        User patient = saveActivePatient("+84922222222");
        String patientToken = patientAccessToken(patient, "123456");
        Instant mealTime = Instant.now().minusSeconds(60);
        Instant workoutTime = Instant.now().minusSeconds(30);

        mockMvc.perform(post("/api/patient/meal-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\":\"Patient meal\",\"scheduledAt\":\"" + mealTime
                                + "\",\"dishes\":[{\"dishName\":\"Rice\",\"quantity\":1,\"unit\":\"bowl\",\"totalCalories\":250}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.prescriptionId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.dishes[0].dishName").value("Rice"));

        mockMvc.perform(post("/api/patient/workout-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workoutName\":\"Patient workout\",\"content\":\"Stretch\",\"scheduledAt\":\""
                                + workoutTime + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.prescriptionId").doesNotExist())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/patient/meal-plans").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
        mockMvc.perform(get("/api/patient/workout-plans").header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    void appointmentHasOnePrescriptionAndDoctorUpdatesItWithoutPrescriptionId() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isOk());
        Instant scheduledAt = Instant.now().plusSeconds(3600);
        String createBody = "{\"content\":\"Initial prescription\",\"medicineSchedules\":["
                + "{\"medicineName\":\"Medicine A\",\"dosage\":\"1 tablet\",\"scheduledAt\":\""
                + scheduledAt + "\"}]}";

        MvcResult created = mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andReturn();
        String prescriptionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This appointment already has a prescription."));

        mockMvc.perform(patch("/api/doctor/appointments/{id}/prescription", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Updated prescription\",\"medicineSchedules\":["
                                + "{\"medicineName\":\"Medicine B\",\"dosage\":\"2 tablets\",\"scheduledAt\":\""
                                + scheduledAt.plusSeconds(60) + "\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(prescriptionId))
                .andExpect(jsonPath("$.data.content").value("Updated prescription"))
                .andExpect(jsonPath("$.data.medicineSchedules[0].medicineName").value("Medicine B"));
    }

    @Test
    void patientCannotCreateMealOrWorkoutInFutureOrOlderThanTwoDays() throws Exception {
        User patient = saveActivePatient("+84922222222");
        String patientToken = patientAccessToken(patient, "123456");
        Instant tomorrow = LocalDate.now(VIETNAM_ZONE).plusDays(1)
                .atTime(12, 0).atZone(VIETNAM_ZONE).toInstant();
        Instant threeDaysAgo = LocalDate.now(VIETNAM_ZONE).minusDays(3)
                .atTime(12, 0).atZone(VIETNAM_ZONE).toInstant();

        mockMvc.perform(post("/api/patient/meal-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\":\"Future meal\",\"scheduledAt\":\"" + tomorrow
                                + "\",\"dishes\":[{\"dishName\":\"Rice\",\"quantity\":1,\"unit\":\"bowl\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Activity time cannot be in the future."));

        mockMvc.perform(post("/api/patient/meal-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"mealName\":\"Old meal\",\"scheduledAt\":\"" + threeDaysAgo
                                + "\",\"dishes\":[{\"dishName\":\"Rice\",\"quantity\":1,\"unit\":\"bowl\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Patient-created activity must be for today or up to 2 previous days."));

        mockMvc.perform(post("/api/patient/workout-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workoutName\":\"Future workout\",\"scheduledAt\":\"" + tomorrow + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Activity time cannot be in the future."));

        mockMvc.perform(post("/api/patient/workout-plans")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workoutName\":\"Old workout\",\"scheduledAt\":\"" + threeDaysAgo + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Patient-created activity must be for today or up to 2 previous days."));
    }

    private Appointment saveConfirmedAppointment(User doctor, User patient) {
        DoctorWorkSlot slot = doctorWorkSlotRepository.save(DoctorWorkSlot.builder()
                .doctorId(doctor.getId())
                .workDate(LocalDate.now())
                .slotId("clinical-test-slot")
                .roomId("clinical-test-room")
                .status(DoctorWorkSlotStatus.BOOKED)
                .submittedAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        return appointmentRepository.save(Appointment.builder()
                .patientId(patient.getId())
                .doctorWorkSlotId(slot.getId())
                .status(AppointmentStatus.CONFIRMED)
                .requestedAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private String loginAccessToken(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"role\":\"DOCTOR\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }

    private String patientAccessToken(User patient, String code) throws Exception {
        patientOtpRepository.save(PatientOtp.builder()
                .userId(patient.getId())
                .phoneLookup(patient.getPhoneLookup())
                .purpose(OtpPurpose.PATIENT_LOGIN)
                .codeHash(patientDataProtectionService.secureLookup("otp:" + patient.getId() + ":" + code))
                .attempts(0)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .build());
        MvcResult result = mockMvc.perform(post("/api/auth/patient-otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"0922222222\",\"code\":\"" + code + "\",\"deviceId\":\"clinical-test-device\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
