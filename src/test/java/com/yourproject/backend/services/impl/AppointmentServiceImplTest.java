package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.yourproject.backend.dtos.requests.AppointmentDecisionRequest;
import com.yourproject.backend.dtos.requests.BookAppointmentRequest;
import com.yourproject.backend.dtos.requests.CancelAppointmentRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.ScheduleDecision;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.WorkSlotApprovalStatus;
import com.yourproject.backend.models.WorkSlotBookingStatus;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.services.PatientDataProtectionService;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {
    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private DoctorWorkSlotRepository doctorWorkSlotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @Mock
    private MongoTemplate mongoTemplate;

    @InjectMocks
    private AppointmentServiceImpl service;

    private User patient;
    private User doctor;
    private User staff;
    private DoctorWorkSlot availableSlot;

    @BeforeEach
    void setUp() {
        patient = User.builder()
                .id("patient-user-1")
                .patientId("PAT-0001")
                .role(UserRole.PATIENT)
                .status(AccountStatus.ACTIVE)
                .build();
        doctor = User.builder().id("doctor-1").role(UserRole.DOCTOR).status(AccountStatus.ACTIVE).build();
        staff = User.builder().id("staff-1").role(UserRole.STAFF).status(AccountStatus.ACTIVE).build();
        availableSlot = DoctorWorkSlot.builder()
                .id("work-slot-1")
                .doctorId("doctor-1")
                .workDate(LocalDate.now().plusDays(2))
                .slotId("slot-1")
                .slotName("Slot1")
                .roomId("room-1")
                .roomCode("ROOM-101")
                .startAt(Instant.now().plusSeconds(48 * 60 * 60))
                .endAt(Instant.now().plusSeconds(48 * 60 * 60 + 1800))
                .approvalStatus(WorkSlotApprovalStatus.APPROVED)
                .bookingStatus(WorkSlotBookingStatus.AVAILABLE)
                .build();
    }

    @Test
    void bookClaimsSlotAndCreatesPendingAppointment() {
        when(userService.getActiveUserById("patient-user-1")).thenReturn(patient);
        when(userService.getActiveUserById("doctor-1")).thenReturn(doctor);
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(availableSlot));
        when(appointmentRepository.existsByPatientUserIdAndAppointmentDateAndActiveTrue(any(), any()))
                .thenReturn(false);
        DoctorWorkSlot claimed = copySlot(WorkSlotBookingStatus.PENDING_CONFIRMATION);
        when(mongoTemplate.findAndModify(
                any(Query.class),
                any(Update.class),
                any(FindAndModifyOptions.class),
                eq(DoctorWorkSlot.class))).thenReturn(claimed);
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorWorkSlotId("work-slot-1");

        Appointment appointment = service.book("patient-user-1", request);

        assertEquals(AppointmentStatus.PENDING_STAFF_CONFIRMATION, appointment.getStatus());
        assertEquals("patient-user-1", appointment.getPatientUserId());
        assertEquals("PAT-0001", appointment.getPatientId());
        assertEquals("work-slot-1", appointment.getDoctorWorkSlotId());
    }

    @Test
    void bookRejectsSecondActiveAppointmentOnSameDay() {
        when(userService.getActiveUserById("patient-user-1")).thenReturn(patient);
        when(userService.getActiveUserById("doctor-1")).thenReturn(doctor);
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(availableSlot));
        when(appointmentRepository.existsByPatientUserIdAndAppointmentDateAndActiveTrue(any(), any()))
                .thenReturn(true);
        BookAppointmentRequest request = new BookAppointmentRequest();
        request.setDoctorWorkSlotId("work-slot-1");

        assertThrows(ConflictException.class, () -> service.book("patient-user-1", request));
    }

    @Test
    void rejectingAppointmentReleasesSlot() {
        Appointment appointment = pendingAppointment();
        DoctorWorkSlot pendingSlot = copySlot(WorkSlotBookingStatus.PENDING_CONFIRMATION);
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(pendingSlot));
        when(doctorWorkSlotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        AppointmentDecisionRequest request = new AppointmentDecisionRequest();
        request.setDecision(ScheduleDecision.REJECT);
        request.setRejectionReason("Patient record requires review");

        Appointment result = service.decide("staff-1", "appointment-1", request);

        assertEquals(AppointmentStatus.REJECTED, result.getStatus());
        assertFalse(result.isActive());
        assertEquals(WorkSlotBookingStatus.AVAILABLE, pendingSlot.getBookingStatus());
    }

    @Test
    void rejectingAppointmentRequiresReason() {
        Appointment appointment = pendingAppointment();
        DoctorWorkSlot pendingSlot = copySlot(WorkSlotBookingStatus.PENDING_CONFIRMATION);
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(pendingSlot));
        AppointmentDecisionRequest request = new AppointmentDecisionRequest();
        request.setDecision(ScheduleDecision.REJECT);

        assertThrows(BadRequestException.class, () -> service.decide("staff-1", "appointment-1", request));
    }

    @Test
    void toResponsesLooksUpDoctorAndPatientDetails() {
        doctor.setFullName("Dr Nguyen Van A");
        doctor.setPhoneNumber("+84911111111");
        doctor.setCertificate("Internal Medicine");
        patient.setFullName("Tran Thi B");
        patient.setPhoneNumber("+84922222222");
        Appointment appointment = pendingAppointment();
        appointment.setDoctorId(doctor.getId());
        when(userRepository.findAllById(any())).thenReturn(List.of(doctor, patient));

        var response = service.toResponses(List.of(appointment)).get(0);

        assertEquals("Dr Nguyen Van A", response.getDoctor().getFullName());
        assertEquals("Internal Medicine", response.getDoctor().getCertificate());
        assertEquals("Tran Thi B", response.getPatient().getFullName());
        assertEquals("PAT-0001", response.getPatient().getPatientId());
    }

    @Test
    void getAppointmentsFiltersByStatus() {
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(appointmentRepository.findAllByStatusOrderByRequestedAtDesc(AppointmentStatus.CONFIRMED))
                .thenReturn(List.of(pendingAppointment()));

        List<Appointment> result = service.getAppointments("staff-1", AppointmentStatus.CONFIRMED);

        assertEquals(1, result.size());
    }

    @Test
    void cancelConfirmedAppointmentReleasesSlotAndMakesAppointmentInactive() {
        Appointment appointment = pendingAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setDoctorWorkSlotId("work-slot-1");
        DoctorWorkSlot bookedSlot = copySlot(WorkSlotBookingStatus.BOOKED);
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(bookedSlot));
        when(doctorWorkSlotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setCancellationReason("Doctor unavailable");

        Appointment result = service.cancel("staff-1", "appointment-1", request);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertFalse(result.isActive());
        assertEquals("Doctor unavailable", result.getCancellationReason());
        assertEquals("staff-1", result.getCancelledBy());
        assertEquals(WorkSlotBookingStatus.AVAILABLE, bookedSlot.getBookingStatus());
    }

    @Test
    void cancelRejectsAppointmentThatIsNotConfirmed() {
        Appointment appointment = pendingAppointment();
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setCancellationReason("Schedule changed");

        assertThrows(ConflictException.class, () -> service.cancel("staff-1", "appointment-1", request));
    }

    @Test
    void patientCancelsOwnPendingAppointmentAndReleasesSlot() {
        Appointment appointment = pendingAppointment();
        appointment.setDoctorWorkSlotId("work-slot-1");
        DoctorWorkSlot pendingSlot = copySlot(WorkSlotBookingStatus.PENDING_CONFIRMATION);
        when(userService.getActiveUserById("patient-user-1")).thenReturn(patient);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(pendingSlot));
        when(doctorWorkSlotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setCancellationReason("Patient changed plans");

        Appointment result = service.cancelByPatient("patient-user-1", "appointment-1", request);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertFalse(result.isActive());
        assertEquals("patient-user-1", result.getCancelledBy());
        assertEquals(WorkSlotBookingStatus.AVAILABLE, pendingSlot.getBookingStatus());
    }

    @Test
    void patientCancelsOwnConfirmedAppointmentAndReleasesSlot() {
        Appointment appointment = pendingAppointment();
        appointment.setStatus(AppointmentStatus.CONFIRMED);
        appointment.setDoctorWorkSlotId("work-slot-1");
        DoctorWorkSlot bookedSlot = copySlot(WorkSlotBookingStatus.BOOKED);
        when(userService.getActiveUserById("patient-user-1")).thenReturn(patient);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.findById("work-slot-1")).thenReturn(Optional.of(bookedSlot));
        when(doctorWorkSlotRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setCancellationReason("Patient is unavailable");

        Appointment result = service.cancelByPatient("patient-user-1", "appointment-1", request);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(WorkSlotBookingStatus.AVAILABLE, bookedSlot.getBookingStatus());
    }

    @Test
    void patientCannotCancelAnotherPatientsAppointment() {
        User anotherPatient = User.builder()
                .id("patient-user-2")
                .role(UserRole.PATIENT)
                .status(AccountStatus.ACTIVE)
                .build();
        Appointment appointment = pendingAppointment();
        when(userService.getActiveUserById("patient-user-2")).thenReturn(anotherPatient);
        when(appointmentRepository.findById("appointment-1")).thenReturn(Optional.of(appointment));
        CancelAppointmentRequest request = new CancelAppointmentRequest();
        request.setCancellationReason("Invalid cancellation attempt");

        assertThrows(ForbiddenException.class,
                () -> service.cancelByPatient("patient-user-2", "appointment-1", request));
    }

    private DoctorWorkSlot copySlot(WorkSlotBookingStatus bookingStatus) {
        return DoctorWorkSlot.builder()
                .id(availableSlot.getId())
                .doctorId(availableSlot.getDoctorId())
                .workDate(availableSlot.getWorkDate())
                .slotId(availableSlot.getSlotId())
                .slotName(availableSlot.getSlotName())
                .roomId(availableSlot.getRoomId())
                .roomCode(availableSlot.getRoomCode())
                .startAt(availableSlot.getStartAt())
                .endAt(availableSlot.getEndAt())
                .approvalStatus(WorkSlotApprovalStatus.APPROVED)
                .bookingStatus(bookingStatus)
                .build();
    }

    private Appointment pendingAppointment() {
        return Appointment.builder()
                .id("appointment-1")
                .patientUserId("patient-user-1")
                .patientId("PAT-0001")
                .doctorWorkSlotId("work-slot-1")
                .status(AppointmentStatus.PENDING_STAFF_CONFIRMATION)
                .active(true)
                .build();
    }
}
