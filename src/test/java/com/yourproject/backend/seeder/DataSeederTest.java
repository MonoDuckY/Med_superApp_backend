package com.yourproject.backend.seeder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.models.Appointment;
import com.yourproject.backend.models.AppointmentStatus;
import com.yourproject.backend.models.ClinicRoom;
import com.yourproject.backend.models.DoctorWorkSlot;
import com.yourproject.backend.models.DoctorWorkSlotStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.models.WorkSession;
import com.yourproject.backend.models.WorkSlot;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.WorkSlotRepository;
import com.yourproject.backend.services.UserService;

@SpringBootTest
public class DataSeederTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ClinicRoomRepository clinicRoomRepository;

    @Autowired
    private WorkSlotRepository workSlotRepository;

    @Autowired
    private DoctorWorkSlotRepository doctorWorkSlotRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Test
    public void seedDoctorScheduleAndAppointment() {
        // 1. Create a Doctor
        String doctorPhone = "0987654321";
        User doctor;
        try {
            doctor = userService.findByPhoneNumberAndRole(doctorPhone, UserRole.DOCTOR);
        } catch (com.yourproject.backend.exceptions.UnauthorizedException e) {
            doctor = null;
        }
        if (doctor == null) {
            CreateUserRequest doctorRequest = new CreateUserRequest();
            doctorRequest.setRole(UserRole.DOCTOR);
            doctorRequest.setFullName("Nguyen Van Doctor");
            doctorRequest.setPhoneNumber(doctorPhone);
            doctorRequest.setPassword("Doctor123!");
            doctorRequest.setDateOfBirth(LocalDate.of(1980, 1, 1));
            doctorRequest.setGender("MALE");
            doctorRequest.setCertificate("CERT12345");
            doctor = userService.createUser(doctorRequest, "SYSTEM");
            System.out.println("Created new doctor: " + doctor.getId());
        } else {
            System.out.println("Found existing doctor: " + doctor.getId());
        }

        ClinicRoom room = clinicRoomRepository.findAll().stream()
                .filter(r -> "A310".equals(r.getName()))
                .findFirst()
                .orElse(null);
        if (room == null) {
            try {
                room = ClinicRoom.builder()
                        .name("A310")
                        .active(true)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build();
                room = clinicRoomRepository.save(room);
                System.out.println("Created new clinic room: " + room.getId());
            } catch (Exception e) {
                // Ignore and just get any room or create a unique one
                room = clinicRoomRepository.findAll().stream().findFirst().orElse(null);
                if (room == null) {
                    room = ClinicRoom.builder()
                            .name("A310_" + System.currentTimeMillis())
                            .active(true)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    room = clinicRoomRepository.save(room);
                }
                System.out.println("Using fallback room: " + room.getId());
            }
        } else {
            System.out.println("Found existing clinic room: " + room.getId());
        }

        // 3. Ensure WorkSlots exist from 8:00 to 17:30
        List<WorkSlot> allSlots = new ArrayList<>();
        for (int i = 0; i < 19; i++) {
            LocalTime startTime;
            WorkSession session;
            if (i < 8) { // 8:00 to 12:00
                startTime = LocalTime.of(8, 0).plusMinutes(i * 30L);
                session = WorkSession.MORNING;
            } else { // 13:00 to 17:30
                startTime = LocalTime.of(13, 0).plusMinutes((i - 8) * 30L);
                session = WorkSession.AFTERNOON;
            }
            LocalTime endTime = startTime.plusMinutes(30);
            String name = "Slot" + (i + 1);

            WorkSlot slot = workSlotRepository.findByName(name).orElse(null);
            if (slot == null) {
                slot = WorkSlot.builder()
                        .name(name)
                        .startTime(startTime)
                        .endTime(endTime)
                        .session(session)
                        .active(true)
                        .build();
                slot = workSlotRepository.save(slot);
            }
            allSlots.add(slot);
        }

        // 4. Create Work Schedule for Doctor from 8/7/2026 to 15/7/2026
        LocalDate startDate = LocalDate.of(2026, 7, 8); // 8/7/2026
        LocalDate endDate = LocalDate.of(2026, 7, 15); // 15/7/2026
        
        // Note: as per user prompt, today might be in August. 
        // We will seed July 8 to July 15 as requested literally, BUT ALSO seed today (August 7, 2026) so the appointment can be scheduled.
        LocalDate today = LocalDate.of(2026, 8, 7);
        
        List<LocalDate> scheduleDates = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            scheduleDates.add(date);
        }
        if (!scheduleDates.contains(today)) {
            scheduleDates.add(today);
        }

        DoctorWorkSlot firstSlotForToday = null;

        for (LocalDate date : scheduleDates) {
            for (WorkSlot slot : allSlots) {
                final String finalDoctorId = doctor.getId();
                final String finalRoomId = room.getId();
                DoctorWorkSlot dws = doctorWorkSlotRepository.findAll().stream()
                        .filter(d -> d.getDoctorId().equals(finalDoctorId) &&
                                     d.getWorkDate().equals(date) &&
                                     d.getSlotId().equals(slot.getId()) &&
                                     d.getRoomId().equals(finalRoomId))
                        .findFirst()
                        .orElse(null);

                if (dws == null) {
                    dws = DoctorWorkSlot.builder()
                            .doctorId(doctor.getId())
                            .workDate(date)
                            .slotId(slot.getId())
                            .roomId(room.getId())
                            .status(DoctorWorkSlotStatus.AVAILABLE)
                            .conflictActive(true)
                            .submittedAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    dws = doctorWorkSlotRepository.save(dws);
                }

                if (date.equals(today) && firstSlotForToday == null) {
                    firstSlotForToday = dws;
                }
            }
        }
        System.out.println("Created doctor work slots.");

        // 5. Create an Appointment today
        if (firstSlotForToday != null) {
            // Need a patient
            String patientPhone = "0123456789";
            User patient;
            try {
                patient = userService.findByPhoneNumberAndRole(patientPhone, UserRole.PATIENT);
            } catch (com.yourproject.backend.exceptions.UnauthorizedException e) {
                patient = null;
            }
            if (patient == null) {
                CreateUserRequest patientReq = new CreateUserRequest();
                patientReq.setRole(UserRole.PATIENT);
                patientReq.setFullName("Nguyen Van Patient");
                patientReq.setPhoneNumber(patientPhone);
                patientReq.setDateOfBirth(LocalDate.of(2000, 1, 1));
                patientReq.setGender("MALE");
                patient = userService.createUser(patientReq, "SYSTEM");
            }
            
            // Note: DoctorWorkSlot status might need to be BOOKED
            firstSlotForToday.setStatus(DoctorWorkSlotStatus.BOOKED);
            doctorWorkSlotRepository.save(firstSlotForToday);

            Appointment appointment = Appointment.builder()
                    .patientUserId(patient.getId())
                    .doctorWorkSlotId(firstSlotForToday.getId())
                    .status(AppointmentStatus.CONFIRMED)
                    .requestedAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
            appointmentRepository.save(appointment);
            System.out.println("Created appointment: " + appointment.getId());
        }
    }
}
