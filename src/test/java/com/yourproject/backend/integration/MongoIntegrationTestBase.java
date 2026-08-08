package com.yourproject.backend.integration;

import java.util.Base64;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;

import com.yourproject.backend.models.AccountStatus;
import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.PatientOtpRepository;
import com.yourproject.backend.repositories.AppointmentRepository;
import com.yourproject.backend.repositories.ClinicRoomRepository;
import com.yourproject.backend.repositories.DoctorWorkSlotRepository;
import com.yourproject.backend.repositories.SmsGatewayDeviceRepository;
import com.yourproject.backend.repositories.SmsGatewayJobRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.repositories.MedicalRecordRepository;
import com.yourproject.backend.repositories.PrescriptionRepository;
import com.yourproject.backend.repositories.MedicineScheduleRepository;
import com.yourproject.backend.repositories.MealRepository;
import com.yourproject.backend.repositories.WorkoutRepository;
import com.yourproject.backend.repositories.DishRepository;
import com.yourproject.backend.services.FcmGatewayService;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.JwtUtils;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
public abstract class MongoIntegrationTestBase {
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    static {
        MONGO.start();
    }

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected MedicalRecordRepository medicalRecordRepository;

    @Autowired
    protected PrescriptionRepository prescriptionRepository;

    @Autowired
    protected MedicineScheduleRepository medicineScheduleRepository;

    @Autowired
    protected MealRepository mealRepository;

    @Autowired
    protected WorkoutRepository workoutRepository;

    @Autowired
    protected DishRepository dishRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected PatientDataProtectionService patientDataProtectionService;

    @Autowired
    protected PatientOtpRepository patientOtpRepository;

    @Autowired
    protected AppointmentRepository appointmentRepository;

    @Autowired
    protected DoctorWorkSlotRepository doctorWorkSlotRepository;

    @Autowired
    protected ClinicRoomRepository clinicRoomRepository;

    @Autowired
    protected SmsGatewayJobRepository smsGatewayJobRepository;

    @Autowired
    protected SmsGatewayDeviceRepository smsGatewayDeviceRepository;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtUtils jwtUtils;

    @MockitoBean
    protected FcmGatewayService fcmGatewayService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("app.jwt.secret", () -> "integration-test-secret-with-at-least-thirty-two-characters");
        registry.add("app.patient-data.aes-key", () -> key((byte) 1));
        registry.add("app.patient-data.lookup-hmac-key", () -> key((byte) 2));
        registry.add("app.bootstrap.admin.phone-number", () -> "");
        registry.add("app.bootstrap.admin.password", () -> "");
        registry.add("app.patient-data.migrate-legacy-on-startup", () -> "false");
        registry.add("app.sms-gateway.direct-fcm-token", () -> "integration-test-fcm-token");
        registry.add("app.sms-gateway.registration-key", () -> "integration-test-gateway-key");
        registry.add("app.otp.expiration-minutes", () -> "5");
        registry.add("app.otp.resend-cooldown-seconds", () -> "60");
        registry.add("app.otp.max-attempts", () -> "5");
        registry.add("app.auth.max-failed-login-attempts", () -> "5");
        registry.add("app.auth.lockout-minutes", () -> "15");
    }

    @BeforeEach
    void clearDatabase() {
        appointmentRepository.deleteAll();
        dishRepository.deleteAll();
        workoutRepository.deleteAll();
        mealRepository.deleteAll();
        medicineScheduleRepository.deleteAll();
        prescriptionRepository.deleteAll();
        medicalRecordRepository.deleteAll();
        doctorWorkSlotRepository.deleteAll();
        clinicRoomRepository.deleteAll();
        patientOtpRepository.deleteAll();
        smsGatewayJobRepository.deleteAll();
        smsGatewayDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected User saveActiveDoctor(String normalizedPhone, String password) {
        Instant now = Instant.now();
        return userRepository.save(User.builder()
                .fullName("Dr Integration")
                .roleId(UserRole.DOCTOR.getId())
                .status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone)
                .phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode(password))
                .certificate("Practice certificate")
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now.minusSeconds(10))
                .build());
    }

    protected User saveActiveAdmin(String normalizedPhone, String password) {
        Instant now = Instant.now();
        return userRepository.save(User.builder()
                .fullName("Admin Integration")
                .roleId(UserRole.ADMIN.getId())
                .status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone)
                .phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now.minusSeconds(10))
                .build());
    }

    protected User saveActiveStaff(String normalizedPhone, String password) {
        Instant now = Instant.now();
        return userRepository.save(User.builder()
                .fullName("Staff Integration")
                .roleId(UserRole.STAFF.getId())
                .status(AccountStatus.ACTIVE)
                .phoneNumber(normalizedPhone)
                .phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .passwordHash(passwordEncoder.encode(password))
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now.minusSeconds(10))
                .build());
    }

    protected User saveActivePatient(String normalizedPhone) {
        return saveActivePatient(normalizedPhone, "PAT-INTEGRATION");
    }

    protected User saveActivePatient(String normalizedPhone, String patientId) {
        Instant now = Instant.now();
        User patient = User.builder()
                .fullName("Patient Integration")
                .roleId(UserRole.PATIENT.getId())
                .status(AccountStatus.ACTIVE)
                .gender("NONE")
                .dateOfBirth(java.time.LocalDate.of(1995, 1, 1))
                .phoneNumber(normalizedPhone)
                .phoneLookup(patientDataProtectionService.phoneLookup(normalizedPhone))
                .address("Test address")
                .createdAt(now)
                .updatedAt(now)
                .passwordChangedAt(now.minusSeconds(10))
                .build();
        patientDataProtectionService.encryptPatientFields(patient);
        return userRepository.save(patient);
    }

    private static String key(byte seed) {
        byte[] bytes = new byte[32];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (seed + index);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
