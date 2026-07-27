package com.yourproject.backend.config;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.utils.PhoneNumberNormalizer;

import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@Order(4)
@RequiredArgsConstructor
public class DevDataBootstrapper implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DevDataBootstrapper.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PatientDataProtectionService patientDataProtectionService;

    @Override
    public void run(ApplicationArguments args) {
        // Tạo sẵn một patient để test OTP
        String testPatientPhone = "0869465858";
        String normalizedTestPhone = PhoneNumberNormalizer.normalize(testPatientPhone);
        if (!userRepository.existsByPhoneLookup(patientDataProtectionService.phoneLookup(normalizedTestPhone))) {
            CreateUserRequest patientReq = new CreateUserRequest();
            patientReq.setPhoneNumber(testPatientPhone);
            patientReq.setPassword("Password123!");
            patientReq.setFullName("Bệnh nhân Test");
            patientReq.setGender("MALE");
            patientReq.setDateOfBirth(LocalDate.of(1990, 1, 1));
            patientReq.setRole(UserRole.PATIENT);
            userService.createUser(patientReq, null);
            LOGGER.info("Dev profile active: Test patient account created for OTP testing: " + testPatientPhone);
        } else {
            LOGGER.info("Dev profile active: Test patient account already exists.");
        }
    }
}
