package com.yourproject.backend.services;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.yourproject.backend.dtos.requests.UpdateClinicalInformationRequest;
import com.yourproject.backend.dtos.requests.UpdateDiagnosisRequest;
import com.yourproject.backend.dtos.requests.UpdateMedicineScheduleTimeRequest;
import com.yourproject.backend.dtos.requests.UpsertPrescriptionRequest;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.DoctorExaminationResponse;
import com.yourproject.backend.dtos.responses.MedicineScheduleResponse;
import com.yourproject.backend.dtos.responses.MedicalImageResponse;
import com.yourproject.backend.dtos.responses.PrescriptionResponse;
import com.yourproject.backend.dtos.responses.UserSummaryResponse;
import com.yourproject.backend.models.AppointmentStatus;

public interface ClinicalMedicationService {
    List<AppointmentResponse> getDoctorAppointments(String doctorId, AppointmentStatus status);
    DoctorExaminationResponse getDoctorExamination(String doctorId, String appointmentId);
    List<UserSummaryResponse> getDoctorPatients(String doctorId);
    List<DoctorExaminationResponse> getPatientMedicalRecordHistory(String doctorId, String patientId);
    DoctorExaminationResponse startExamination(String doctorId, String appointmentId);
    DoctorExaminationResponse updateClinicalInformation(
            String doctorId, String appointmentId, UpdateClinicalInformationRequest request);
    DoctorExaminationResponse updateDiagnosis(
            String doctorId, String appointmentId, UpdateDiagnosisRequest request);
    List<MedicalImageResponse> uploadMedicalImage(
            String doctorId, String appointmentId, MultipartFile image);
    List<MedicalImageResponse> deleteMedicalImage(
            String doctorId, String appointmentId, String imageId);
    PrescriptionResponse createPrescription(
            String doctorId, String appointmentId, UpsertPrescriptionRequest request);
    PrescriptionResponse updatePrescription(
            String doctorId, String appointmentId, UpsertPrescriptionRequest request);
    DoctorExaminationResponse completeExamination(String doctorId, String appointmentId);
    List<MedicineScheduleResponse> getPatientMedicineSchedules(String patientId);
    MedicineScheduleResponse updatePatientScheduleTime(
            String patientId, String scheduleId, UpdateMedicineScheduleTimeRequest request);
    MedicineScheduleResponse markMedicineTaken(String patientId, String scheduleId);
}
