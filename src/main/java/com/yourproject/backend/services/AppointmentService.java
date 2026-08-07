package com.yourproject.backend.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.yourproject.backend.dtos.requests.AppointmentDecisionRequest;
import com.yourproject.backend.dtos.requests.BookAppointmentRequest;
import com.yourproject.backend.dtos.requests.CancelAppointmentRequest;
import com.yourproject.backend.dtos.requests.RescheduleAppointmentRequest;
import com.yourproject.backend.dtos.requests.StaffCreateAppointmentRequest;
import com.yourproject.backend.dtos.responses.AppointmentResponse;
import com.yourproject.backend.dtos.responses.AvailableAppointmentSlotResponse;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.DoctorWorkSlot;

public interface AppointmentService {
    List<DoctorWorkSlot> getAvailableSlots(String patientUserId, LocalDate date, String doctorName);

    Map<String, String> getDoctorNames(List<DoctorWorkSlot> slots);

    List<AvailableAppointmentSlotResponse> toAvailableSlotResponses(List<DoctorWorkSlot> slots);

    Appointment book(String patientUserId, BookAppointmentRequest request);

    List<Appointment> getPatientAppointments(String patientUserId);

    List<Appointment> getPendingAppointments(String staffId);

    List<Appointment> getAppointments(String staffId, com.yourproject.backend.models.AppointmentStatus status);

    Appointment decide(String staffId, String appointmentId, AppointmentDecisionRequest request);

    Appointment cancel(String staffId, String appointmentId, CancelAppointmentRequest request);

    Appointment cancelByPatient(String patientUserId, String appointmentId, CancelAppointmentRequest request);

    Appointment reschedule(String staffId, String appointmentId, RescheduleAppointmentRequest request);

    Appointment createByStaff(String staffId, StaffCreateAppointmentRequest request);

    AppointmentResponse toResponse(Appointment appointment);

    List<AppointmentResponse> toResponses(List<Appointment> appointments);
}
