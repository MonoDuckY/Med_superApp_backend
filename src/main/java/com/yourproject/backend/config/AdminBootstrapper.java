package com.yourproject.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.yourproject.backend.dtos.requests.CreateUserRequest;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.UserRepository;
import com.yourproject.backend.services.UserService;
import com.yourproject.backend.utils.UsernameNormalizer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AdminBootstrapper implements ApplicationRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapper.class);

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.admin.username}")
    private String username;

    @Value("${app.bootstrap.admin.password}")
    private String password;

    @Value("${app.bootstrap.admin.full-name}")
    private String fullName;

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            LOGGER.warn("No bootstrap admin credentials were provided. Set BOOTSTRAP_ADMIN_USERNAME and BOOTSTRAP_ADMIN_PASSWORD.");
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException("Both bootstrap admin username and password must be provided.");
        }
        if (userRepository.existsByUsername(UsernameNormalizer.normalizeUsername(username))) {
            LOGGER.info("Bootstrap administrator account already exists; creation skipped.");
            return;
        }

        CreateUserRequest request = new CreateUserRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setRole(UserRole.ADMIN);
        userService.createUser(request, null);
        LOGGER.info("Bootstrap administrator account created.");
    }
}
