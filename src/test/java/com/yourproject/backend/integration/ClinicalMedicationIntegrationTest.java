package com.yourproject.backend.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import com.jayway.jsonpath.JsonPath;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.MedicalRecord;
import com.yourproject.backend.models.MedicineScheduleStatus;
import com.yourproject.backend.models.OtpPurpose;
import com.yourproject.backend.models.PatientOtp;
import com.yourproject.backend.models.User;
import com.yourproject.backend.services.S3StorageService;
import com.yourproject.backend.services.S3StorageService.PresignedObjectUrl;

class ClinicalMedicationIntegrationTest extends MongoIntegrationTestBase {
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockitoBean
    private S3StorageService s3StorageService;

    // =========================================================================
    // TC-INT-ClinicalMed-001
    // GET /api/doctor/appointments – get all appointments (no filter)
    // =========================================================================

    @Test
    void getDoctorAppointments_returnsAllAppointmentsForDoctor() throws Exception {
        // TC-INT-ClinicalMed-001
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments")
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-002
    // GET /api/doctor/appointments?status=CONFIRMED – filter by status
    // =========================================================================

    @Test
    void getDoctorAppointments_filtersAppointmentsByStatus() throws Exception {
        // TC-INT-ClinicalMed-002
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments")
                        .param("status", "CONFIRMED")
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));

        mockMvc.perform(get("/api/doctor/appointments")
                        .param("status", "COMPLETED")
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-003
    // GET /api/doctor/appointments – non-DOCTOR caller → 403
    // =========================================================================

    @Test
    void getDoctorAppointments_returnsForbiddenForNonDoctor() throws Exception {
        // TC-INT-ClinicalMed-003
        User patient = saveActivePatient("+84922222222");
        String patientToken = patientAccessToken(patient, "123456");

        mockMvc.perform(get("/api/doctor/appointments")
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // TC-INT-ClinicalMed-004
    // GET /api/doctor/appointments/{appointmentId} – valid request
    // =========================================================================

    @Test
    void getDoctorExamination_returnsExaminationDetails() throws Exception {
        // TC-INT-ClinicalMed-004
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments/{id}", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointment.id").value(appointment.getId()))
                .andExpect(jsonPath("$.data.appointment.status").value("CONFIRMED"));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-005
    // GET /api/doctor/appointments/{appointmentId} – non-existent id → 404
    // =========================================================================

    @Test
    void getDoctorExamination_returnsNotFoundForInvalidId() throws Exception {
        // TC-INT-ClinicalMed-005
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments/{id}", "non-existent-id-000")
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-006
    // GET /api/doctor/appointments/{appointmentId} – another doctor → 403
    // =========================================================================

    @Test
    void getDoctorExamination_returnsForbiddenForOtherDoctorAppointment() throws Exception {
        // TC-INT-ClinicalMed-006
        User owner = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User anotherDoctor = saveActiveDoctor("+84933333333", "OtherDoctor1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(owner, patient);
        String anotherToken = loginAccessToken("0933333333", "OtherDoctor1!");

        mockMvc.perform(get("/api/doctor/appointments/{id}", appointment.getId())
                        .header("Authorization", "Bearer " + anotherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Doctors can only access their own appointments."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-007
    // PATCH /api/doctor/appointments/{appointmentId}/start – success
    // =========================================================================

    @Test
    void startExamination_transitionsStatusToInProgress() throws Exception {
        // TC-INT-ClinicalMed-007
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointment.status").value("IN_PROGRESS"));

        assertEquals(AppointmentStatus.IN_PROGRESS,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    // =========================================================================
    // TC-INT-ClinicalMed-008
    // PATCH .../start when already IN_PROGRESS → 409
    // =========================================================================

    @Test
    void startExamination_returnsConflictWhenNotConfirmed() throws Exception {
        // TC-INT-ClinicalMed-008
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        // First start – should succeed
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Second start – should conflict
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only confirmed appointments can be started."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-009
    // PATCH .../clinical-information when IN_PROGRESS – success
    // =========================================================================

    @Test
    void updateClinicalInformation_updatesPatientAndMedicalRecord() throws Exception {
        // TC-INT-ClinicalMed-009
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "medicalHistory": "No known allergy",
                                  "currentSickness": "Persistent cough",
                                  "height": 170,
                                  "weight": 65,
                                  "bloodType": "O+",
                                  "note": "Initial examination",
                                  "bloodPressure": "120/80",
                                  "heartRate": 78,
                                  "breathingRate": 18,
                                  "bodyTemperature": 37.2,
                                  "bloodLipids": 4.5
                                }
                                """))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.medicalRecord.bloodPressure").value("120/080"))
                .andExpect(jsonPath("$.data.medicalRecord.heartRate").value(78));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-010
    // PATCH .../clinical-information when NOT IN_PROGRESS → 409
    // =========================================================================

    @Test
    void updateClinicalInformation_returnsConflictWhenNotInProgress() throws Exception {
        // TC-INT-ClinicalMed-010
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        // Appointment is CONFIRMED (not yet started)
        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\": 80}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Examination information can only be changed while the appointment is in progress."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-011
    // PATCH .../diagnosis when IN_PROGRESS – success
    // =========================================================================

    @Test
    void updateDiagnosis_updatesMedicalRecord() throws Exception {
        // TC-INT-ClinicalMed-011
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"Acute pneumonia\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.medicalRecord.diagnosis").value("Acute pneumonia"));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-012
    // PATCH .../diagnosis – blank body → 400
    // =========================================================================

    @Test
    void updateDiagnosis_returnsBadRequestForBlankDiagnosis() throws Exception {
        // TC-INT-ClinicalMed-012
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-013
    // POST .../prescriptions – valid prescription → 201
    // =========================================================================

    @Test
    void createPrescription_createsNewPrescriptionWithSchedules() throws Exception {
        // TC-INT-ClinicalMed-013
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Instant futureTime = Instant.now().plusSeconds(3600);
        mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Take after meal\","
                                + "\"medicineSchedules\":[{\"medicineName\":\"Paracetamol\","
                                + "\"dosage\":\"500mg\",\"scheduledAt\":\"" + futureTime + "\"}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.medicineSchedules.length()").value(1))
                .andExpect(jsonPath("$.data.medicineSchedules[0].status").value("NOT_YET"));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-014
    // POST .../prescriptions – duplicate schedules → 409
    // =========================================================================

    @Test
    void createPrescription_returnsConflictForDuplicateSchedules() throws Exception {
        // TC-INT-ClinicalMed-014
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Instant futureTime = Instant.now().plusSeconds(3600);
        String sameSchedule = "{\"medicineName\":\"Paracetamol\",\"dosage\":\"500mg\","
                + "\"scheduledAt\":\"" + futureTime + "\"}";

        mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineSchedules\":[" + sameSchedule + "," + sameSchedule + "]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Duplicate medicine schedules are not allowed at the same time."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-015
    // POST .../prescriptions – past scheduledAt → 400
    // =========================================================================

    @Test
    void createPrescription_returnsBadRequestForPastScheduleTime() throws Exception {
        // TC-INT-ClinicalMed-015
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineSchedules\":[{\"medicineName\":\"Paracetamol\","
                                + "\"dosage\":\"500mg\",\"scheduledAt\":\"2020-01-01T00:00:00Z\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Medicine schedule time must be in the future."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-016
    // PATCH .../prescription – update replaces old schedules
    // =========================================================================

    @Test
    void updatePrescription_replacesOldSchedulesWithNewOnes() throws Exception {
        // TC-INT-ClinicalMed-016
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Instant futureTime = Instant.now().plusSeconds(3600);
        MvcResult created = mockMvc.perform(
                        post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                                .header("Authorization", "Bearer " + doctorToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"content\":\"Take after meal\","
                                        + "\"medicineSchedules\":[{\"medicineName\":\"Paracetamol\","
                                        + "\"dosage\":\"500mg\",\"scheduledAt\":\"" + futureTime + "\"}]}"))
                .andExpect(status().isCreated())
                .andReturn();
        String prescriptionId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        Instant newTime = futureTime.plusSeconds(3600);
        mockMvc.perform(patch("/api/doctor/appointments/{id}/prescription", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Take before meal\","
                                + "\"medicineSchedules\":[{\"medicineName\":\"Ibuprofen\","
                                + "\"dosage\":\"200mg\",\"scheduledAt\":\"" + newTime + "\"}]}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(prescriptionId))
                .andExpect(jsonPath("$.data.content").value("Take before meal"))
                .andExpect(jsonPath("$.data.medicineSchedules.length()").value(1))
                .andExpect(jsonPath("$.data.medicineSchedules[0].medicineName").value("Ibuprofen"));

        assertEquals(1, medicineScheduleRepository.findAll().size());
    }

    // =========================================================================
    // TC-INT-ClinicalMed-017
    // PATCH .../prescription – no existing prescription → 404
    // (Design scenario: prescription mismatch; actual API finds prescription by
    //  medical record, so "not belonging" manifests as 404 when none exists)
    // =========================================================================

    @Test
    void updatePrescription_returnsForbiddenForMismatchedAppointment() throws Exception {
        // TC-INT-ClinicalMed-017
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // No prescription created – PATCH /prescription should return 404
        Instant futureTime = Instant.now().plusSeconds(3600);
        mockMvc.perform(patch("/api/doctor/appointments/{id}/prescription", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineSchedules\":[{\"medicineName\":\"Ibuprofen\","
                                + "\"dosage\":\"200mg\",\"scheduledAt\":\"" + futureTime + "\"}]}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-018
    // PATCH .../complete – all conditions met → 200
    // =========================================================================

    @Test
    void completeExamination_transitionsToCompleted() throws Exception {
        // TC-INT-ClinicalMed-018
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\": 75, \"bloodPressure\": \"120/80\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"Hypertension\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        Instant futureTime = Instant.now().plusSeconds(3600);
        mockMvc.perform(post("/api/doctor/appointments/{id}/prescriptions", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"medicineSchedules\":[{\"medicineName\":\"Amlodipine\","
                                + "\"dosage\":\"5mg\",\"scheduledAt\":\"" + futureTime + "\"}]}"))
                .andExpect(status().isCreated());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appointment.status").value("COMPLETED"));

        assertEquals(AppointmentStatus.COMPLETED,
                appointmentRepository.findById(appointment.getId()).orElseThrow().getStatus());
    }

    // =========================================================================
    // TC-INT-ClinicalMed-019
    // PATCH .../complete – no MedicalRecord → 400
    // =========================================================================

    @Test
    void completeExamination_returnsBadRequestWhenNoMedicalRecord() throws Exception {
        // TC-INT-ClinicalMed-019
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Immediately complete without any clinical data
        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Medical record is required before completing an examination."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-020
    // PATCH .../complete – MedicalRecord exists but no diagnosis → 400
    // =========================================================================

    @Test
    void completeExamination_returnsBadRequestWhenNoDiagnosis() throws Exception {
        // TC-INT-ClinicalMed-020
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Update vital signs only – skip diagnosis
        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\": 80}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Diagnosis is required before completing an examination."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-021
    // PATCH .../complete – diagnosis exists but no vital signs → 400
    // =========================================================================

    @Test
    void completeExamination_returnsBadRequestWhenNoVitalSigns() throws Exception {
        // TC-INT-ClinicalMed-021
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Update diagnosis only – skip vital signs
        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"Flu\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("Clinical vital signs are required before completing an examination."));
    }

    // =========================================================================
    // TC-INT-ClinicalMed-022
    // PATCH .../complete – diagnosis + vital signs exist but no prescription → 400
    // =========================================================================

    @Test
    void completeExamination_returnsBadRequestWhenNoPrescription() throws Exception {
        // TC-INT-ClinicalMed-022
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Provide vital signs + diagnosis – skip prescription
        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heartRate\": 78}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\": \"Common cold\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message")
                        .value("At least one prescription is required before completing an examination."));
    }

    // =========================================================================
    // Existing flow tests (TC-023 + TC-024 coverage, image tests, etc.)
    // =========================================================================

    // TC-INT-ClinicalMed-026
    @Test
    void doctorUploadsReadsAndDeletesMedicalImageDuringExamination() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        when(s3StorageService.uploadMedicalImage(anyString(), any(MultipartFile.class)))
                .thenAnswer(invocation -> "medical-images/" + invocation.getArgument(0) + "/image-1.jpg");
        when(s3StorageService.createPresignedGetUrl(anyString()))
                .thenReturn(new PresignedObjectUrl(
                        "https://signed.example/medical-image.jpg",
                        Instant.now().plusSeconds(600)));
        MockMultipartFile image = new MockMultipartFile(
                "image", "scan.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] { 1, 2, 3 });

        mockMvc.perform(multipart("/api/doctor/appointments/{id}/medical-images", appointment.getId())
                        .file(image)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].imageId").value("image-1.jpg"))
                .andExpect(jsonPath("$.data[0].url").value("https://signed.example/medical-image.jpg"));

        var medicalRecord = medicalRecordRepository.findByAppointmentId(appointment.getId()).orElseThrow();
        String objectKey = medicalRecord.getMedicalImages().get(0);
        assertEquals("medical-images/" + medicalRecord.getId() + "/image-1.jpg", objectKey);

        mockMvc.perform(get("/api/doctor/appointments/{id}", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medicalRecord.medicalImages[0].imageId").value("image-1.jpg"));

        mockMvc.perform(delete(
                        "/api/doctor/appointments/{id}/medical-images/{imageId}",
                        appointment.getId(),
                        "image-1.jpg")
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        verify(s3StorageService).deleteObject(objectKey);
        assertEquals(0, medicalRecordRepository.findById(medicalRecord.getId())
                .orElseThrow().getMedicalImages().size());
    }

    // TC-INT-ClinicalMed-027
    @Test
    void doctorCannotUploadMedicalImageBeforeExaminationStarts() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        MockMultipartFile image = new MockMultipartFile(
                "image", "scan.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[] { 1 });

        mockMvc.perform(multipart("/api/doctor/appointments/{id}/medical-images", appointment.getId())
                        .file(image)
                        .header("Authorization", "Bearer " + doctorToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Examination information can only be changed while the appointment is in progress."));
    }

    // TC-INT-ClinicalMed-028
    @Test
    void doctorCompletesExaminationAndPatientManagesGeneratedMedicineSchedule() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.medicalRecord.bloodPressure").value("120/080"));

        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"diagnosis\":\"Acute cough\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.appointment.status").value("COMPLETED"));

        String patientToken = patientAccessToken(patient, "123456");
        mockMvc.perform(get("/api/patient/medicine-schedules")
                        .header("Authorization", "Bearer " + patientToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));

        Instant changedTime = medicineDay.atTime(18, 0).atZone(VIETNAM_ZONE).toInstant();
        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/time", scheduleId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"" + changedTime + "\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NOT_YET"));

        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/take", scheduleId)
                        .header("Authorization", "Bearer " + patientToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TAKEN"));

        assertEquals(MedicineScheduleStatus.TAKEN,
                medicineScheduleRepository.findById(scheduleId).orElseThrow().getStatus());
        assertNotNull(medicalRecordRepository.findByAppointmentId(appointment.getId()).orElseThrow());
    }

    // TC-INT-ClinicalMed-029
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

    // TC-INT-ClinicalMed-030
    @Test
    void doctorCannotStoreBloodPressureWithInvalidFormat() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/doctor/appointments/{id}/clinical-information", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bloodPressure\":\"120-80\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Blood pressure must use the format xxx/xxx with digits only, for example 120/80."));
    }

    // TC-INT-ClinicalMed-025
    @Test
    void updateMedicineScheduleTime_returnsBadRequestForDifferentCalendarDay() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/doctor/appointments/{id}/diagnosis", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"diagnosis\":\"Diagnosis\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/doctor/appointments/{id}/complete", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());
        String patientToken = patientAccessToken(patient, "123456");

        mockMvc.perform(patch("/api/patient/medicine-schedules/{id}/time", scheduleId)
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"scheduledAt\":\"" + originalTime.plusSeconds(86400) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Medicine schedule time must remain on the same calendar day."));
    }

    // TC-INT-ClinicalMed-023
    @Test
    void getDoctorPatients_returnsDistinctPatientsForDoctor() throws Exception {
        // TC-INT-ClinicalMed-023 + TC-INT-ClinicalMed-024
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User anotherDoctor = saveActiveDoctor("+84933333333", "OtherDoctor1!");
        User patient = saveActivePatient("+84922222222");
        Appointment firstAppointment = saveConfirmedAppointment(doctor, patient);
        Appointment secondAppointment = saveConfirmedAppointment(anotherDoctor, patient);
        medicalRecordRepository.save(MedicalRecord.builder()
                .appointmentId(firstAppointment.getId()).diagnosis("Historical diagnosis").build());
        medicalRecordRepository.save(MedicalRecord.builder()
                .appointmentId(secondAppointment.getId()).diagnosis("Diagnosis by another doctor").build());
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");

        mockMvc.perform(get("/api/doctor/appointments/patients")
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(patient.getId()))
                .andExpect(jsonPath("$.data[0].phoneNumber").value("+84922222222"));

        mockMvc.perform(get("/api/doctor/appointments/patients/{patientId}/medical-records", patient.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].medicalRecord.diagnosis")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                "Historical diagnosis", "Diagnosis by another doctor")));
    }

    // TC-INT-ClinicalMed-024
    @Test
    void getPatientMedicalRecordHistory_returnsMedicalRecordHistory() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        medicalRecordRepository.save(MedicalRecord.builder()
                .appointmentId(appointment.getId())
                .diagnosis("Patient diagnosis")
                .bloodPressure("120/080")
                .build());
        String patientToken = patientAccessToken(patient, "123456");

        mockMvc.perform(get("/api/patient/medical-records")
                        .header("Authorization", "Bearer " + patientToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].appointment.id").value(appointment.getId()))
                .andExpect(jsonPath("$.data[0].medicalRecord.diagnosis").value("Patient diagnosis"))
                .andExpect(jsonPath("$.data[0].medicalRecord.bloodPressure").value("120/080"));
    }

    // TC-INT-ClinicalMed-031
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
        mockMvc.perform(get("/api/patient/workout-plans").header("Authorization", "Bearer " + patientToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    // TC-INT-ClinicalMed-032
    @Test
    void appointmentHasOnePrescriptionAndDoctorUpdatesItWithoutPrescriptionId() throws Exception {
        User doctor = saveActiveDoctor("+84911111111", "DoctorPassword1!");
        User patient = saveActivePatient("+84922222222");
        Appointment appointment = saveConfirmedAppointment(doctor, patient);
        String doctorToken = loginAccessToken("0911111111", "DoctorPassword1!");
        mockMvc.perform(patch("/api/doctor/appointments/{id}/start", appointment.getId())
                        .header("Authorization", "Bearer " + doctorToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(prescriptionId))
                .andExpect(jsonPath("$.data.content").value("Updated prescription"))
                .andExpect(jsonPath("$.data.medicineSchedules[0].medicineName").value("Medicine B"));
    }

    // TC-INT-ClinicalMed-033
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

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Creates a CONFIRMED appointment with a BOOKED DoctorWorkSlot.
     *
     * <p>We insert the DoctorWorkSlot via MongoTemplate using a raw BSON Document so
     * that {@code workDate} is stored as a proper BSON Date.  Spring Data MongoDB has no
     * built-in LocalDate↔Date converter, which means that if the slot is saved through
     * the repository the field round-trips as {@code null} when the service later reads
     * and re-saves the entity, causing a MongoDB schema-validation failure (error 121).
     */
    private Appointment saveConfirmedAppointment(User doctor, User patient) {
        DoctorWorkSlot slot = DoctorWorkSlot.builder()
                .doctorId(doctor.getId())
                .workDate(LocalDate.now())
                .slotId(new org.bson.types.ObjectId().toHexString())
                .roomId(new org.bson.types.ObjectId().toHexString())
                .status(DoctorWorkSlotStatus.BOOKED)
                .conflictActive(false)
                .submittedAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        
        Document slotDoc = new Document();
        mongoTemplate.getConverter().write(slot, slotDoc);
        
        // Compute workDate as a java.util.Date (= BSON Date) so it survives the
        // read-back inside startExamination without being nulled out.
        Date workDate = Date.from(LocalDate.now(VIETNAM_ZONE).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
        slotDoc.put("workDate", workDate);
        slotDoc.put("version", 0L);
        
        mongoTemplate.getCollection("doctor_work_slots").insertOne(slotDoc);
        String slotId = slotDoc.getObjectId("_id").toHexString();

        return appointmentRepository.save(Appointment.builder()
                .patientId(patient.getId())
                .doctorWorkSlotId(slotId)
                .status(AppointmentStatus.CONFIRMED)
                .requestedAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private String loginAccessToken(String phoneNumber, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phoneNumber\":\"" + phoneNumber + "\",\"role\":\"DOCTOR\",\"password\":\"" + password + "\"}"))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
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
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
    }
}







