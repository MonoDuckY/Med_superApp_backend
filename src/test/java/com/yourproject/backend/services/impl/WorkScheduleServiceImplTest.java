package com.yourproject.backend.services.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yourproject.backend.dtos.requests.ScheduleDecisionRequest;
import com.yourproject.backend.dtos.requests.SubmitWorkScheduleRequest;
import com.yourproject.backend.dtos.requests.BlockWorkSlotRequest;
import com.yourproject.backend.exceptions.BadRequestException;
import com.yourproject.backend.exceptions.ConflictException;
import com.yourproject.backend.exceptions.ForbiddenException;
import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.ScheduleDecision;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.models.WorkSlotApprovalStatus;
import com.yourproject.backend.models.WorkSlotBookingStatus;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.WorkSlotRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;

@ExtendWith(MockitoExtension.class)
class WorkScheduleServiceImplTest {
    @Mock
    private DoctorWorkSlotRepository doctorWorkSlotRepository;

    @Mock
    private WorkSlotRepository workSlotRepository;

    @Mock
    private ClinicRoomRepository clinicRoomRepository;

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientDataProtectionService patientDataProtectionService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private WorkScheduleServiceImpl service;

    private User doctor;
    private User staff;
    private ClinicRoom room;
    private List<WorkSlot> morningSlots;

    @BeforeEach
    void setUp() {
        doctor = User.builder().id("doctor-1").roleId(UserRole.DOCTOR.name()).status(AccountStatus.ACTIVE).build();
        staff = User.builder().id("staff-1").roleId(UserRole.STAFF.name()).status(AccountStatus.ACTIVE).build();
        room = ClinicRoom.builder().id("room-1").code("ROOM-101").active(true).build();
        morningSlots = new ArrayList<>();
        for (int index = 0; index < 8; index++) {
            morningSlots.add(WorkSlot.builder()
                    .id("slot-" + (index + 1))
                    .name("Slot" + (index + 1))
                    .startTime(LocalTime.of(8, 0).plusMinutes(index * 30L))
                    .endTime(LocalTime.of(8, 30).plusMinutes(index * 30L))
                    .session(WorkSession.MORNING)
                    .active(true)
                    .build());
        }
    }

    @Test
    void submitMorningCreatesEightPendingSlots() {
        when(userService.getActiveUserById("doctor-1")).thenReturn(doctor);
        when(clinicRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(workSlotRepository.findAllByOrderByStartTimeAsc()).thenReturn(morningSlots);
        when(doctorWorkSlotRepository.findAllByWorkDateAndSlotIdIn(any(), any()))
                .thenReturn(List.of());
        when(doctorWorkSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<DoctorWorkSlot> result = service.submit("doctor-1", request(WorkSession.MORNING));

        assertEquals(8, result.size());
        assertEquals(8, result.stream().map(DoctorWorkSlot::getSlotId).distinct().count());
        result.forEach(slot -> {
            assertEquals(DoctorWorkSlotStatus.PENDING, slot.getStatus());
            assertEquals("doctor-1", slot.getDoctorId());
            assertEquals("room-1", slot.getRoomId());
        });
    }

    @Test
    void submitRejectsExistingDoctorOrRoomConflict() {
        when(userService.getActiveUserById("doctor-1")).thenReturn(doctor);
        when(clinicRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(workSlotRepository.findAllByOrderByStartTimeAsc()).thenReturn(morningSlots);
        when(doctorWorkSlotRepository.findAllByWorkDateAndSlotIdIn(any(), any()))
                .thenReturn(List.of(DoctorWorkSlot.builder()
                        .doctorId("another-doctor")
                        .roomId("room-1")
                        .slotName("Slot1")
                        .build()));

        assertThrows(ConflictException.class, () -> service.submit("doctor-1", request(WorkSession.MORNING)));
        verify(doctorWorkSlotRepository, never()).saveAll(anyList());
    }

    @Test
    void rejectingScheduleRequiresReason() {
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(doctorWorkSlotRepository.findAllBySubmissionIdOrderBySlotIdAsc("submission-1"))
                .thenReturn(List.of(pendingSlot()));
        ScheduleDecisionRequest request = new ScheduleDecisionRequest();
        request.setDecision(ScheduleDecision.REJECT);

        assertThrows(BadRequestException.class, () -> service.decide("staff-1", "submission-1", request));
    }

    @Test
    void rejectingScheduleReleasesEveryConflictKey() {
        DoctorWorkSlot first = pendingSlot();
        DoctorWorkSlot second = pendingSlot();
        second.setId("work-slot-2");
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(doctorWorkSlotRepository.findAllBySubmissionIdOrderBySlotIdAsc("submission-1"))
                .thenReturn(List.of(first, second));
        when(doctorWorkSlotRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        ScheduleDecisionRequest request = new ScheduleDecisionRequest();
        request.setDecision(ScheduleDecision.REJECT);
        request.setRejectionReason("Room maintenance");

        List<DoctorWorkSlot> result = service.decide("staff-1", "submission-1", request);

        result.forEach(slot -> {
            assertEquals(DoctorWorkSlotStatus.REJECTED, slot.getStatus());
            assertFalse(slot.isConflictActive());
            assertEquals("Room maintenance", slot.getRejectionReason());
        });
    }

    @Test
    void getSchedulesFiltersByApprovalStatus() {
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(doctorWorkSlotRepository.findAllByStatusOrderBySubmittedAtDesc(
                DoctorWorkSlotStatus.AVAILABLE)).thenReturn(List.of(pendingSlot()));

        List<DoctorWorkSlot> result = service.getSchedules("staff-1", DoctorWorkSlotStatus.AVAILABLE);

        assertEquals(1, result.size());
    }

    @Test
    void staffUserCanUseStaffScheduleOperations() {
        User multiRoleUser = User.builder().id("staff-user")
                .roleId(UserRole.STAFF.name())
                .status(AccountStatus.ACTIVE).build();
        when(userService.getActiveUserById("staff-user")).thenReturn(multiRoleUser);
        when(doctorWorkSlotRepository.findAllByStatusOrderBySubmittedAtDesc(
                DoctorWorkSlotStatus.AVAILABLE)).thenReturn(List.of());

        List<DoctorWorkSlot> result = service.getSchedules(
                "staff-user", DoctorWorkSlotStatus.AVAILABLE);

        assertEquals(0, result.size());
    }

    @Test
    void adminWithoutStaffRoleCannotUseStaffScheduleOperations() {
        User admin = User.builder().id("admin-user")
                .roleId(UserRole.ADMIN.name()).status(AccountStatus.ACTIVE).build();
        when(userService.getActiveUserById("admin-user")).thenReturn(admin);

        assertThrows(ForbiddenException.class,
                () -> service.getSchedules("admin-user", DoctorWorkSlotStatus.AVAILABLE));
    }

    @Test
    void toResponsesIncludesDoctorDetails() {
        doctor.setFullName("Dr Nguyen Van A");
        doctor.setPhoneNumber("+84911111111");
        doctor.setCertificate("Cardiology");
        when(userRepository.findAllById(any())).thenReturn(List.of(doctor));

        var response = service.toResponses(List.of(pendingSlot())).get(0);

        assertEquals("doctor-1", response.getDoctor().getId());
        assertEquals("Dr Nguyen Van A", response.getDoctor().getFullName());
        assertEquals("Cardiology", response.getDoctor().getCertificate());
    }

    private SubmitWorkScheduleRequest request(WorkSession session) {
        SubmitWorkScheduleRequest request = new SubmitWorkScheduleRequest();
        request.setWorkDate(LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).plusDays(2));
        request.setSession(session);
        request.setRoomId("room-1");
        return request;
    }

    private DoctorWorkSlot pendingSlot() {
        return DoctorWorkSlot.builder()
                .id("work-slot-1")
                .submissionId("submission-1")
                .doctorId("doctor-1")
                .workDate(LocalDate.now().plusDays(2))
                .slotId("slot-1")
                .status(DoctorWorkSlotStatus.PENDING)
                .conflictActive(true)
                .build();
    }

    @Test
    void modifyPendingSubmissionReplacesSlotsAndKeepsSubmissionId() {
        DoctorWorkSlot pending = pendingSlot();
        when(userService.getActiveUserById("doctor-1")).thenReturn(doctor);
        when(doctorWorkSlotRepository.findAllBySubmissionIdAndDoctorIdOrderBySlotIdAsc(
                "submission-1", "doctor-1")).thenReturn(List.of(pending));
        when(clinicRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(workSlotRepository.findAllByOrderByStartTimeAsc()).thenReturn(morningSlots);
        when(doctorWorkSlotRepository.findAllByWorkDateAndSlotIdIn(any(), any()))
                .thenReturn(List.of());
        when(doctorWorkSlotRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<DoctorWorkSlot> result = service.modifyPendingSubmission(
                "doctor-1", "submission-1", request(WorkSession.MORNING));

        assertEquals(8, result.size());
        result.forEach(slot -> {
            assertEquals("submission-1", slot.getSubmissionId());
            assertEquals(DoctorWorkSlotStatus.PENDING, slot.getStatus());
        });
        verify(doctorWorkSlotRepository).deleteAll(List.of(pending));
    }

    @Test
    void blockBookedSlotCancelsAffectedAppointment() {
        DoctorWorkSlot booked = pendingSlot();
        booked.setStatus(DoctorWorkSlotStatus.BOOKED);
        Appointment appointment = Appointment.builder()
                .id("appointment-1")
                .doctorWorkSlotId(booked.getId())
                .status(AppointmentStatus.CONFIRMED)
                .build();
        BlockWorkSlotRequest request = new BlockWorkSlotRequest();
        request.setReason("Doctor unavailable");
        when(userService.getActiveUserById("staff-1")).thenReturn(staff);
        when(doctorWorkSlotRepository.findById(booked.getId())).thenReturn(Optional.of(booked));
        when(appointmentRepository.findFirstByDoctorWorkSlotIdAndStatusIn(any(), any()))
                .thenReturn(Optional.of(appointment));
        when(doctorWorkSlotRepository.save(booked)).thenReturn(booked);

        DoctorWorkSlot result = service.blockSlot("staff-1", booked.getId(), request);

        assertEquals(DoctorWorkSlotStatus.CANCELLED, result.getStatus());
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Doctor unavailable", appointment.getCancellationReason());
        verify(appointmentRepository).save(appointment);
    }
}
