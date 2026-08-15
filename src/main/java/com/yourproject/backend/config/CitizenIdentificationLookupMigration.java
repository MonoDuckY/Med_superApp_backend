package com.yourproject.backend.config;

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;

import lombok.RequiredArgsConstructor;

@Component
@Order(3)
@RequiredArgsConstructor
public class CitizenIdentificationLookupMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(CitizenIdentificationLookupMigration.class);

    private final UserRepository userRepository;
    private final PatientDataProtectionService patientDataProtectionService;

    @Override
    public void run(ApplicationArguments args) {
        long migrated = 0;
        for (User user : userRepository.findAll()) {
            if (user.getCitizenIdentificationLookup() != null) continue;
            patientDataProtectionService.decryptPatientFields(user);
            String citizenId = user.getCitizenIdentificationCode();
            if (citizenId == null || citizenId.isBlank()) continue;
            user.setCitizenIdentificationLookup(patientDataProtectionService.secureLookup(
                    "citizen-id:" + citizenId.trim().toUpperCase(Locale.ROOT)));
            if (user.getRole() == UserRole.PATIENT) {
                patientDataProtectionService.encryptPatientFields(user);
            }
            userRepository.save(user);
            migrated++;
        }
        if (migrated > 0) {
            LOGGER.info("Created citizen identification lookup values for {} users.", migrated);
        }
    }
}
