package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.yourproject.backend.models.User;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;
import com.yourproject.backend.utils.PhoneNumberNormalizer;

@Component
@Order(2)
public class PatientDataMigration implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(PatientDataMigration.class);
    private final UserRepository userRepository;
    private final PatientDataProtectionService patientDataProtectionService;

    @Value("${app.patient-data.migrate-legacy-on-startup:false}")
    private boolean migrateLegacyOnStartup;

    public PatientDataMigration(UserRepository userRepository, PatientDataProtectionService patientDataProtectionService) {
        this.userRepository = userRepository;
        this.patientDataProtectionService = patientDataProtectionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!migrateLegacyOnStartup) {
            LOGGER.info("Legacy Patient encryption migration is disabled.");
            return;
        }

        userRepository.findAll().forEach(this::migrateUser);
        LOGGER.info("Legacy Patient encryption migration completed.");
    }

    private void migrateUser(User user) {
        if (user.getPhoneNumber() != null && user.getPhoneLookup() == null) {
            user.setPhoneLookup(patientDataProtectionService.phoneLookup(PhoneNumberNormalizer.normalize(user.getPhoneNumber())));
        }
        if (user.getRole() == UserRole.PATIENT && user.getEncryptionVersion() == null) {
            user.setPatientIdLookup(patientDataProtectionService.patientIdLookup(user.getPatientId()));
            patientDataProtectionService.encryptPatientFields(user);
        }
        userRepository.save(user);
    }
}
