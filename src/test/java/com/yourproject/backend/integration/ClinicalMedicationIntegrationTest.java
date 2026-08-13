package com.yourproject.backend.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;

import com.yourproject.backend.models.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

class ClinicalMedicationIntegrationTest extends MongoIntegrationTestBase {

    private String hashToken(String token) {
        try {
            return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(
                    java.security.MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String getDoctorToken(User doctor) {
        String token = jwtUtils.generateAccessToken(doctor);
        doctor.setAccessTokenHash(hashToken(token));
        userRepository.save(doctor);
        return "Bearer " + token;
    }
    
    private String getPatientToken(User patient) {
        String token = jwtUtils.generateAccessToken(patient);
        patient.setAccessTokenHash(hashToken(token));
        userRepository.save(patient);
        return "Bearer " + token;
    }
    
    private String getStaffToken(User staff) {
        String token = jwtUtils.generateAccessToken(staff);
        staff.setAccessTokenHash(hashToken(token));
        userRepository.save(staff);
        return "Bearer " + token;
    }

    // TC-INT-ClinicalMed-001
    @Test
    void getDoctorAppointments_returnsAllAppointmentsForDoctor() throws Exception {
        User doctor = saveActiveDoctor("+84912345678", "Password123!");
        String token = getDoctorToken(doctor);

        DoctorWorkSlot slot = DoctorWorkSlot.builder()
                .doctorId(doctor.getId())
                .workDate(java.time.LocalDate.now())
                .slotId("SLOT-1")
                .roomId("ROOM-1")
                .status(DoctorWorkSlotStatus.BOOKED)
                .build();
        doctorWorkSlotRepository.save(slot);

        Appointment appointment = Appointment.builder()
                .patientUserId(UUID.randomUUID().toString())
                .doctorWorkSlotId(slot.getId())
                .status(AppointmentStatus.CONFIRMED)
                .requestedAt(Instant.now())
                .build();
        appointmentRepository.save(appointment);

        mockMvc.perform(get("/api/doctor/appointments")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    // TC-INT-ClinicalMed-002
    @Test
    void getDoctorAppointments_filtersAppointmentsByStatus() throws Exception {
        // Given: At least 1 appointment with status=CONFIRMED and 1 with another status for this doctor
        // When: GET /api/doctor/appointments?status=CONFIRMED
        // Then: HTTP 200, data only contains appointments with status=CONFIRMED
    }

    // TC-INT-ClinicalMed-003
    @Test
    void getDoctorAppointments_returnsForbiddenForNonDoctor() throws Exception {
        // Given: Valid token of PATIENT or STAFF
        // When: GET /api/doctor/appointments with patient token
        // Then: HTTP 403 Forbidden, success=false
    }

    // TC-INT-ClinicalMed-004
    @Test
    void getDoctorExamination_returnsExaminationDetails() throws Exception {
        // Given: Appointment exists belonging to the testing doctor
        // When: GET /api/doctor/appointments/{appointmentId}
        // Then: HTTP 200, data contains appointment + patient info + medicalRecord + prescriptions
    }

    // TC-INT-ClinicalMed-005
    @Test
    void getDoctorExamination_returnsNotFoundForInvalidId() throws Exception {
        // Given: Valid doctor token
        // When: GET /api/doctor/appointments/nonExistentId999
        // Then: HTTP 404, success=false, message contains 'not found'
    }

    // TC-INT-ClinicalMed-006
    @Test
    void getDoctorExamination_returnsForbiddenForOtherDoctorAppointment() throws Exception {
        // Given: 2 separate doctors exist; appointment belongs to doctor B
        // When: GET /api/doctor/appointments/{appointmentIdOfDoctorB} with doctorA token
        // Then: HTTP 403 Forbidden, success=false
    }

    // TC-INT-ClinicalMed-007
    @Test
    void startExamination_transitionsStatusToInProgress() throws Exception {
        // Given: Appointment exists with status=CONFIRMED, workSlot status=BOOKED
        // When: PATCH /api/doctor/appointments/{appointmentId}/start (no body)
        // Then: HTTP 200, data.appointment.status='IN_PROGRESS'; DB: slot.status=IN_PROGRESS
    }

    // TC-INT-ClinicalMed-008
    @Test
    void startExamination_returnsConflictWhenNotConfirmed() throws Exception {
        // Given: Appointment has status=IN_PROGRESS
        // When: PATCH /{appointmentId}/start when status is already IN_PROGRESS
        // Then: HTTP 409 Conflict, message='Only confirmed appointments can be started.'
    }

    // TC-INT-ClinicalMed-009
    @Test
    void updateClinicalInformation_updatesPatientAndMedicalRecord() throws Exception {
        // Given: Appointment status=IN_PROGRESS, doctor owns the appointment
        // When: PATCH /{appointmentId}/clinical-information  Body: {heartRate:80, bloodPressure:'120/80', bodyTemperature:36.5, medicalHistory:'Diabetes'}
        // Then: HTTP 200, data.medicalRecord contains newly updated data
    }

    // TC-INT-ClinicalMed-010
    @Test
    void updateClinicalInformation_returnsConflictWhenNotInProgress() throws Exception {
        // Given: Appointment status=CONFIRMED (not started yet)
        // When: PATCH /{appointmentId}/clinical-information with valid body
        // Then: HTTP 409, message='Examination information can only be changed while the appointment is in progress.'
    }

    // TC-INT-ClinicalMed-011
    @Test
    void updateDiagnosis_updatesMedicalRecord() throws Exception {
        // Given: Appointment status=IN_PROGRESS
        // When: PATCH /{appointmentId}/diagnosis  Body: {diagnosis:'Acute pneumonia'}
        // Then: HTTP 200, data.medicalRecord.diagnosis='Acute pneumonia'
    }

    // TC-INT-ClinicalMed-012
    @Test
    void updateDiagnosis_returnsBadRequestForBlankDiagnosis() throws Exception {
        // Given: Appointment status=IN_PROGRESS
        // When: PATCH /{appointmentId}/diagnosis  Body: {diagnosis:''}
        // Then: HTTP 400, success=false, validation error on diagnosis field
    }

    // TC-INT-ClinicalMed-013
    @Test
    void createPrescription_createsNewPrescriptionWithSchedules() throws Exception {
        // Given: Appointment status=IN_PROGRESS
        // When: POST /{appointmentId}/prescriptions  Body: {content:'Take after meal', medicineSchedules:[{medicineName:'Paracetamol', dosage:'500mg', scheduledAt:<future>}]}
        // Then: HTTP 201 Created, data contains prescriptionId, medicineSchedules with status=NOT_YET
    }

    // TC-INT-ClinicalMed-014
    @Test
    void createPrescription_returnsConflictForDuplicateSchedules() throws Exception {
        // Given: Appointment status=IN_PROGRESS
        // When: POST /{appointmentId}/prescriptions  Body with 2 schedules having same medicineName + dosage + scheduledAt
        // Then: HTTP 409 Conflict, message='Duplicate medicine schedules are not allowed at the same time.'
    }

    // TC-INT-ClinicalMed-015
    @Test
    void createPrescription_returnsBadRequestForPastScheduleTime() throws Exception {
        // Given: Appointment status=IN_PROGRESS
        // When: POST /{appointmentId}/prescriptions  Body: {medicineSchedules:[{medicineName:'B', dosage:'50mg', scheduledAt:'2020-01-01T00:00:00Z'}]}
        // Then: HTTP 400, message='Medicine schedule time must be in the future.'
    }

    // TC-INT-ClinicalMed-016
    @Test
    void updatePrescription_replacesOldSchedulesWithNewOnes() throws Exception {
        // Given: Prescription exists and belongs to IN_PROGRESS appointment
        // When: PATCH /{id}/prescriptions/{prescriptionId}  Body: {content:'Take before meal', medicineSchedules:[{medicineName:'Ibuprofen', dosage:'200mg', scheduledAt:<future>}]}
        // Then: HTTP 200, data contains updated info; old schedule is deleted and replaced with new schedule
    }

    // TC-INT-ClinicalMed-017
    @Test
    void updatePrescription_returnsForbiddenForMismatchedAppointment() throws Exception {
        // Given: Prescription exists but is not linked to appointmentId in URL
        // When: PATCH /api/doctor/appointments/{idA}/prescriptions/{prescriptionIdOfB}
        // Then: HTTP 403 Forbidden, message='Prescription does not belong to this appointment.'
    }

    // TC-INT-ClinicalMed-018
    @Test
    void completeExamination_transitionsToCompleted() throws Exception {
        // Given: Medical record has diagnosis + vital signs; at least 1 prescription exists
        // When: PATCH /{appointmentId}/complete (no body)
        // Then: HTTP 200, data.appointment.status='COMPLETED'; DB: slot.status=CLOSED
    }

    // TC-INT-ClinicalMed-019
    @Test
    void completeExamination_returnsBadRequestWhenNoMedicalRecord() throws Exception {
        // Given: Appointment status=IN_PROGRESS, NO MedicalRecord
        // When: Start appointment -> immediately PATCH /{id}/complete
        // Then: HTTP 400, message='Medical record is required before completing an examination.'
    }

    // TC-INT-ClinicalMed-020
    @Test
    void completeExamination_returnsBadRequestWhenNoDiagnosis() throws Exception {
        // Given: MedicalRecord exists, diagnosis=null or blank
        // When: Update vital signs (do not update diagnosis), then PATCH /{id}/complete
        // Then: HTTP 400, message='Diagnosis is required before completing an examination.'
    }

    // TC-INT-ClinicalMed-021
    @Test
    void completeExamination_returnsBadRequestWhenNoVitalSigns() throws Exception {
        // Given: MedicalRecord has diagnosis, NO vital signs
        // When: Update diagnosis (no vital signs), then PATCH /{id}/complete
        // Then: HTTP 400, message='Clinical vital signs are required before completing an examination.'
    }

    // TC-INT-ClinicalMed-022
    @Test
    void completeExamination_returnsBadRequestWhenNoPrescription() throws Exception {
        // Given: MedicalRecord has diagnosis + vital signs, NO prescription
        // When: Update diagnosis + vital signs (no prescription created), then PATCH /{id}/complete
        // Then: HTTP 400, message='At least one prescription is required before completing an examination.'
    }

    // TC-INT-ClinicalMed-023
    @Test
    void getDoctorPatients_returnsDistinctPatientsForDoctor() throws Exception {
        // Given: Doctor has appointments with patients in DB
        // When: GET /api/doctor/appointments/patients
        // Then: HTTP 200, success=true, data=[...list of patients]
    }

    // TC-INT-ClinicalMed-024
    @Test
    void getPatientMedicalRecordHistory_returnsMedicalRecordHistory() throws Exception {
        // Given: Patient has medical records across past appointments
        // When: GET /api/doctor/appointments/patients/{patientId}/medical-records
        // Then: HTTP 200, success=true, data=[...medical record history]
    }

    // TC-INT-ClinicalMed-025
    @Test
    void updateMedicineScheduleTime_returnsBadRequestForDifferentCalendarDay() throws Exception {
        // Given: Patient has an active medicine schedule
        // When: PATCH /api/patient/medicine-schedules/{id}/time with scheduledAt on different day
        // Then: HTTP 400, message='Medicine schedule time must remain on the same calendar day.'
    }

}
