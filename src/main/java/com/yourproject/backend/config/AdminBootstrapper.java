package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
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
@Order(3)
@RequiredArgsConstructor
public class AdminBootstrapper implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final UserRepository userRepository;
    private final UserService userService;
    private final PatientDataProtectionService patientDataProtectionService;

    @Value("${app.bootstrap.admin.phone-number}")
    private String phoneNumber;

    @Value("${app.bootstrap.admin.password}")
    private String password;

    @Value("${app.bootstrap.admin.full-name}")
    private String fullName;

    @Override
    public void run(ApplicationArguments args) {
        if (phoneNumber.isBlank() && password.isBlank()) {
            LOGGER.warn("No bootstrap admin credentials were provided. Set BOOTSTRAP_ADMIN_PHONE_NUMBER and BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (phoneNumber.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap admin phone number and password must be provided.");
        }
        String normalizedPhoneNumber = PhoneNumberNormalizer.normalize(phoneNumber);
        if (userRepository.existsByPhoneLookup(patientDataProtectionService.phoneLookup(normalizedPhoneNumber))
                || userRepository.findByPhoneNumber(normalizedPhoneNumber).isPresent()) {
            LOGGER.info("Bootstrap administrator account already exists; creation skipped.");
            return;
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setPhoneNumber(phoneNumber);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setRole(UserRole.ADMIN);
        userService.createUser(request, null);
        LOGGER.info("Bootstrap administrator account created.");
    }
}
