package com.yourproject.backend.config;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class SecurityConfigTest {
    @Test
    void passwordEncoder_usesBcryptCostFactorTwelve() {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null);
        PasswordEncoder passwordEncoder = securityConfig.passwordEncoder();

        String passwordHash = passwordEncoder.encode("Validpass1");

        assertTrue(passwordHash.startsWith("$2a$12$"));
        assertTrue(passwordEncoder.matches("Validpass1", passwordHash));
    }
}
