package com.yourproject.backend.integration;

import java.util.Base64;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.yourproject.backend.repositories.RefreshTokenRepository;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.PatientDataProtectionService;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
public abstract class MongoIntegrationTestBase {
    @Container
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected PatientDataProtectionService patientDataProtectionService;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mongodb.uri", MONGO::getReplicaSetUrl);
        registry.add("app.jwt.secret", () -> "integration-test-secret-with-at-least-thirty-two-characters");
        registry.add("app.patient-data.aes-key", () -> key((byte) 1));
        registry.add("app.patient-data.lookup-hmac-key", () -> key((byte) 2));
        registry.add("app.bootstrap.admin.phone-number", () -> "");
        registry.add("app.bootstrap.admin.password", () -> "");
        registry.add("app.patient-data.migrate-legacy-on-startup", () -> "false");
    }

    @BeforeEach
    void clearDatabase() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    private static String key(byte seed) {
        byte[] bytes = new byte[32];
        for (int index = 0; index < bytes.length; index++) {
            bytes[index] = (byte) (seed + index);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }
}
