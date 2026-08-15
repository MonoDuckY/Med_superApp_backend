package com.yourproject.backend.config;

import com.yourproject.backend.models.Role;
import com.yourproject.backend.models.UserRole;
import com.yourproject.backend.repositories.RoleRepository;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(0)
@RequiredArgsConstructor
public class RoleCatalogInitializer implements ApplicationRunner {
    private final RoleRepository roleRepository;

    @Override
    public void run(ApplicationArguments args) {
        Arrays.stream(UserRole.values()).forEach(this::synchronizeRole);
    }

    private void synchronizeRole(UserRole roleName) {
        roleRepository.findByRoleName(roleName)
                .filter(existingRole -> !roleName.getId().equals(existingRole.getId()))
                .ifPresent(roleRepository::delete);

        Role role = roleRepository.findById(roleName.getId()).orElseGet(Role::new);
        role.setId(roleName.getId());
        role.setRoleName(roleName);
        role.setDescription(descriptionFor(roleName));
        roleRepository.save(role);
    }

    private String descriptionFor(UserRole roleName) {
        return switch (roleName) {
            case ADMIN -> "System administrator";
            case STAFF -> "Hospital staff";
            case DOCTOR -> "Doctor";
            case RESEARCHER -> "Researcher";
            case PATIENT -> "Patient";
        };
    }
}
